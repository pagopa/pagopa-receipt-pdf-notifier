package it.gov.pagopa.receipt.pdf.notifier.service;

import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartPayment;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.exception.MissingFieldsForNotificationException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.MessagePayload;

/**
 * Service for building notification message
 */
public interface NotificationMessageBuilder {

    /**
     * Build IO notification message from Receipt's data
     *
     * @param receipt    Receipt from CosmosDB
     * @param fiscalCode User's fiscal code
     * @param userType   the type of user to be notified {@link UserType}
     * @return {@link MessagePayload} used as request to notify
     */
    MessagePayload buildMessagePayload(
            String fiscalCode,
            Receipt receipt,
            UserType userType
    ) throws MissingFieldsForNotificationException;

    /**
     * Build IO notification message for cart payer
     *
     * @param fiscalCode User's fiscal code
     * @param cart       the cart entity
     * @return {@link MessagePayload} used as request to notify
     */
    MessagePayload buildCartPayerMessagePayload(
            String fiscalCode,
            CartForReceipt cart
    ) throws MissingFieldsForNotificationException;

    /**
     * Build IO notification message for cart debtor
     *
     * @param fiscalCode  User's fiscal code
     * @param cartPayment contains the data relative to debtor notice
     * @param cartId      cart identifier
     * @return {@link MessagePayload} used as request to notify
     */
    MessagePayload buildCartDebtorMessagePayload(
            String fiscalCode,
            CartPayment cartPayment,
            String cartId
    ) throws MissingFieldsForNotificationException;
}
