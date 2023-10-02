package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.models.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.client.NotifierQueueClient;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.NotifierQueueClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.EventData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.IOMessageData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.ReasonError;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.exception.ErrorToNotifyException;
import it.gov.pagopa.receipt.pdf.notifier.exception.MissingFieldsForNotificationException;
import it.gov.pagopa.receipt.pdf.notifier.generated.client.ApiException;
import it.gov.pagopa.receipt.pdf.notifier.generated.client.ApiResponse;
import it.gov.pagopa.receipt.pdf.notifier.generated.client.api.IOClient;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.CreatedMessage;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.FiscalCodePayload;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.LimitedProfile;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.NewMessage;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.service.IOMessageService;
import it.gov.pagopa.receipt.pdf.notifier.service.ReceiptToIOService;
import it.gov.pagopa.receipt.pdf.notifier.utils.ObjectMapperUtils;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;

public class ReceiptToIOServiceImpl implements ReceiptToIOService {

    private final Logger logger = LoggerFactory.getLogger(ReceiptToIOServiceImpl.class);

    private static final int MAX_NUMBER_RETRY = Integer.parseInt(System.getenv().getOrDefault("NOTIFY_RECEIPT_MAX_RETRY", "5"));
    private static final List<String> CF_FILTER_NOTIFIER = Arrays.asList(System.getenv().getOrDefault("CF_FILTER_NOTIFIER", "").split(","));

    private final IOClient ioClient;
    private final NotifierQueueClient notifierQueueClient;
    private final IOMessageService ioMessageService;

    public ReceiptToIOServiceImpl() {
        this.ioClient = IOClient.getInstance();
        this.notifierQueueClient = NotifierQueueClientImpl.getInstance();
        this.ioMessageService = new IOMessageServiceImpl();
    }

    ReceiptToIOServiceImpl(IOClient ioClient, NotifierQueueClient notifierQueueClient, IOMessageService ioMessageService) {
        this.ioClient = ioClient;
        this.notifierQueueClient = notifierQueueClient;
        this.ioMessageService = ioMessageService;
    }

    /**
     * Handles IO user validation and notification
     *
     * @param fiscalCode    User fiscal code
     * @param userType      Enum User type
     * @param receipt       the Receipt
     * @return the status of the notification {@link UserNotifyStatus}
     */
    @Override
    public UserNotifyStatus notifyMessage(String fiscalCode, UserType userType, Receipt receipt) {
        if (!isToBeNotified(fiscalCode, userType, receipt)) {
            return UserNotifyStatus.NOT_TO_BE_NOTIFIED;
        }
        try {
            boolean isNotifyAllowed = handleGetProfile(fiscalCode);
            if (!isNotifyAllowed) {
                logger.info("User with fiscal code {} has not to be notified", fiscalCode);
                return UserNotifyStatus.NOT_TO_BE_NOTIFIED;
            }

            //Send notification to user
            handleSendNotificationToUser(fiscalCode, userType, receipt);
            return UserNotifyStatus.NOTIFIED;
        } catch (Exception e) {
            logger.error("Error notifying IO user with fiscal code {}", fiscalCode, e);
            return UserNotifyStatus.NOT_NOTIFIED;
        }
    }

    /**
     * Invoke getProfile API and verify its response status
     *
     * @param fiscalCode         User fiscal code
     * @throws ErrorToNotifyException in case of error notifying user
     */
    private boolean handleGetProfile(String fiscalCode) throws ErrorToNotifyException, ApiException {
        FiscalCodePayload fiscalCodePayload = new FiscalCodePayload();
        fiscalCodePayload.setFiscalCode(fiscalCode);
        ApiResponse<LimitedProfile> getProfileResponse = this.ioClient.getProfileByPOSTWithHttpInfo(fiscalCodePayload);

        if (getProfileResponse == null) {
            throw new ErrorToNotifyException("IO /profiles failed to respond");
        }

        if (getProfileResponse.getData() == null || getProfileResponse.getStatusCode() != HttpStatus.SC_OK) {
            String errorMsg = String.format("IO /profiles responded with code %s", getProfileResponse.getStatusCode());
            throw new ErrorToNotifyException(errorMsg);
        }

        return getProfileResponse.getData().getSenderAllowed();
    }

