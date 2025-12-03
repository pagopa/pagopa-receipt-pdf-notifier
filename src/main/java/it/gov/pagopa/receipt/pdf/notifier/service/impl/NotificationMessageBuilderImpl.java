package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartPayment;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.CartItem;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.EventData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.exception.MissingFieldsForNotificationException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.MessageContent;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.MessagePayload;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.ThirdPartyData;
import it.gov.pagopa.receipt.pdf.notifier.service.NotificationMessageBuilder;
import org.apache.commons.text.StringSubstitutor;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@inheritDoc}
 */
public class NotificationMessageBuilderImpl implements NotificationMessageBuilder {

    private static final String IO_CONFIGURATION_ID = System.getenv().getOrDefault("IO_CONFIGURATION_ID", "");
    private static final String SUBJECT_PAYER = new String(System.getenv().getOrDefault("SUBJECT_PAYER", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    private static final String SUBJECT_PAYER_CART = new String(System.getenv().getOrDefault("SUBJECT_PAYER_CART", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    private static final String SUBJECT_DEBTOR = new String(System.getenv().getOrDefault("SUBJECT_DEBTOR", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    private static final String MARKDOWN_PAYER = new String(System.getenv().getOrDefault("MARKDOWN_PAYER", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    private static final String MARKDOWN_PAYER_CART = new String(System.getenv().getOrDefault("MARKDOWN_PAYER_CART", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    private static final String MARKDOWN_PAYER_CART_ITEM = new String(System.getenv().getOrDefault("MARKDOWN_PAYER_CART_ITEM", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    private static final String MARKDOWN_DEBTOR = new String(System.getenv().getOrDefault("MARKDOWN_DEBTOR", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    private static final String ADVANCED = "ADVANCED";
    private static final String CART_PLACEHOLDER = "_CART_";
    private static final String CART_ITEM_PAYEE_NAME = "cart.item.payee.name";
    private static final String TRANSACTION_AMOUNT = "transaction.amount";
    private static final String CART_ITEM_SUBJECT = "cart.item.subject";
    public static final String NOTICE_INDEX = "notice.index";
    private static final String NOTICES = "notices";
    private static final String NOTICES_TOTAL = "notices_total";
    private static final String PREFIX = "{";
    private static final String SUFFIX = "}";

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePayload buildMessagePayload(
            String fiscalCode,
            Receipt receipt,
            UserType userType
    ) throws MissingFieldsForNotificationException {
        StringSubstitutor stringSubstitutor = buildSingleReceiptStringSubstitutor(receipt.getEventData(), receipt.getId());

        String subject;
        String markdown;
        if (userType.equals(UserType.DEBTOR)) {
            subject = stringSubstitutor.replace(SUBJECT_DEBTOR);
            markdown = stringSubstitutor.replace(MARKDOWN_DEBTOR);
        } else {
            subject = stringSubstitutor.replace(SUBJECT_PAYER);
            markdown = stringSubstitutor.replace(MARKDOWN_PAYER);
        }
        return buildMessage(fiscalCode, subject, markdown, receipt.getEventId());
    }

    @Override
    public MessagePayload buildCartPayerMessagePayload(
            String fiscalCode,
            CartForReceipt cart
    ) throws MissingFieldsForNotificationException {
        if (cart.getPayload() == null
                || cart.getPayload().getCart() == null
                || cart.getPayload().getCart().isEmpty()
        ) {
            throw new MissingFieldsForNotificationException(
                    "Unable to build the notification message for cart receipt, there are missing fields in receipt necessary for subject and markdown");
        }

        StringBuilder notices = new StringBuilder(10);
        int count = 1;
        for (CartPayment cartPayment : cart.getPayload().getCart()) {
            StringSubstitutor stringSubstitutor = buildCartItemStringSubstitutor(cartPayment, String.valueOf(count));
            notices.append(stringSubstitutor.replace(MARKDOWN_PAYER_CART_ITEM));
            count++;
        }

        String thirdPartyId = String.format("%s%s", cart.getEventId(), CART_PLACEHOLDER);
        StringSubstitutor stringSubstitutor = buildCartStringSubstitutor(notices.toString(), cart.getPayload().getTotalNotice());
        String markdown = stringSubstitutor.replace(MARKDOWN_PAYER_CART);

        return buildMessageWithRemoteContent(fiscalCode, SUBJECT_PAYER_CART, markdown, thirdPartyId);
    }

    @Override
    public MessagePayload buildCartDebtorMessagePayload(
            String fiscalCode,
            CartPayment cartPayment,
            String cartId
    ) throws MissingFieldsForNotificationException {
        StringSubstitutor stringSubstitutor = buildCartItemStringSubstitutor(cartPayment, "");

        String thirdPartyId = String.format("%s%s%s", cartId, CART_PLACEHOLDER, cartPayment.getBizEventId());
        String subject = stringSubstitutor.replace(SUBJECT_DEBTOR);
        String markdown = stringSubstitutor.replace(MARKDOWN_DEBTOR);

        return buildMessageWithRemoteContent(fiscalCode, subject, markdown, thirdPartyId);
    }

    private MessagePayload buildMessageWithRemoteContent(
            String fiscalCode,
            String subject,
            String markdown,
            String thirdPartyId
    ) {
        MessagePayload messagePayload = buildMessage(fiscalCode, subject, markdown, thirdPartyId);
        messagePayload.getContent().getThirdPartyData().setHasRemoteContent(true);
        return messagePayload;
    }

    private MessagePayload buildMessage(
            String fiscalCode,
            String subject,
            String markdown,
            String thirdPartyId
    ) {
        return MessagePayload.builder()
                .fiscalCode(fiscalCode)
                .featureLevelType(ADVANCED)
                .content(
                        MessageContent.builder()
                                .subject(subject)
                                .markdown(markdown)
                                .thirdPartyData(ThirdPartyData.builder()
                                        .id(thirdPartyId)
                                        .hasAttachments(true)
                                        .hasRemoteContent(false)
                                        .configurationId(IO_CONFIGURATION_ID)
                                        .build())
                                .build()
                )
                .build();
    }

    private StringSubstitutor buildSingleReceiptStringSubstitutor(
            EventData eventData,
            String receiptId
    ) throws MissingFieldsForNotificationException {
        if (eventData == null
                || eventData.getAmount() == null
                || eventData.getCart() == null
                || eventData.getCart().isEmpty()
                || eventData.getCart().get(0).getPayeeName() == null) {
            throw new MissingFieldsForNotificationException(
                    String.format(
                            "Unable to build the notification message for receipt with id %s, there are missing fields in receipt necessary for subject and markdown",
                            receiptId));
        }

        List<CartItem> cart = eventData.getCart();
        // Build map
        Map<String, String> valuesMap = new HashMap<>(4);
        valuesMap.put(CART_ITEM_PAYEE_NAME, cart.get(0).getPayeeName());
        valuesMap.put(TRANSACTION_AMOUNT, eventData.getAmount());
        valuesMap.put(CART_ITEM_SUBJECT, cart.get(0).getSubject() != null ? cart.get(0).getSubject() : "-");

        // Build StringSubstitutor
        return new StringSubstitutor(valuesMap, PREFIX, SUFFIX);
    }

    private StringSubstitutor buildCartItemStringSubstitutor(CartPayment cartPayment, String count) throws MissingFieldsForNotificationException {
        if (cartPayment == null
                || cartPayment.getAmount() == null
                || cartPayment.getPayeeName() == null) {
            throw new MissingFieldsForNotificationException(
                    "Unable to build the notification message for cart receipt, there are missing fields in receipt necessary for subject and markdown");
        }

        // Build map
        Map<String, String> valuesMap = new HashMap<>(5);
        valuesMap.put(NOTICE_INDEX, count);
        valuesMap.put(CART_ITEM_PAYEE_NAME, cartPayment.getPayeeName());
        valuesMap.put(TRANSACTION_AMOUNT, cartPayment.getAmount());
        valuesMap.put(CART_ITEM_SUBJECT, cartPayment.getSubject() != null ? cartPayment.getSubject() : "-");

        // Build StringSubstitutor
        return new StringSubstitutor(valuesMap, PREFIX, SUFFIX);
    }

    private StringSubstitutor buildCartStringSubstitutor(String cartItems, int totalNotices) {
        // Build map
        Map<String, String> valuesMap = new HashMap<>(3);
        valuesMap.put(NOTICES, cartItems);
        valuesMap.put(NOTICES_TOTAL, String.valueOf(totalNotices));

        // Build StringSubstitutor
        return new StringSubstitutor(valuesMap, PREFIX, SUFFIX);
    }
}