package it.gov.pagopa.receipt.pdf.notifier.exception;

/** Thrown in case no receipt is found in the CosmosDB container */
public class ReceiptNotFoundException extends Exception{

    /**
     * Constructs new exception with provided message
     *
     * @param message Detail message
     */
    public ReceiptNotFoundException(String message) {
        super(message);
    }
}


