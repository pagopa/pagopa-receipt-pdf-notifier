package it.gov.pagopa.receipt.pdf.notifier.exception;

import lombok.Getter;

/**
 * Thrown in case an error occur when invoking IO APIs
 */
@Getter
public class IOAPIException extends Exception {

    private final int statusCode;

    /**
     * Constructs new exception with provided message
     *
     * @param message Detail message
     * @param statusCode status code
     */
    public IOAPIException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Constructs new exception with provided message
     *
     * @param message Detail message
     * @param statusCode status code
     * @param cause Exception causing the constructed one
     */
    public IOAPIException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
