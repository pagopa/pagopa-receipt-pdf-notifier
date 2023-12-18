package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.models.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.client.NotifierQueueClient;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.NotifierQueueClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.IOMessageData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.ReasonError;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReasonErrorCode;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.exception.ErrorToNotifyException;
import it.gov.pagopa.receipt.pdf.notifier.exception.MissingFieldsForNotificationException;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
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
import it.gov.pagopa.receipt.pdf.notifier.service.PDVTokenizerServiceRetryWrapper;
import it.gov.pagopa.receipt.pdf.notifier.service.ReceiptToIOService;
import it.gov.pagopa.receipt.pdf.notifier.utils.ObjectMapperUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus.NOTIFIED;
import static it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus.NOT_NOTIFIED;
import static it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus.NOT_TO_BE_NOTIFIED;

public class ReceiptToIOServiceImpl implements ReceiptToIOService {

    private final Logger logger = LoggerFactory.getLogger(ReceiptToIOServiceImpl.class);

    private static final int MAX_NUMBER_RETRY = Integer.parseInt(System.getenv().getOrDefault("NOTIFY_RECEIPT_MAX_RETRY", "5"));
    private static final List<String> CF_FILTER_NOTIFIER = Arrays.asList(System.getenv().getOrDefault("CF_FILTER_NOTIFIER", "").split(","));

    private final IOClient ioClient;
    private final NotifierQueueClient notifierQueueClient;
    private final IOMessageService ioMessageService;
    private final PDVTokenizerServiceRetryWrapper pdvTokenizerServiceRetryWrapper;

    public ReceiptToIOServiceImpl() {
        this.ioClient = IOClient.getInstance();
        this.notifierQueueClient = NotifierQueueClientImpl.getInstance();
        this.ioMessageService = new IOMessageServiceImpl();
        this.pdvTokenizerServiceRetryWrapper = new PDVTokenizerServiceRetryWrapperImpl();
    }

