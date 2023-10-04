package it.gov.pagopa.receipt.pdf.notifier.service;

import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.exception.MissingFieldsForNotificationException;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.NewMessage;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;

public interface IOMessageService {

    /**
     * Build IO message from Receipt's data
     *
     * @param receipt    Receipt from CosmosDB
     * @param fiscalCode User's fiscal code
     * @param userType the type of user to be notified {@link UserType}
     * @return {@link NewMessage} used as request to notify
     */
    NewMessage buildNewMessage(String fiscalCode, Receipt receipt, UserType userType) throws MissingFieldsForNotificationException;
}