    /**
     * Handles sending notification to IO user
     *
     * @param fiscalCode User fiscal code
     * @param userType   Enum User type
     * @throws ErrorToNotifyException in case of error during API call
     */
    private void handleSendNotificationToUser(String fiscalCode, UserType userType, Receipt receipt) throws ErrorToNotifyException, MissingFieldsForNotificationException {
        NewMessage message = this.ioMessageService.buildNewMessage(fiscalCode, receipt, userType);

        //IO /messages
        ApiResponse<CreatedMessage> sendMessageResponse;
        try {
            sendMessageResponse = this.ioClient.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(message);
        } catch (Exception e) {
            String errorMsg = String.format("Error sending notification to IO user with fiscal code %s", fiscalCode);
            throw new ErrorToNotifyException(errorMsg, e);
        }

        IOMessageData messageData = receipt.getIoMessageData() != null ? receipt.getIoMessageData() : new IOMessageData();
        if (sendMessageResponse == null) {
            throw new ErrorToNotifyException("IO /messages failed to respond");
        }
        if (sendMessageResponse.getData() == null || sendMessageResponse.getStatusCode() != HttpStatus.SC_CREATED) {
            String errorMsg = String.format("IO /messages responded with code %s", sendMessageResponse.getStatusCode());
            throw new ErrorToNotifyException(errorMsg);
        }
        if (userType.equals(UserType.DEBTOR)) {
            messageData.setIdMessageDebtor(sendMessageResponse.getData().getId());
        } else {
            messageData.setIdMessagePayer(sendMessageResponse.getData().getId());
        }
        receipt.setIoMessageData(messageData);
    }

    /**
     * Verifies if all users have been notified
     *
     * @param usersToBeVerified Map<FiscalCode, Status> containing user notification status
     * @param messagesNotified  List of messages with message id to be saved on CosmosDB
     * @param receipt           Receipt to update and save on CosmosDB
     * @return 1 if a message has been sent to queue
     */
    @Override
    public boolean verifyMessagesNotification(
            EnumMap<UserType, UserNotifyStatus> usersToBeVerified,
            List<IOMessage> messagesNotified,
            Receipt receipt
    ) throws JsonProcessingException {
        String errorMessage = "";
        EventData eventData = receipt.getEventData();
        String debtorCF = eventData.getDebtorFiscalCode();
        String payerCF = eventData.getPayerFiscalCode();
        UserNotifyStatus debtorNotified = getUserNotifyStatus(debtorCF, usersToBeVerified.get(UserType.DEBTOR));
        UserNotifyStatus payerNotified = getUserNotifyStatus(payerCF, usersToBeVerified.get(UserType.PAYER));
        boolean queueSent = false;

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

        }

