package it.gov.pagopa.receipt.pdf.notifier.service;

import com.azure.cosmos.implementation.apachecommons.lang.tuple.Pair;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    Pair<Receipt, UserNotifyStatus> notifyMessage(String fiscalCode, UserType userType, Receipt receipt);

    /**
     * Verifies if all users have been notified
     *
     * @param usersToBeVerified Map<FiscalCode, Status> containing user notification status
     * @param messagesNotified  List of messages with message id to be saved on CosmosDB
     * @param receipt           Receipt to update and save on CosmosDB
     * @return true if a message has been sent to queue, false otherwise
     */
    Pair<Receipt,Boolean> verifyMessagesNotification(
            EnumMap<UserType, UserNotifyStatus> usersToBeVerified,
            List<IOMessage> messagesNotified,
            Receipt receipt
    ) throws JsonProcessingException;
}
