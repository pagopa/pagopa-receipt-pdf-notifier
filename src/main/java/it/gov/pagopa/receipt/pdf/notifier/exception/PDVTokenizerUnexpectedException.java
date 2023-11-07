package it.gov.pagopa.receipt.pdf.notifier.exception;

import lombok.Getter;

/**
 * Thrown in case an unexpected error occur when invoking PDV Tokenizer service
 */
@Getter
public class PDVTokenizerUnexpectedException extends RuntimeException {

    /**
     * Constructs new exception with provided cause
     *
     * @param cause Exception causing the constructed one
     */
    public PDVTokenizerUnexpectedException(Throwable cause) {
        super(cause);
    }
}
