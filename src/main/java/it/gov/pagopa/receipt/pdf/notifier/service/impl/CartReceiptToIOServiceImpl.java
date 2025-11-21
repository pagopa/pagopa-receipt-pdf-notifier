package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.models.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.notifier.client.NotifierCartQueueClient;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.CartReceiptCosmosClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.NotifierCartQueueClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartStatusType;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.CartIOMessage;
import it.gov.pagopa.receipt.pdf.notifier.exception.CartIoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.model.NotifyCartResult;
import it.gov.pagopa.receipt.pdf.notifier.model.NotifyUserResult;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.MessagePayload;
import it.gov.pagopa.receipt.pdf.notifier.service.CartReceiptToIOService;
import it.gov.pagopa.receipt.pdf.notifier.service.IOService;
import it.gov.pagopa.receipt.pdf.notifier.service.NotificationMessageBuilder;
import it.gov.pagopa.receipt.pdf.notifier.service.PDVTokenizerServiceRetryWrapper;
import it.gov.pagopa.receipt.pdf.notifier.utils.ObjectMapperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus.ALREADY_NOTIFIED;
import static it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus.NOTIFIED;
import static it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus.NOT_NOTIFIED;
import static it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus.NOT_TO_BE_NOTIFIED;
import static it.gov.pagopa.receipt.pdf.notifier.utils.ReceiptToIOUtils.ANONIMO;
import static it.gov.pagopa.receipt.pdf.notifier.utils.ReceiptToIOUtils.buildReasonError;
import static it.gov.pagopa.receipt.pdf.notifier.utils.ReceiptToIOUtils.getCodeOrDefault;
import static it.gov.pagopa.receipt.pdf.notifier.utils.ReceiptToIOUtils.isFiscalCodeValid;

public class CartReceiptToIOServiceImpl implements CartReceiptToIOService {

    private final Logger logger = LoggerFactory.getLogger(CartReceiptToIOServiceImpl.class);

    private static final int MAX_NUMBER_RETRY = Integer.parseInt(System.getenv().getOrDefault("NOTIFY_CART_RECEIPT_MAX_RETRY", "5"));
    public static final Boolean PAYER_NOTIFY_DISABLED = Boolean.parseBoolean(System.getenv().getOrDefault("PAYER_NOTIFY_DISABLED", "true"));

    private final IOService ioService;
    private final NotifierCartQueueClient notifierCartQueueClient;
    private final NotificationMessageBuilder notificationMessageBuilder;
    private final PDVTokenizerServiceRetryWrapper pdvTokenizerServiceRetryWrapper;
    private final CartReceiptCosmosClient cartReceiptCosmosClient;

    public CartReceiptToIOServiceImpl() {
        this.ioService = new IOServiceImpl();
        this.notifierCartQueueClient = NotifierCartQueueClientImpl.getInstance();
        this.notificationMessageBuilder = new NotificationMessageBuilderImpl();
        this.pdvTokenizerServiceRetryWrapper = new PDVTokenizerServiceRetryWrapperImpl();
        this.cartReceiptCosmosClient = CartReceiptCosmosClientImpl.getInstance();
    }

