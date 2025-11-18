package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.models.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.client.NotifierQueueClient;
import it.gov.pagopa.receipt.pdf.notifier.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.NotifierQueueClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.ReceiptCosmosClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.IOMessageData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.exception.ErrorToNotifyException;
import it.gov.pagopa.receipt.pdf.notifier.exception.IOAPIException;
import it.gov.pagopa.receipt.pdf.notifier.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.exception.MissingFieldsForNotificationException;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.MessagePayload;
import it.gov.pagopa.receipt.pdf.notifier.service.IOService;
import it.gov.pagopa.receipt.pdf.notifier.service.NotificationMessageBuilder;
import it.gov.pagopa.receipt.pdf.notifier.service.PDVTokenizerServiceRetryWrapper;
import it.gov.pagopa.receipt.pdf.notifier.service.ReceiptToIOService;
import it.gov.pagopa.receipt.pdf.notifier.utils.ObjectMapperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus.ALREADY_NOTIFIED;
import static it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus.NOTIFIED;
import static it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus.NOT_NOTIFIED;
import static it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus.NOT_TO_BE_NOTIFIED;
import static it.gov.pagopa.receipt.pdf.notifier.utils.ReceiptToIOUtils.buildReasonError;
import static it.gov.pagopa.receipt.pdf.notifier.utils.ReceiptToIOUtils.getCodeOrDefault;

public class ReceiptToIOServiceImpl implements ReceiptToIOService {

    private final Logger logger = LoggerFactory.getLogger(ReceiptToIOServiceImpl.class);

    private static final int MAX_NUMBER_RETRY = Integer.parseInt(System.getenv().getOrDefault("NOTIFY_RECEIPT_MAX_RETRY", "5"));
    private static final List<String> CF_FILTER_NOTIFIER = Arrays.asList(System.getenv().getOrDefault("CF_FILTER_NOTIFIER", "").split(","));

    private final IOService ioService;
    private final NotifierQueueClient notifierQueueClient;
    private final NotificationMessageBuilder notificationMessageBuilder;
    private final PDVTokenizerServiceRetryWrapper pdvTokenizerServiceRetryWrapper;
    private final ReceiptCosmosClient receiptCosmosClient;

    public ReceiptToIOServiceImpl() {
        this.ioService = new IOServiceImpl();
        this.notifierQueueClient = NotifierQueueClientImpl.getInstance();
        this.notificationMessageBuilder = new NotificationMessageBuilderImpl();
        this.pdvTokenizerServiceRetryWrapper = new PDVTokenizerServiceRetryWrapperImpl();
        this.receiptCosmosClient = ReceiptCosmosClientImpl.getInstance();
    }

