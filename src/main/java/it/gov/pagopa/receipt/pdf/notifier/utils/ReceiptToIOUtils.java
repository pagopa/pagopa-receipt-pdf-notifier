package it.gov.pagopa.receipt.pdf.notifier.utils;

import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.MessageContent;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.NewMessage;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.ThirdPartyData;

public class ReceiptToIOUtils {

    /**
     * Hide from public usage.
     */
    private ReceiptToIOUtils() {
    }

    /**
     * Verifies receipt is ready to be notified
     *
     * @param receipt Receipt from CosmosDB
     * @return boolean
     */
    public static boolean verifyReceiptStatus(Receipt receipt) {
        return receipt.getStatus().equals(ReceiptStatusType.GENERATED) ||
                receipt.getStatus().equals(ReceiptStatusType.SIGNED) ||
                receipt.getStatus().equals(ReceiptStatusType.IO_NOTIFIER_RETRY);
    }

    /**
     * Verifies message hasn't been sent already
     *
     * @param userType Enum user type
     * @param receipt  Receipt from CosmosDB
     * @return boolean asserting if there is or not a message id
     */
    public static boolean verifyMessageIdIsNotPresent(UserType userType, Receipt receipt) {
        return (userType.equals(UserType.DEBTOR) && receipt.getIoMessageData().getIdMessageDebtor() == null)
                ||
                (userType.equals(UserType.PAYER) && receipt.getIoMessageData().getIdMessagePayer() == null);
    }

    /**
     * Build message request from Receipt's data
     *
     * @param receipt    Receipt from CosmosDB
     * @param fiscalCode User's fiscal code
     * @return NewMessage object used as request to notify
     */
    public static NewMessage buildNewMessage(String fiscalCode, Receipt receipt) {
        NewMessage message = new NewMessage();

        message.setFiscalCode(fiscalCode);
        message.setFeatureLevelType("ADVANCED");

        MessageContent content = new MessageContent();
        //https://pagopa.atlassian.net/wiki/spaces/PPR/pages/719093789/Overview+processo+invio+messaggi+IO
        //TODO subject, markdown
        content.setSubject("Test pagoPA pdf receipt");
        content.setMarkdown("A message body markdown Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris in nulla tristique, euismod eros ac, sodales magna .... on min 80 character");

        ThirdPartyData thirdPartyData = new ThirdPartyData();
        String pdfId = String.format(receipt.getEventId(), fiscalCode);
        thirdPartyData.setId(pdfId);
        thirdPartyData.setHasAttachments(true);
        //TODO set summary ( NOT mandatory )
        thirdPartyData.setSummary("");

        content.setThirdPartyData(thirdPartyData);

        message.setContent(content);

        return message;
    }
}
