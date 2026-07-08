package it.gov.pagopa.receipt.pdf.notifier.client;


import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;

/**
 * Client for the CosmosDB database
 */
public interface ReceiptCosmosClient {

    /**
     * Retrieve io message document from CosmosDB database with the provided event id and user type
     *
     * @param eventId  Receipt event id
     * @param userType Recipient of the IO message
     * @return io message document
     * @throws IoMessageNotFoundException in case no receipt has been found with the given eventId and user type
     */
    IOMessage findIOMessageWithEventIdAndUserType(String eventId, UserType userType) throws IoMessageNotFoundException;
}
