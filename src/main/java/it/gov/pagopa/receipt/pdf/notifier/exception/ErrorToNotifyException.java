package it.gov.pagopa.receipt.pdf.notifier.exception;

/**
 * Thrown in case IO user can't be notified
 */
public class ErrorToNotifyException extends Exception {

    /**
     * Constructs new exception with provided message
     *
     * @param message Detail message
     */
    public ErrorToNotifyException(String message) {
        super(message);
    }

    /**
     * Constructs new exception with provided message and cause
     *
     * @param message Detail message
     * @param cause Exception causing the constructed one
     */
    public ErrorToNotifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
