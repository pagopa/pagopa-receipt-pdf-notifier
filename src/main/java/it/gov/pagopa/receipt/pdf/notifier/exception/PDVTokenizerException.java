package it.gov.pagopa.receipt.pdf.notifier.exception;

import lombok.Getter;

/**
 * Thrown in case an error occur when invoking PDV Tokenizer service
 */
@Getter
public class PDVTokenizerException extends Exception {

    private final int statusCode;

    /**
     * Constructs new exception with provided message
     *
     * @param message Detail message
     * @param statusCode status code
     */
    public PDVTokenizerException(String message, int statusCode) {
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
    public PDVTokenizerException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
