package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.models.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.notifier.client.NotifierCartQueueClient;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.CartReceiptCosmosClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.NotifierCartQueueClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartPayment;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartStatusType;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.Payload;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.CartIOMessage;
import it.gov.pagopa.receipt.pdf.notifier.exception.CartIoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.exception.ErrorToNotifyException;
import it.gov.pagopa.receipt.pdf.notifier.exception.IOAPIException;
import it.gov.pagopa.receipt.pdf.notifier.model.NotifyCartResult;
import it.gov.pagopa.receipt.pdf.notifier.model.NotifyUserResult;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.MessagePayload;
import it.gov.pagopa.receipt.pdf.notifier.service.CartReceiptToIOService;
import it.gov.pagopa.receipt.pdf.notifier.service.IOService;
import it.gov.pagopa.receipt.pdf.notifier.service.NotificationMessageBuilder;
import it.gov.pagopa.receipt.pdf.notifier.service.PDVTokenizerServiceRetryWrapper;
import it.gov.pagopa.receipt.pdf.notifier.utils.MDCConstants;
import it.gov.pagopa.receipt.pdf.notifier.utils.ObjectMapperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

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
    private final Boolean payerNotifyDisabled = Boolean.parseBoolean(System.getenv().getOrDefault("PAYER_NOTIFY_DISABLED", "true"));

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
            NotifierCartQueueClient notifierCartQueueClient,
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
        Payload payload = cartForReceipt.getPayload();
        String payerFiscalCode = payload.getPayerFiscalCode();

        try {
            NotifyCartResult notifyCartResult = new NotifyCartResult();
            if (!Boolean.TRUE.equals(payerNotifyDisabled) && payerFiscalCode != null) {
                MDC.put(MDCConstants.USER_TYPE, UserType.PAYER.name());

                // Notify to payer
                NotifyUserResult payerNotifyResult = notifyPayer(payerFiscalCode, cartForReceipt);
                notifyCartResult.setPayerNotifyResult(payerNotifyResult);
            }

            payload.getCart().forEach(cartPayment -> {
                MDC.put(MDCConstants.USER_TYPE, UserType.DEBTOR.name());
                MDC.put(MDCConstants.BIZ_EVENT_ID, cartPayment.getBizEventId());

                String debtorFiscalCode = cartPayment.getDebtorFiscalCode();
                // Notify to debtor
                if (!ANONIMO.equals(debtorFiscalCode) && !Objects.equals(debtorFiscalCode, payerFiscalCode)) {
                    NotifyUserResult debtorNotifyResult = notifyDebtor(
                            debtorFiscalCode,
                            cartForReceipt.getEventId(),
                            cartPayment
                    );
                    notifyCartResult.addDebtorNotifyStatusToMap(cartPayment.getBizEventId(), debtorNotifyResult);
                }
            });
            return notifyCartResult;
        } finally {
            MDC.remove(MDCConstants.BIZ_EVENT_ID);
            MDC.remove(MDCConstants.USER_TYPE);
        }
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

        UserNotifyStatus payerNotified = gePayerNotifyStatus(notifyCartResult);
        if (payerNotified.equals(NOTIFIED)) {
            CartIOMessage message = CartIOMessage.builder()
                    .id(notifyCartResult.getPayerNotifyResult().getMessageId() + UUID.randomUUID())
                    .messageId(notifyCartResult.getPayerNotifyResult().getMessageId())
                    .cartId(cartForReceipt.getEventId())
                    .userType(UserType.PAYER)
                    .build();
            ioMessages.add(message);
        }

        List<UserNotifyStatus> debtorNotifyStatus = new ArrayList<>();
        if (notifyCartResult.getDebtorNotifyResultMap() != null) {
            notifyCartResult.getDebtorNotifyResultMap().forEach((bizEventId, notifyDebtorResult) -> {
                UserNotifyStatus debtorNotified = notifyDebtorResult.getNotifyStatus();
                if (NOTIFIED.equals(debtorNotified)) {
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
        }

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

    private NotifyUserResult notifyPayer(
            String fiscalCodeToken,
            CartForReceipt cartForReceipt
    ) {
        Payload payload = cartForReceipt.getPayload();
        try {
            String fiscalCode = this.pdvTokenizerServiceRetryWrapper.getFiscalCodeWithRetry(fiscalCodeToken);

            if (userShouldBeDiscardedFromNotification(fiscalCode, payload.getIdMessagePayer())) {
                return NotifyUserResult.builder()
                        .notifyStatus(NOT_TO_BE_NOTIFIED)
                        .build();
            }

            String ioMessageId = getIOMessageForUserIfAlreadyExist(cartForReceipt.getEventId(), null, UserType.PAYER);
            if (ioMessageId != null) {
                payload.setIdMessagePayer(ioMessageId);
                return NotifyUserResult.builder()
                        .notifyStatus(ALREADY_NOTIFIED)
                        .build();
            }

            //Send notification to user
            MessagePayload messagePayload = this.notificationMessageBuilder
                    .buildCartPayerMessagePayload(fiscalCode, cartForReceipt);
            String messageId = this.ioService.sendNotificationToIOUser(messagePayload);
            payload.setIdMessagePayer(messageId);
            return NotifyUserResult.builder()
                    .notifyStatus(NOTIFIED)
                    .messageId(messageId)
                    .build();
        } catch (Exception e) {
            int code = getCodeOrDefault(e);
            logger.error("Error notifying IO user", e);
            payload.setReasonErrPayer(buildReasonError(e.getMessage(), code));
            return NotifyUserResult.builder()
                    .notifyStatus(NOT_NOTIFIED)
                    .build();
        }
    }

    private NotifyUserResult notifyDebtor(
            String fiscalCodeToken,
            String cartId,
            CartPayment cartPayment
    ) {
        try {
            String fiscalCode = this.pdvTokenizerServiceRetryWrapper.getFiscalCodeWithRetry(fiscalCodeToken);

            if (userShouldBeDiscardedFromNotification(fiscalCode, cartPayment.getIdMessageDebtor())) {
                return NotifyUserResult.builder()
                        .notifyStatus(NOT_TO_BE_NOTIFIED)
                        .build();
            }

            String existingMessageId = getIOMessageForUserIfAlreadyExist(cartId, cartPayment.getBizEventId(), UserType.DEBTOR);
            if (existingMessageId != null) {
                cartPayment.setIdMessageDebtor(existingMessageId);
                return NotifyUserResult.builder()
                        .notifyStatus(ALREADY_NOTIFIED)
                        .build();
            }

            //Send notification to user
            MessagePayload messagePayload = this.notificationMessageBuilder
                    .buildCartDebtorMessagePayload(fiscalCode, cartPayment, cartId);
            String messageId = this.ioService.sendNotificationToIOUser(messagePayload);
            cartPayment.setIdMessageDebtor(messageId);
            return NotifyUserResult.builder()
                    .notifyStatus(NOTIFIED)
                    .messageId(messageId)
                    .build();
        } catch (Exception e) {
            int code = getCodeOrDefault(e);
            logger.error("Error notifying IO user", e);
            cartPayment.setReasonErrDebtor(buildReasonError(e.getMessage(), code));
            return NotifyUserResult.builder()
                    .notifyStatus(NOT_NOTIFIED)
                    .build();
        }
    }

    private boolean userShouldBeDiscardedFromNotification(
            String fiscalCode,
            String idMessage
    ) throws IOAPIException, ErrorToNotifyException {
        if (!isFiscalCodeValid(fiscalCode) || idMessage != null) {
            return true;
        }

        if (!this.ioService.isNotifyToIOUserAllowed(fiscalCode)) {
            logger.info("User has not to be notified");
            return true;
        }
        return false;
    }

    private void requeueReceiptForRetry(CartForReceipt cartForReceipt) {
        int numRetry = cartForReceipt.getNotificationNumRetry();
        cartForReceipt.setNotificationNumRetry(numRetry + 1);

        if (numRetry >= MAX_NUMBER_RETRY) {
            logger.error("Maximum number of retries for cart. Cart receipt updated with status UNABLE_TO_SEND");
            cartForReceipt.setStatus(CartStatusType.UNABLE_TO_SEND);
            return;
        }

        String receiptString;
        try {
            receiptString = ObjectMapperUtils.writeValueAsString(cartForReceipt);
        } catch (JsonProcessingException e) {
            logger.error("Unable to requeue cart for retry. Cart receipt will be updated with status IO_ERROR_TO_NOTIFY", e);
            cartForReceipt.setStatus(CartStatusType.IO_ERROR_TO_NOTIFY);
            return;
        }
        try {
            Response<SendMessageResult> response = this.notifierCartQueueClient.sendMessageToQueue(Base64.getMimeEncoder().encodeToString(receiptString.getBytes()));
            if (response.getStatusCode() == com.microsoft.azure.functions.HttpStatus.CREATED.value()) {
                cartForReceipt.setStatus(CartStatusType.IO_ERROR_TO_NOTIFY);
                return;
            }
            logger.error("Error in sending message to queue for cart, queue responded with status {}. Cart receipt updated with status UNABLE_TO_SEND",
                    response.getStatusCode());
            cartForReceipt.setStatus(CartStatusType.UNABLE_TO_SEND);
        } catch (Exception e) {
            logger.error("Error in sending message to queue for cart. Cart receipt updated with status UNABLE_TO_SEND", e);
            cartForReceipt.setStatus(CartStatusType.UNABLE_TO_SEND);
        }
    }

    private String getIOMessageForUserIfAlreadyExist(String cartId, String eventId, UserType userType) {
        try {
            CartIOMessage ioMessage = this.cartReceiptCosmosClient
                    .findIOMessageWithCartIdAndEventIdAndUserType(cartId, eventId, userType);
            logger.warn("The cart receipt has already been notified for user");
            return ioMessage.getMessageId();
        } catch (CartIoMessageNotFoundException e) {
            return null;
        }
    }

    private UserNotifyStatus gePayerNotifyStatus(NotifyCartResult notifyCartResult) {
        if (notifyCartResult.getPayerNotifyResult() != null
                && notifyCartResult.getPayerNotifyResult().getNotifyStatus() != null
        ) {
            return notifyCartResult.getPayerNotifyResult().getNotifyStatus();
        }
        return NOT_TO_BE_NOTIFIED;
    }
}
