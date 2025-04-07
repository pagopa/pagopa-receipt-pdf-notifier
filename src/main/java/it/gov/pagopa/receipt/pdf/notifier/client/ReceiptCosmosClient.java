package it.gov.pagopa.receipt.pdf.notifier.client;


import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;

public interface ReceiptCosmosClient {

    Receipt getReceiptDocument(String receiptId) throws ReceiptNotFoundException;

    IOMessage findIOMessageWithEventIdAndUserType(String eventId, UserType userType) throws IoMessageNotFoundException;
}
