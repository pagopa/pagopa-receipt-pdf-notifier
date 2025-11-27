package it.gov.pagopa.receipt.pdf.notifier.service;

import it.gov.pagopa.receipt.pdf.notifier.exception.ErrorToNotifyException;
import it.gov.pagopa.receipt.pdf.notifier.exception.IOAPIException;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.MessagePayload;

/**
 * Service that wrap logic for invoking IO services
 */
public interface IOService {

    /**
     * Verify if the user with the provided fiscal code allow receipts notification
     *
     * @param fiscalCode fiscal code of the user
     * @return <code>true</code> if the notification is allowed, <code>false</code> otherwise
     * @throws IOAPIException         if an unexpected error occur while invoking IO services
     * @throws ErrorToNotifyException if the response from IO service is not OK
     */
    boolean isNotifyToIOUserAllowed(String fiscalCode) throws IOAPIException, ErrorToNotifyException;

    /**
     * Send a notification message to a specific user through IO services
     *
     * @param message contains all the necessary information to send the notification
     * @return the message id that reference the notification
     * @throws IOAPIException         if an unexpected error occur while invoking IO services
     * @throws ErrorToNotifyException if the response from IO service is not OK
     */
    String sendNotificationToIOUser(MessagePayload message) throws IOAPIException, ErrorToNotifyException;
}
