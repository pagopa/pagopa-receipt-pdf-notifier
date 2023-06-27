package it.gov.pagopa.receipt.pdf.notifier.service;

import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.receipt.pdf.notifier.client.generated.ApiResponse;
import it.gov.pagopa.receipt.pdf.notifier.client.generated.api.IOClient;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.EventData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.IOMessageData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.ReasonError;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.exception.ErrorToNotifyException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.model.generated.CreatedMessage;
import it.gov.pagopa.receipt.pdf.notifier.model.generated.FiscalCodePayload;
import it.gov.pagopa.receipt.pdf.notifier.model.generated.LimitedProfile;
import it.gov.pagopa.receipt.pdf.notifier.model.generated.NewMessage;
import lombok.NoArgsConstructor;
import org.apache.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@NoArgsConstructor
public class ReceiptToIOService {

    private static final int MAX_NUMBER_RETRY = Integer.parseInt(System.getenv().getOrDefault("COSMOS_RECEIPT_QUEUE_MAX_RETRY", "5"));

    /**
     * Handles IO user validation and notification
     *
     * @param usersToBeVerified Map<FiscalCode, Status> containing user notification status
     * @param fiscalCode        User fiscal code
     * @param userType          Enum User type
     */
    public void notifyMessage(Map<String, UserNotifyStatus> usersToBeVerified,
                              String fiscalCode,
                              UserType userType,
                              Logger logger) {
        FiscalCodePayload fiscalCodePayload = new FiscalCodePayload();
        IOClient client = new IOClient();

        fiscalCodePayload.setFiscalCode(fiscalCode);
        //Verify user is an IO user
        //IO /profiles
        ApiResponse<LimitedProfile> getProfileResponse;
        try {
            getProfileResponse = client.getProfileByPOSTWithHttpInfo(fiscalCodePayload);

            if (getProfileResponse != null
            ) {
                if (
                        getProfileResponse.getData() != null &&
                                getProfileResponse.getStatusCode() == HttpStatus.SC_OK
                ) {
                    if (getProfileResponse.getData().getSenderAllowed()) {
                        //Send notification to user
                        handleSendNotificationToUser(fiscalCode, userType, client);

                        usersToBeVerified.put(fiscalCode, UserNotifyStatus.NOTIFIED);

                    } else {
                        usersToBeVerified.put(fiscalCode, UserNotifyStatus.NOT_TO_BE_NOTIFIED);

                        logger.info("User with fiscal code %s has not to be notified");
                    }
                } else {
                    String errorMsg = String.format("IO /profiles responded with code %s", getProfileResponse.getStatusCode());
                    throw new ErrorToNotifyException(errorMsg);
                }
            } else {
                throw new ErrorToNotifyException("IO /profiles failed to respond");
            }
        } catch (Exception e) {
            usersToBeVerified.put(fiscalCode, UserNotifyStatus.NOT_NOTIFIED);

            String logMsg = String.format("Error verifying IO user with fiscal code %s : %s", fiscalCode, e);
            logger.severe(logMsg);
        }
    }

    /**
     * Handles sending notification to IO user
     *
     * @param fiscalCode User fiscal code
     * @param userType   Enum User type
     * @param client     IO API Client
     * @throws ErrorToNotifyException in case of error during API call
     */
    private static void handleSendNotificationToUser(
            String fiscalCode,
            UserType userType,
            IOClient client
    ) throws ErrorToNotifyException {
        NewMessage message = new NewMessage();
        //TODO build message and send notification to users
        //IO /messages
        try {
            ApiResponse<CreatedMessage> sendMessageResponse = client.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(message);
            IOMessageData messageData = new IOMessageData();
            if (sendMessageResponse != null) {
                if (sendMessageResponse.getData() != null &&
                        sendMessageResponse.getStatusCode() == HttpStatus.SC_OK
                ) {
                    if (userType.equals(UserType.DEBTOR)) {
                        messageData.setIdMessageDebtor(sendMessageResponse.getData().getId());
                    } else {
                        messageData.setIdMessagePayer(sendMessageResponse.getData().getId());
                    }

                } else {
                    String errorMsg = String.format("IO /messages responded with code %s", sendMessageResponse.getStatusCode());
                    throw new ErrorToNotifyException(errorMsg);
                }
            } else {
                throw new ErrorToNotifyException("IO /messages failed to respond");
            }
        } catch (Exception e) {
            String errorMsg = String.format("Error sending notification to IO user with fiscal code %s : %s", fiscalCode, e);
            throw new ErrorToNotifyException(errorMsg);
        }
    }

