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
import org.jetbrains.annotations.NotNull;

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
    private static final String SUBJECT_DEBTOR = new String(System.getenv().getOrDefault("SUBJECT_DEBTOR", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    private static final String MARKDOWN_PAYER = new String(System.getenv().getOrDefault("MARKDOWN_PAYER", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    private static final String MARKDOWN_DEBTOR = new String(System.getenv().getOrDefault("MARKDOWN_DEBTOR", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    public static final String ADVANCED = "ADVANCED";

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePayload buildMessagePayload(String fiscalCode, Receipt receipt, UserType userType) throws MissingFieldsForNotificationException {
        StringSubstitutor stringSubstitutor = buildStringSubstitutor(receipt.getEventData(), receipt.getId());

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
    public MessagePayload buildCartPayerMessagePayload(String fiscalCode, CartForReceipt cart) throws MissingFieldsForNotificationException {
        // TODO define string builder for cart payer message
//        StringSubstitutor stringSubstitutor = buildStringSubstitutor(receipt.getEventData(), receipt.getId());

        String thirdPartyId = String.format("%s_CART", cart.getEventId());
//        String subject = stringSubstitutor.replace(SUBJECT_PAYER);
//        String markdown = stringSubstitutor.replace(MARKDOWN_PAYER);

        return buildMessage(fiscalCode, SUBJECT_PAYER, MARKDOWN_PAYER, thirdPartyId);
    }

    @Override
    public MessagePayload buildCartDebtorMessagePayload(String fiscalCode, CartPayment cartPayment, String cartId) throws MissingFieldsForNotificationException {
        // TODO define string builder for cart debtor message
//        StringSubstitutor stringSubstitutor = buildStringSubstitutor(receipt.getEventData(), receipt.getId());

        String thirdPartyId = String.format("%s_CART_%s", cartId, cartPayment.getBizEventId());
//        String subject = stringSubstitutor.replace(SUBJECT_PAYER);
//        String markdown = stringSubstitutor.replace(MARKDOWN_PAYER);

        return buildMessage(fiscalCode, SUBJECT_PAYER, MARKDOWN_PAYER, thirdPartyId);
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
                                        .configurationId(IO_CONFIGURATION_ID)
                                        .build())
                                .build()
                )
                .build();
    }

    @NotNull
    private StringSubstitutor buildStringSubstitutor(EventData eventData, String receiptId) throws MissingFieldsForNotificationException {
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
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("cart.items[0].payee.name", cart.get(0).getPayeeName());
        valuesMap.put("transaction.amount", eventData.getAmount());
        valuesMap.put("cart.items[0].subject", cart.get(0).getSubject() != null ? cart.get(0).getSubject() : "-");

        // Build StringSubstitutor
        return new StringSubstitutor(valuesMap, "{", "}");
    }
}