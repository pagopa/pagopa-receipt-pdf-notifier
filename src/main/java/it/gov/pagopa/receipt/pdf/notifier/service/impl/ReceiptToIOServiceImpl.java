package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.models.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.client.IOClient;
import it.gov.pagopa.receipt.pdf.notifier.client.NotifierQueueClient;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.IOClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.NotifierQueueClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.IOMessageData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.ReasonError;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReasonErrorCode;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.exception.ErrorToNotifyException;
import it.gov.pagopa.receipt.pdf.notifier.exception.IOAPIException;
import it.gov.pagopa.receipt.pdf.notifier.exception.MissingFieldsForNotificationException;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.service.IOMessageService;
import it.gov.pagopa.receipt.pdf.notifier.service.PDVTokenizerServiceRetryWrapper;
import it.gov.pagopa.receipt.pdf.notifier.model.io.IOProfilePayload;
import it.gov.pagopa.receipt.pdf.notifier.model.io.IOProfileResponse;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.IOMessageResponse;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.MessagePayload;
import it.gov.pagopa.receipt.pdf.notifier.service.IOService;
import it.gov.pagopa.receipt.pdf.notifier.service.ReceiptToIOService;
import it.gov.pagopa.receipt.pdf.notifier.utils.ObjectMapperUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;

import static it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus.NOTIFIED;
import static it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus.NOT_NOTIFIED;
import static it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus.NOT_TO_BE_NOTIFIED;

public class ReceiptToIOServiceImpl implements ReceiptToIOService {

    private final Logger logger = LoggerFactory.getLogger(ReceiptToIOServiceImpl.class);

    private static final int MAX_NUMBER_RETRY = Integer.parseInt(System.getenv().getOrDefault("NOTIFY_RECEIPT_MAX_RETRY", "5"));
    private static final List<String> CF_FILTER_NOTIFIER = Arrays.asList(System.getenv().getOrDefault("CF_FILTER_NOTIFIER", "").split(","));

    private final IOClient ioClient;
    private final NotifierQueueClient notifierQueueClient;
    private final IOService ioService;
    private final PDVTokenizerServiceRetryWrapper pdvTokenizerServiceRetryWrapper;

    public ReceiptToIOServiceImpl() {
        this.ioClient = IOClientImpl.getInstance();
        this.notifierQueueClient = NotifierQueueClientImpl.getInstance();
        this.ioService = new IOServiceImpl();
        this.pdvTokenizerServiceRetryWrapper = new PDVTokenizerServiceRetryWrapperImpl();
    }

    ReceiptToIOServiceImpl(IOClient ioClient, NotifierQueueClient notifierQueueClient, IOService ioService, PDVTokenizerServiceRetryWrapper pdvTokenizerServiceRetryWrapper) {
        this.ioClient = ioClient;
        this.notifierQueueClient = notifierQueueClient;
        this.ioService = ioService;
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
            if (!isNotifyToIOUserAllowed(fiscalCode)) {
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

    private void handleSendNotificationToUser(String fiscalCode, UserType userType, Receipt receipt) throws ErrorToNotifyException, MissingFieldsForNotificationException, IOAPIException {
        MessagePayload messagePayload = this.ioService.buildMessagePayload(fiscalCode, receipt, userType);
        String messageId = sendNotificationToIOUser(messagePayload);

        IOMessageData messageData = receipt.getIoMessageData() != null ? receipt.getIoMessageData() : new IOMessageData();
        if (userType.equals(UserType.DEBTOR)) {
            messageData.setIdMessageDebtor(messageId);
        } else {
            messageData.setIdMessagePayer(messageId);
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

    private boolean isNotifyToIOUserAllowed(String fiscalCode) throws IOAPIException, ErrorToNotifyException {
        IOProfilePayload iOProfilePayload = IOProfilePayload.builder().fiscalCode(fiscalCode).build();
        String payload = serializePayload(iOProfilePayload);

        logger.debug("IO API getProfile called");
        HttpResponse<String> getProfileResponse = this.ioClient.getProfile(payload);
        logger.debug("IO API getProfile invocation completed");

        if (getProfileResponse == null) {
            throw new ErrorToNotifyException("IO /profiles failed to respond");
        }

        if (getProfileResponse.statusCode() != HttpStatus.SC_OK || getProfileResponse.body() == null) {
            String errorMsg = String.format("IO /profiles responded with code %s", getProfileResponse.statusCode());
            throw new ErrorToNotifyException(errorMsg);
        }

        IOProfileResponse ioProfileResponse = deserializeResponse(getProfileResponse.body(), IOProfileResponse.class);
        return ioProfileResponse.isSenderAllowed();
    }

    private String sendNotificationToIOUser(MessagePayload message) throws IOAPIException, ErrorToNotifyException {
        String payload = serializePayload(message);

        logger.debug("IO API submitMessage called");
        HttpResponse<String> notificationResponse = this.ioClient.submitMessage(payload);
        logger.debug("IO API submitMessage invocation completed");

        if (notificationResponse == null) {
            throw new ErrorToNotifyException("IO /messages failed to respond");
        }
        if (notificationResponse.statusCode() != HttpStatus.SC_CREATED || notificationResponse.body() == null) {
            String errorMsg = String.format("IO /messages responded with code %s", notificationResponse.statusCode());
            throw new ErrorToNotifyException(errorMsg);
        }

        IOMessageResponse ioMessageResponse = deserializeResponse(notificationResponse.body(), IOMessageResponse.class);
        return ioMessageResponse.getId();
    }

    private String serializePayload(Object payload) throws ErrorToNotifyException {
        try {
            return ObjectMapperUtils.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ErrorToNotifyException("Failed to serialize payload for IO API invocation", e);
        }
    }

    private <T> T deserializeResponse(String response, Class<T> clazz) throws ErrorToNotifyException {
        try {
            return ObjectMapperUtils.mapString(response, clazz);
        } catch (JsonProcessingException e) {
            throw new ErrorToNotifyException("Failed to deserialize response of IO API invocation", e);
        }
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