    ReceiptToIOServiceImpl(IOClient ioClient, NotifierQueueClient notifierQueueClient, IOMessageService ioMessageService, PDVTokenizerServiceRetryWrapper pdvTokenizerServiceRetryWrapper) {
        this.ioClient = ioClient;
        this.notifierQueueClient = notifierQueueClient;
        this.ioMessageService = ioMessageService;
        this.pdvTokenizerServiceRetryWrapper = pdvTokenizerServiceRetryWrapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserNotifyStatus notifyMessage(String fiscalCodeToken, UserType userType, Receipt receipt) {
        try {
            String fiscalCode = getFiscalCode(fiscalCodeToken);

            if (!isToBeNotified(fiscalCode, userType, receipt)) {
                return NOT_TO_BE_NOTIFIED;
            }
            boolean isNotifyAllowed = handleGetProfile(fiscalCode);
            if (!isNotifyAllowed) {
                logger.info("User {} has not to be notified", userType);
                return NOT_TO_BE_NOTIFIED;
            }

            //Send notification to user
            handleSendNotificationToUser(fiscalCode, userType, receipt);
            return NOTIFIED;
        } catch (Exception e) {
            int code = getCodeOrDefault(e);
            if (userType.equals(UserType.DEBTOR)) {
                receipt.setReasonErr(buildReasonError(e.getMessage(), code));
            } else {
                receipt.setReasonErrPayer(buildReasonError(e.getMessage(), code));
            }
            logger.error("Error notifying IO user {}", userType, e);
            return NOT_NOTIFIED;
        }
    }

    /**
     * Invoke getProfile API and verify its response status
     *
     * @param fiscalCode         User fiscal code
     * @throws ErrorToNotifyException in case of error notifying user
     */
    private boolean handleGetProfile(String fiscalCode) throws ErrorToNotifyException {
        FiscalCodePayload fiscalCodePayload = new FiscalCodePayload();
        fiscalCodePayload.setFiscalCode(fiscalCode);
        ApiResponse<LimitedProfile> getProfileResponse;
        try {
            getProfileResponse = this.ioClient.getProfileByPOSTWithHttpInfo(fiscalCodePayload);
        } catch (ApiException e) {
            if (e.getCode() == HttpStatus.SC_NOT_FOUND) {
                return false;
            }
            throw new ErrorToNotifyException("IO /profiles failed", e);
        }

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
            String errorMsg = String.format("Error sending notification to IO user %s", userType);
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
     * {@inheritDoc}
     */
    @Override
    public boolean verifyMessagesNotification(
            EnumMap<UserType, UserNotifyStatus> usersToBeVerified,
            List<IOMessage> messagesNotified,
            Receipt receipt
    ) throws JsonProcessingException {
        UserNotifyStatus debtorNotified =  usersToBeVerified.getOrDefault(UserType.DEBTOR, NOT_TO_BE_NOTIFIED);
        UserNotifyStatus payerNotified = usersToBeVerified.getOrDefault(UserType.PAYER, NOT_TO_BE_NOTIFIED);

        if (debtorNotified.equals(NOTIFIED)) {
            IOMessage ioMessage = getIoMessage(receipt, UserType.DEBTOR);
            messagesNotified.add(ioMessage);
        }
        if (payerNotified.equals(NOTIFIED)) {
            IOMessage ioMessage = getIoMessage(receipt, UserType.PAYER);
            messagesNotified.add(ioMessage);
        }

        if (debtorNotified.equals(NOT_NOTIFIED) || payerNotified.equals(NOT_NOTIFIED)) {
            return requeueReceiptForRetry(receipt);
        }

        if (debtorNotified.equals(NOT_TO_BE_NOTIFIED) && payerNotified.equals(NOT_TO_BE_NOTIFIED)) {
            receipt.setStatus(ReceiptStatusType.NOT_TO_NOTIFY);
            return false;
        }

        receipt.setStatus(ReceiptStatusType.IO_NOTIFIED);
        receipt.setNotified_at(System.currentTimeMillis());
        return false;
    }

    private boolean requeueReceiptForRetry(Receipt receipt) throws JsonProcessingException {
        int numRetry = receipt.getNotificationNumRetry();
        receipt.setNotificationNumRetry(numRetry + 1);

        if (numRetry >= MAX_NUMBER_RETRY) {
            logger.error("Maximum number of retries for event with event id: {}. Receipt updated with status UNABLE_TO_SEND", receipt.getEventId());
            receipt.setStatus(ReceiptStatusType.UNABLE_TO_SEND);
            return false;
        }

        String receiptString = ObjectMapperUtils.writeValueAsString(receipt);
        try {
            Response<SendMessageResult> response = this.notifierQueueClient.sendMessageToQueue(Base64.getMimeEncoder().encodeToString(receiptString.getBytes()));
            if (response.getStatusCode() == com.microsoft.azure.functions.HttpStatus.CREATED.value()) {
                receipt.setStatus(ReceiptStatusType.IO_ERROR_TO_NOTIFY);
                return true;
            }
            receipt.setStatus(ReceiptStatusType.UNABLE_TO_SEND);
            return false;
        } catch (Exception e) {
            logger.error("Error in sending message to queue for receipt with event id: {}. Receipt updated with status UNABLE_TO_SEND", receipt.getEventId(), e);
            receipt.setStatus(ReceiptStatusType.UNABLE_TO_SEND);
            return false;
        }
    }

    private boolean isToBeNotified(String fiscalCode, UserType userType, Receipt receipt) {
        return  isValidFiscalCode(fiscalCode) &&
                (CF_FILTER_NOTIFIER.contains("*") || CF_FILTER_NOTIFIER.contains(fiscalCode)) &&
                (receipt.getIoMessageData() == null || verifyMessageIdIsNotPresent(userType, receipt));
    }

    private boolean isValidFiscalCode(String fiscalCode) {
        if (fiscalCode != null && !fiscalCode.isEmpty()) {
            Pattern pattern = Pattern.compile("^[A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$");
            Matcher matcher = pattern.matcher(fiscalCode);
            return matcher.find();
        }
        return false;
    }

    private boolean verifyMessageIdIsNotPresent(UserType userType, Receipt receipt) {
        return (userType.equals(UserType.DEBTOR) && receipt.getIoMessageData().getIdMessageDebtor() == null)
                ||
                (userType.equals(UserType.PAYER) && receipt.getIoMessageData().getIdMessagePayer() == null);
    }

    private IOMessage getIoMessage(Receipt receipt, UserType userType) {
        IOMessageData ioMessageData = receipt.getIoMessageData();
        String messageId = userType.equals(UserType.DEBTOR) ? ioMessageData.getIdMessageDebtor() : ioMessageData.getIdMessagePayer();
        return IOMessage.builder()
                .messageId(messageId)
                .eventId(receipt.getEventId())
                .build();
    }

    private ReasonError buildReasonError(String errorMessage, int code) {
        return ReasonError.builder()
                .code(code)
                .message(errorMessage)
                .build();
    }

    private int getCodeOrDefault(Exception e) {
        if (e instanceof PDVTokenizerException pdvTokenizerException) {
            return pdvTokenizerException.getStatusCode();
        }
        if (e instanceof JsonProcessingException) {
            return ReasonErrorCode.ERROR_PDV_MAPPING.getCode();
        }
        return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }

    private String getFiscalCode(String fiscalCodeToken) throws PDVTokenizerException, JsonProcessingException {
        try {
            return pdvTokenizerServiceRetryWrapper.getFiscalCodeWithRetry(fiscalCodeToken);
        } catch (Exception e) {
            logger.error("Failed to call tokenizer service");
            throw e;
        }
    }
}