        if (debtorNotified.equals(UserNotifyStatus.NOT_NOTIFIED) || payerNotified.equals(UserNotifyStatus.NOT_NOTIFIED)) {
            queueSent = handleErrorMessageNotification(receipt, errorMessage);

        } else if (debtorNotified.equals(UserNotifyStatus.NOT_TO_BE_NOTIFIED) &&
                payerNotified.equals(UserNotifyStatus.NOT_TO_BE_NOTIFIED)) {
            receipt.setStatus(ReceiptStatusType.NOT_TO_NOTIFY);

        } else {
            receipt.setStatus(ReceiptStatusType.IO_NOTIFIED);
            receipt.setNotified_at(System.currentTimeMillis());
        }
        return queueSent;
    }

    /**
     * Returns the status of the notification process for the given user
     *
     * @param fiscalCode User fiscal code
     * @param userStatus User status from the previous process
     * @return final userStatus
     */
    private UserNotifyStatus getUserNotifyStatus(String fiscalCode, UserNotifyStatus userStatus) {
        return fiscalCode != null && !fiscalCode.isBlank() && userStatus != null
                ?
                userStatus : UserNotifyStatus.NOT_TO_BE_NOTIFIED;
    }

    /**
     * Generates message's CosmosDB entry with message id and event id
     *
     * @param messagesNotified          List of messages to be saved on CosmosDB
     * @param receipt                   Receipt containing event id
     * @param userNotified              User notification status
     * @param messageData               Message Data containing message id
     * @param userType                  Enum User Type
     * @param previousErrorMessageExist Verify if debtor verification went wrong before the payer's one
     * @return error message if needed
     */
    private String handleMessageMetadata(
            List<IOMessage> messagesNotified,
            Receipt receipt,
            UserNotifyStatus userNotified,
            IOMessageData messageData,
            UserType userType,
            boolean previousErrorMessageExist
    ) {
        if (userNotified != null && messageData != null) {
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
                return generateErrorMessage(userType, previousErrorMessageExist);
            }
        }
        return null;
    }

    /**
     * Return correct error message
     *
     * @param userType                  Enum user type
     * @param previousErrorMessageExist Boolean that indicates if the previous user also had errors
     * @return error message
     */
    @NotNull
    private String generateErrorMessage(UserType userType, boolean previousErrorMessageExist) {
        if (previousErrorMessageExist) {
            return "Error notifying both users";
        }
        if (userType.equals(UserType.DEBTOR)) {
            return "Error notifying debtor user";
        }
        return "Error notifying payer user";
    }

    /**
     * Handles final receipt update with error status and message
     *
     * @param receipt      Receipt to update
     * @param errorMessage Error message to be saved on receipt
     * @return 1 if a message has been sent to queue
     */
    private boolean handleErrorMessageNotification(Receipt receipt, String errorMessage) throws JsonProcessingException {
        int numRetry = receipt.getNotificationNumRetry();
        boolean messageQueueSent = false;

        ReasonError reasonError = new ReasonError();
        reasonError.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        reasonError.setMessage(errorMessage);
        receipt.setReasonErr(reasonError);

        receipt.setNotificationNumRetry(numRetry + 1);
        logger.info("Error sending notification: {}", errorMessage);

        if (numRetry < MAX_NUMBER_RETRY) {
            receipt.setStatus(ReceiptStatusType.IO_ERROR_TO_NOTIFY);

            String receiptString = ObjectMapperUtils.writeValueAsString(receipt);
            Response<SendMessageResult> response;
            try {
                response = this.notifierQueueClient.sendMessageToQueue(Base64.getMimeEncoder().encodeToString(receiptString.getBytes()));
                if (response.getStatusCode() == com.microsoft.azure.functions.HttpStatus.CREATED.value()) {
                    messageQueueSent = true;
                }
            } catch (Exception e) {
                logger.error("Error in sending message to queue for receipt with event id: {}. Receipt updated with status UNABLE_TO_SEND", receipt.getEventId(), e);
            }
        }

        if (!messageQueueSent) {
            receipt.setStatus(ReceiptStatusType.UNABLE_TO_SEND);
        }

        return messageQueueSent;
    }

    private boolean isToBeNotified(String fiscalCode, UserType userType, Receipt receipt) {
        return fiscalCode != null &&
                !fiscalCode.isEmpty() &&
                (CF_FILTER_NOTIFIER.contains("*") || CF_FILTER_NOTIFIER.contains(fiscalCode)) &&
                (receipt.getIoMessageData() == null || verifyMessageIdIsNotPresent(userType, receipt));
    }

    private boolean verifyMessageIdIsNotPresent(UserType userType, Receipt receipt) {
        return (userType.equals(UserType.DEBTOR) && receipt.getIoMessageData().getIdMessageDebtor() == null)
                ||
                (userType.equals(UserType.PAYER) && receipt.getIoMessageData().getIdMessagePayer() == null);
    }
}
