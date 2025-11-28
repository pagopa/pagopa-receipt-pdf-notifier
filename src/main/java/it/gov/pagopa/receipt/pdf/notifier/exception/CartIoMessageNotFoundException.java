package it.gov.pagopa.receipt.pdf.notifier.exception;

/** Thrown in case no cart receipt message to IO is found in the CosmosDB container */
public class CartIoMessageNotFoundException extends Exception{

    /**
     * Constructs new exception with provided message and cause
     *
     * @param message Detail message
     */
    public CartIoMessageNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs new exception with provided message and cause
     *
     * @param message Detail message
     * @param cause Exception thrown
     */
    public CartIoMessageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}