    ReceiptToIOServiceImpl(
            IOService ioService,
            NotifierQueueClient notifierQueueClient,
            NotificationMessageBuilder notificationMessageBuilder,
            PDVTokenizerServiceRetryWrapper pdvTokenizerServiceRetryWrapper,
            ReceiptCosmosClient receiptCosmosClient
    ) {
        this.ioService = ioService;
        this.notifierQueueClient = notifierQueueClient;
        this.notificationMessageBuilder = notificationMessageBuilder;
        this.pdvTokenizerServiceRetryWrapper = pdvTokenizerServiceRetryWrapper;
        this.receiptCosmosClient = receiptCosmosClient;
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

            String ioMessageId = getIOMessageForUserIfAlreadyExist(receipt, userType);
            if (ioMessageId != null) {
                logger.warn("The receipt with event id  {} has already been notified for user type {}", receipt.getEventId(), userType);
                updateReceiptWithIOMessageData(userType, receipt, ioMessageId);
                return ALREADY_NOTIFIED;
            }

            if (!this.ioService.isNotifyToIOUserAllowed(fiscalCode)) {
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
     * {@inheritDoc}
     */
    @Override
    public boolean verifyMessagesNotification(
            EnumMap<UserType, UserNotifyStatus> usersToBeVerified,
            List<IOMessage> messagesNotified,
            Receipt receipt
    ) {
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

        if (receipt.getNotified_at() == 0L) {
            receipt.setNotified_at(System.currentTimeMillis());
        }
        receipt.setStatus(ReceiptStatusType.IO_NOTIFIED);
        return false;
    }

    private void handleSendNotificationToUser(String fiscalCode, UserType userType, Receipt receipt) throws ErrorToNotifyException, MissingFieldsForNotificationException, IOAPIException {
        MessagePayload messagePayload = this.notificationMessageBuilder.buildMessagePayload(fiscalCode, receipt, userType);
        String messageId = this.ioService.sendNotificationToIOUser(messagePayload);

        updateReceiptWithIOMessageData(userType, receipt, messageId);
    }

    private boolean requeueReceiptForRetry(Receipt receipt) {
        int numRetry = receipt.getNotificationNumRetry();
        receipt.setNotificationNumRetry(numRetry + 1);

        if (numRetry >= MAX_NUMBER_RETRY) {
            logger.error("Maximum number of retries for event with event id: {}. Receipt updated with status UNABLE_TO_SEND", receipt.getEventId());
            receipt.setStatus(ReceiptStatusType.UNABLE_TO_SEND);
            return false;
        }

        String receiptString;
        try {
            receiptString = ObjectMapperUtils.writeValueAsString(receipt);
        } catch (JsonProcessingException e) {
            logger.error("Unable to requeue for retry the event with event id: {}. Receipt updated with status IO_ERROR_TO_NOTIFY", receipt.getEventId(), e);
            receipt.setStatus(ReceiptStatusType.IO_ERROR_TO_NOTIFY);
            return false;
        }
        try {
            Response<SendMessageResult> response = this.notifierQueueClient.sendMessageToQueue(Base64.getMimeEncoder().encodeToString(receiptString.getBytes()));
            if (response.getStatusCode() == com.microsoft.azure.functions.HttpStatus.CREATED.value()) {
                receipt.setStatus(ReceiptStatusType.IO_ERROR_TO_NOTIFY);
                return true;
            }
            logger.error("Error in sending message to queue for receipt with event id: {}, queue responded with status {}. Receipt updated with status UNABLE_TO_SEND",
                    receipt.getEventId(), response.getStatusCode());
            receipt.setStatus(ReceiptStatusType.UNABLE_TO_SEND);
            return false;
        } catch (Exception e) {
            logger.error("Error in sending message to queue for receipt with event id: {}. Receipt updated with status UNABLE_TO_SEND", receipt.getEventId(), e);
            receipt.setStatus(ReceiptStatusType.UNABLE_TO_SEND);
            return false;
        }
    }

    private boolean isToBeNotified(String fiscalCode, UserType userType, Receipt receipt) {
        return isValidFiscalCode(fiscalCode) &&
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
                .id(messageId + UUID.randomUUID())
                .messageId(messageId)
                .eventId(receipt.getEventId())
                .userType(userType)
                .build();
    }

    private String getFiscalCode(String fiscalCodeToken) throws PDVTokenizerException, JsonProcessingException {
        try {
            return pdvTokenizerServiceRetryWrapper.getFiscalCodeWithRetry(fiscalCodeToken);
        } catch (Exception e) {
            logger.error("Failed to call tokenizer service");
            throw e;
        }
    }

    private void updateReceiptWithIOMessageData(UserType userType, Receipt receipt, String idMessage) {
        IOMessageData messageData = receipt.getIoMessageData() != null ? receipt.getIoMessageData() : new IOMessageData();
        if (userType.equals(UserType.DEBTOR)) {
            messageData.setIdMessageDebtor(idMessage);
        } else {
            messageData.setIdMessagePayer(idMessage);
        }
        receipt.setIoMessageData(messageData);
    }

    private String getIOMessageForUserIfAlreadyExist(Receipt receipt, UserType userType) {
        try {
            IOMessage ioMessage = this.receiptCosmosClient.findIOMessageWithEventIdAndUserType(receipt.getEventId(), userType);
            return ioMessage.getMessageId();
        } catch (IoMessageNotFoundException e) {
            return null;
        }
    }
}