    /**
     * Verifies if all users have been notified
     *
     * @param usersToBeVerified Map<FiscalCode, Status> containing user notification status
     * @param messagesNotified  List of messages with message id to be saved on CosmosDB
     * @param receipt           Receipt to update and save on CosmosDB
     * @param requeueMessages   OutputBinding to send a message to the queue
     * @param logger            Logger
     * @return 1 if a message has been sent to queue
     */
    public int verifyMessagesNotification(
            Map<String, UserNotifyStatus> usersToBeVerified,
            List<IOMessage> messagesNotified,
            Receipt receipt,
            OutputBinding<String> requeueMessages,
            Logger logger
    ) {
        String errorMessage;
        EventData eventData = receipt.getEventData();
        String debtorCF = eventData.getDebtorFiscalCode();
        String payerCF = eventData.getPayerFiscalCode();
        UserNotifyStatus debtorNotified = usersToBeVerified.get(debtorCF);
        UserNotifyStatus payerNotified = usersToBeVerified.get(payerCF);
        int queueSent = 0;

        if (receipt.getIoMessageData() != null) {
            IOMessageData messageData = receipt.getIoMessageData();

            //Verify notification to debtor user
            errorMessage = handleMessageMetadata(
                    messagesNotified,
                    receipt,
                    debtorNotified,
                    messageData,
                    UserType.DEBTOR,
                    false);

            //Verify notification to payer user
            errorMessage = handleMessageMetadata(
                    messagesNotified,
                    receipt,
                    payerNotified,
                    messageData,
                    UserType.PAYER,
                    errorMessage != null);

        } else {
            errorMessage = "Error to notify message";
        }

        if (debtorNotified.equals(UserNotifyStatus.NOT_NOTIFIED) || payerNotified.equals(UserNotifyStatus.NOT_NOTIFIED)) {
            queueSent = handleErrorMessageNotification(receipt, requeueMessages, errorMessage, logger);

        } else if (debtorNotified.equals(UserNotifyStatus.NOT_TO_BE_NOTIFIED) && payerNotified.equals(UserNotifyStatus.NOT_TO_BE_NOTIFIED)) {
            receipt.setStatus(ReceiptStatusType.NOT_TO_NOTIFY);

        } else {
            receipt.setStatus(ReceiptStatusType.IO_NOTIFIED);
        }

        return queueSent;
    }

    /**
     * Generates message's CosmosDB entry with message id and event id
     *
     * @param messagesNotified List of messages to be saved on CosmosDB
     * @param receipt Receipt containing event id
     * @param userNotified User notification status
     * @param messageData Message Data containing message id
     * @param userType Enum User Type
     * @param previousErrorMessageExist Verify if debtor verification went wrong before the payer's one
     * @return error message if needed
     */
    private static String handleMessageMetadata(
            List<IOMessage> messagesNotified,
            Receipt receipt,
            UserNotifyStatus userNotified,
            IOMessageData messageData,
            UserType userType,
            boolean previousErrorMessageExist
    ) {


        if (userNotified.equals(UserNotifyStatus.NOTIFIED)) {
            IOMessage ioMessage = new IOMessage();
            if (userType.equals(UserType.DEBTOR)) {
                ioMessage.setMessageId(messageData.getIdMessageDebtor());

            } else {
                ioMessage.setMessageId(messageData.getIdMessagePayer());

            }

            ioMessage.setEventId(receipt.getEventId());
            messagesNotified.add(ioMessage);

        } else if (userNotified.equals(UserNotifyStatus.NOT_NOTIFIED)) {
            if (previousErrorMessageExist) {

                return "Error notifying both users";
            } else {
                if (userType.equals(UserType.DEBTOR)) {

                    return "Error notifying debtor user";
                } else {
                    return "Error notifying payer user";
                }
            }
        }

        return null;
    }

    /**
     * Handles final receipt update with error status and message
     *
     * @param receipt Receipt to update
     * @param requeueMessages OutputBinding to send a message to queue
     * @param errorMessage Error message to be saved on receipt
     * @param logger Logger
     * @return 1 if a message has been sent to queue
     */
    private static int handleErrorMessageNotification(
            Receipt receipt, OutputBinding<String> requeueMessages, String errorMessage, Logger logger
    ) {

        int numRetry = receipt.getNotificationNumRetry();
        if (numRetry > MAX_NUMBER_RETRY) {
            receipt.setStatus(ReceiptStatusType.UNABLE_TO_SEND);
        } else {
            receipt.setStatus(ReceiptStatusType.IO_NOTIFIER_RETRY);
        }

        ReasonError reasonError = new ReasonError();
        reasonError.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        reasonError.setMessage(errorMessage);
        receipt.setReasonErr(reasonError);

        receipt.setNotificationNumRetry(numRetry + 1);

        requeueMessages.setValue(receipt.getEventId());

        String logMsg = String.format("Error sending notification: %s", errorMessage);
        logger.severe(logMsg);

        return 1;
    }
}
