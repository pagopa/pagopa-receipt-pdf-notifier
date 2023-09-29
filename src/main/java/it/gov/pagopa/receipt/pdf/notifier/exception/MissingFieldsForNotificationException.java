package it.gov.pagopa.receipt.pdf.notifier.exception;

/** Thrown in case there are missing file in receipt to build notification message */
public class MissingFieldsForNotificationException extends Exception {

    /**
     * Constructs new exception with provided message
     *
     * @param message Detail message
     */
    public MissingFieldsForNotificationException(String message) {
        super(message);
    }
}