    CartReceiptToIOServiceImpl(
            IOService ioService,
            NotifierCartQueueClientImpl notifierCartQueueClient,
            NotificationMessageBuilder notificationMessageBuilder,
            PDVTokenizerServiceRetryWrapper pdvTokenizerServiceRetryWrapper,
            CartReceiptCosmosClient cartReceiptCosmosClient
    ) {
        this.ioService = ioService;
        this.notifierCartQueueClient = notifierCartQueueClient;
        this.notificationMessageBuilder = notificationMessageBuilder;
        this.pdvTokenizerServiceRetryWrapper = pdvTokenizerServiceRetryWrapper;
        this.cartReceiptCosmosClient = cartReceiptCosmosClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotifyCartResult notifyCart(CartForReceipt cartForReceipt) {
        String payerFiscalCode = cartForReceipt.getPayload().getPayerFiscalCode();

        NotifyCartResult notifyCartResult = new NotifyCartResult();
        if (!Boolean.TRUE.equals(PAYER_NOTIFY_DISABLED) && payerFiscalCode != null) {
            //Notify to payer
            NotifyUserResult payerNotifyStatus = notifyMessage(
                    payerFiscalCode,
                    cartForReceipt.getEventId(),
                    null,
                    cartForReceipt.getPayload().getIdMessagePayer(),
                    UserType.PAYER
            );

            notifyCartResult.setPayerNotifyResult(payerNotifyStatus);
        }

        cartForReceipt.getPayload().getCart().forEach(cartPayment -> {
            String debtorFiscalCode = cartPayment.getDebtorFiscalCode();
            //Notify to debtor
            if (!ANONIMO.equals(debtorFiscalCode) && !Objects.equals(debtorFiscalCode, payerFiscalCode)) {
                NotifyUserResult debtorNotifyResult = notifyMessage(
                        debtorFiscalCode,
                        cartForReceipt.getEventId(),
                        cartPayment.getBizEventId(),
                        cartPayment.getIdMessageDebtor(),
                        UserType.DEBTOR
                );
                notifyCartResult.addDebtorNotifyStatusToMap(cartPayment.getBizEventId(), debtorNotifyResult);
            }
        });
        return notifyCartResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CartIOMessage> verifyNotificationResultAndUpdateCartReceipt(
            NotifyCartResult notifyCartResult,
            CartForReceipt cartForReceipt
    ) {
        List<CartIOMessage> ioMessages = new ArrayList<>();

        UserNotifyStatus payerNotified = NOT_TO_BE_NOTIFIED;
        if (notifyCartResult.getPayerNotifyResult() != null) {
            payerNotified = notifyCartResult.getPayerNotifyResult().getNotifyStatus();
        }

        if (payerNotified.equals(NOTIFIED)) {
            CartIOMessage message = CartIOMessage.builder()
                    .id(notifyCartResult.getPayerNotifyResult().getMessageId() + UUID.randomUUID())
                    .messageId(notifyCartResult.getPayerNotifyResult().getMessageId())
                    .cartId(cartForReceipt.getEventId())
                    .userType(UserType.PAYER)
                    .build();
            ioMessages.add(message);
        }

        if (notifyCartResult.getDebtorNotifyResultMap() == null || notifyCartResult.getDebtorNotifyResultMap().isEmpty()) {
            return ioMessages;
        }

        List<UserNotifyStatus> debtorNotifyStatus = new ArrayList<>();
        notifyCartResult.getDebtorNotifyResultMap().forEach((bizEventId, notifyDebtorResult) -> {
            UserNotifyStatus debtorNotified = notifyDebtorResult.getNotifyStatus();
            if (debtorNotified.equals(NOTIFIED)) {
                CartIOMessage message = CartIOMessage.builder()
                        .id(notifyDebtorResult.getMessageId() + UUID.randomUUID())
                        .messageId(notifyDebtorResult.getMessageId())
                        .cartId(cartForReceipt.getEventId())
                        .eventId(bizEventId)
                        .userType(UserType.DEBTOR)
                        .build();
                ioMessages.add(message);
            }
            debtorNotifyStatus.add(debtorNotified);

        });


        boolean atLeastOneDebtorNotNotified = debtorNotifyStatus.stream().anyMatch(status -> status.equals(NOT_NOTIFIED));
        if (atLeastOneDebtorNotNotified || payerNotified.equals(NOT_NOTIFIED)) {
            requeueReceiptForRetry(cartForReceipt);
            return ioMessages;
        }

        boolean allDebtorsNotToBeNotified = debtorNotifyStatus.stream().allMatch(status -> status.equals(NOT_TO_BE_NOTIFIED));
        if (allDebtorsNotToBeNotified && payerNotified.equals(NOT_TO_BE_NOTIFIED)) {
            cartForReceipt.setStatus(CartStatusType.NOT_TO_NOTIFY);
            return ioMessages;
        }

        if (cartForReceipt.getNotified_at() == 0L) {
            cartForReceipt.setNotified_at(System.currentTimeMillis());
        }
        cartForReceipt.setStatus(CartStatusType.IO_NOTIFIED);
        return ioMessages;
    }

    private NotifyUserResult notifyMessage(
            String fiscalCodeToken,
            String cartId,
            String bizEventId,
            String idMessage,
            UserType userType
    ) {
        try {
            String fiscalCode = this.pdvTokenizerServiceRetryWrapper.getFiscalCodeWithRetry(fiscalCodeToken);

            if (!isToBeNotified(fiscalCode, idMessage)) {
                return NotifyUserResult.builder()
                        .notifyStatus(NOT_TO_BE_NOTIFIED)
                        .build();
            }

            String ioMessageId = getIOMessageForUserIfAlreadyExist(cartId, bizEventId, userType);
            if (ioMessageId != null) {
                logger.warn("The receipt with event id {} has already been notified for user type {}", cartId, userType);
                return NotifyUserResult.builder()
                        .notifyStatus(ALREADY_NOTIFIED)
                        .messageId(ioMessageId)
                        .build();
            }

            if (!this.ioService.isNotifyToIOUserAllowed(fiscalCode)) {
                logger.info("User {} has not to be notified", userType);
                return NotifyUserResult.builder()
                        .notifyStatus(NOT_TO_BE_NOTIFIED)
                        .build();
            }

            //Send notification to user
            // TODO define cart message template
            //MessagePayload messagePayload = this.notificationMessageBuilder.buildMessagePayload(fiscalCode, receipt, userType);
            MessagePayload messagePayload = null;
            String messageId = this.ioService.sendNotificationToIOUser(messagePayload);
            return NotifyUserResult.builder()
                    .notifyStatus(NOTIFIED)
                    .messageId(messageId)
                    .build();
        } catch (Exception e) {
            int code = getCodeOrDefault(e);
            logger.error("Error notifying IO user {}", UserType.PAYER, e);
            // TODO reason error should be set in cart object
            return NotifyUserResult.builder()
                    .notifyStatus(NOT_NOTIFIED)
                    .error(buildReasonError(e.getMessage(), code))
                    .build();
        }
    }

    private void requeueReceiptForRetry(CartForReceipt cartForReceipt) {
        int numRetry = cartForReceipt.getNotificationNumRetry();
        cartForReceipt.setNotificationNumRetry(numRetry + 1);

        if (numRetry >= MAX_NUMBER_RETRY) {
            logger.error("Maximum number of retries for event with event id: {}. Cart receipt updated with status UNABLE_TO_SEND", cartForReceipt.getEventId());
            cartForReceipt.setStatus(CartStatusType.UNABLE_TO_SEND);
            return;
        }

        String receiptString;
        try {
            receiptString = ObjectMapperUtils.writeValueAsString(cartForReceipt);
        } catch (JsonProcessingException e) {
            logger.error("Unable to requeue for retry the event with event id: {}. Cart receipt updated with status IO_ERROR_TO_NOTIFY", cartForReceipt.getEventId(), e);
            cartForReceipt.setStatus(CartStatusType.IO_ERROR_TO_NOTIFY);
            return;
        }
        try {
            Response<SendMessageResult> response = this.notifierCartQueueClient.sendMessageToQueue(Base64.getMimeEncoder().encodeToString(receiptString.getBytes()));
            if (response.getStatusCode() == com.microsoft.azure.functions.HttpStatus.CREATED.value()) {
                cartForReceipt.setStatus(CartStatusType.IO_ERROR_TO_NOTIFY);
                return;
            }
            logger.error("Error in sending message to queue for cart receipt with event id: {}, queue responded with status {}. Cart receipt updated with status UNABLE_TO_SEND",
                    cartForReceipt.getEventId(), response.getStatusCode());
            cartForReceipt.setStatus(CartStatusType.UNABLE_TO_SEND);
        } catch (Exception e) {
            logger.error("Error in sending message to queue for cart receipt with event id: {}. Cart receipt updated with status UNABLE_TO_SEND", cartForReceipt.getEventId(), e);
            cartForReceipt.setStatus(CartStatusType.UNABLE_TO_SEND);
        }
    }

    private boolean isToBeNotified(String fiscalCode, String idMessage) {
        return isFiscalCodeValid(fiscalCode) && idMessage == null;
    }

    private String getIOMessageForUserIfAlreadyExist(String cartId, String eventId, UserType userType) {
        try {
            CartIOMessage ioMessage = this.cartReceiptCosmosClient
                    .findIOMessageWithCartIdAndEventIdAndUserType(cartId, eventId, userType);
            return ioMessage.getMessageId();
        } catch (CartIoMessageNotFoundException e) {
            return null;
        }
    }
}
