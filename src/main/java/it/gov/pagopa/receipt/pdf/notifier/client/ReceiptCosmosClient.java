package it.gov.pagopa.receipt.pdf.notifier.client;


import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.exception.ReceiptNotFoundException;

public interface ReceiptCosmosClient {

    Receipt getReceiptDocument(String receiptId) throws ReceiptNotFoundException;
}
