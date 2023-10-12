package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.exception.MissingFieldsForNotificationException;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.MessageContent;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.NewMessage;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.ThirdPartyData;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.service.IOMessageService;
import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class IOMessageServiceImpl implements IOMessageService {

    private static final String SUBJECT_PAYER = new String(System.getenv().getOrDefault("SUBJECT_PAYER", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    private static final String SUBJECT_DEBTOR = new String(System.getenv().getOrDefault("SUBJECT_DEBTOR", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    private static final String MARKDOWN_PAYER = new String(System.getenv().getOrDefault("MARKDOWN_PAYER", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    private static final String MARKDOWN_DEBTOR = new String(System.getenv().getOrDefault("MARKDOWN_DEBTOR", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

    /**
     * {@inheritDoc}
     */
    @Override
    public NewMessage buildNewMessage(String fiscalCode, Receipt receipt, UserType userType) throws MissingFieldsForNotificationException {
        NewMessage message = new NewMessage();
        message.setFiscalCode(fiscalCode);
        message.setFeatureLevelType("ADVANCED");
        message.setContent(buildMessageContent(receipt, userType));

        return message;
    }

    @NotNull
    private MessageContent buildMessageContent(Receipt receipt, UserType userType) throws MissingFieldsForNotificationException {
        StringSubstitutor stringSubstitutor = buildStringSubstitutor(receipt);

        String subject;
        String markdown;
        if (userType.equals(UserType.DEBTOR)) {
            subject = stringSubstitutor.replace(SUBJECT_DEBTOR);
            markdown = stringSubstitutor.replace(MARKDOWN_DEBTOR);
        } else {
            subject = stringSubstitutor.replace(SUBJECT_PAYER);
            markdown = stringSubstitutor.replace(MARKDOWN_PAYER);
        }
        MessageContent content = new MessageContent();
        content.setSubject(subject);
        content.setMarkdown(markdown);
        content.setThirdPartyData(buildThirdPartyData(receipt));

        return content;
    }

    @NotNull
    private ThirdPartyData buildThirdPartyData(Receipt receipt) {
        ThirdPartyData thirdPartyData = new ThirdPartyData();
        thirdPartyData.setId(receipt.getEventId());
        thirdPartyData.setHasAttachments(true);
        return thirdPartyData;
    }


    @NotNull
    private StringSubstitutor buildStringSubstitutor(Receipt receipt) throws MissingFieldsForNotificationException {
        if (receipt.getEventData() == null
                || receipt.getEventData().getAmount() == null
                || receipt.getEventData().getCart() == null
                || receipt.getEventData().getCart().isEmpty()
                || receipt.getEventData().getCart().get(0).getPayeeName() == null
                || receipt.getEventData().getCart().get(0).getSubject() == null
        ) {
            String errMsg = String.format("Unable to build the notification message for receipt with id %s, there are missing fields in receipt necessary for subject and markdown", receipt.getId());
            throw new MissingFieldsForNotificationException(errMsg);
        }


        // Build map
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("cart.items[0].payee.name", receipt.getEventData().getCart().get(0).getPayeeName());
        valuesMap.put("transaction.amount", formatAmount(receipt.getEventData().getAmount()));
        valuesMap.put("cart.items[0].subject", receipt.getEventData().getCart().get(0).getSubject());

        // Build StringSubstitutor
        return new StringSubstitutor(valuesMap, "{", "}");
    }

    private String formatAmount(String amount) {
        BigDecimal valueToFormat = new BigDecimal(amount);
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.ITALY);
        return numberFormat.format(valueToFormat);
    }


}