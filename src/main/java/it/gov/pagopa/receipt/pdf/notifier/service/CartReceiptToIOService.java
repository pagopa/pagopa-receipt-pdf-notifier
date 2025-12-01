package it.gov.pagopa.receipt.pdf.notifier.service;

import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.CartIOMessage;
import it.gov.pagopa.receipt.pdf.notifier.model.NotifyCartResult;

import java.util.List;

public interface CartReceiptToIOService {

    /**
     * Handles IO user validation and notification for payer and/or debtor in the provided cart
     *
     * @param cartForReceipt the Cart Receipt entity
     * @return the result of the notification {@link NotifyCartResult}
     */
    NotifyCartResult notifyCart(CartForReceipt cartForReceipt);

    /**
     * Verifies if all users have been notified and update the cart receipt
     *
     * @param notifyCartResult contains the user notification result status
     * @param cartForReceipt   Cart Receipt to update and save on CosmosDB
     * @return the list of notification message reference to be saved on CosmosDB
     */
    List<CartIOMessage> verifyNotificationResultAndUpdateCartReceipt(
            NotifyCartResult notifyCartResult,
            CartForReceipt cartForReceipt
    );
}
