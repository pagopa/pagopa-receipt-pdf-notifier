package it.gov.pagopa.receipt.pdf.notifier.service;

import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;

import java.util.EnumMap;
import java.util.List;

public interface ReceiptToIOService {

    /**
     * Handles IO user validation and notification
     *
     * @param fiscalCode User fiscal code
     * @param userType   Enum User type
     * @param receipt    the Receipt
     * @return the status of the notification {@link UserNotifyStatus}
     */
    UserNotifyStatus notifyMessage(String fiscalCode, UserType userType, Receipt receipt);

    /**
     * Verifies if all users have been notified and updates the receipt
     *
     * @param usersToBeVerified Map<FiscalCode, Status> containing user notification status
     * @param receipt           Receipt to update and save on CosmosDB
     * @return the list of notification message reference to be saved on CosmosDB
     */
    List<IOMessage> verifyMessagesNotification(
            EnumMap<UserType, UserNotifyStatus> usersToBeVerified,
            Receipt receipt
    );
}
