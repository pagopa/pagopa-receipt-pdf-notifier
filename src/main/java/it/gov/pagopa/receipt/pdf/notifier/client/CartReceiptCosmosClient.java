package it.gov.pagopa.receipt.pdf.notifier.client;


import it.gov.pagopa.receipt.pdf.notifier.entity.message.CartIOMessage;
import it.gov.pagopa.receipt.pdf.notifier.exception.CartIoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;

/**
 * Client for the CosmosDB database
 */
public interface CartReceiptCosmosClient {

    /**
     * Retrieve io message document from CosmosDB database with the provided event id and user type
     *
     * @param cartId   cart identifier
     * @param eventId  Biz event id, (used only when the recipient is a {@link UserType#DEBTOR}
     * @param userType Recipient of the IO message
     * @return io message document
     * @throws CartIoMessageNotFoundException in case no cart receipt has been found with the given cartId, eventId and user type
     */
    CartIOMessage findIOMessageWithCartIdAndEventIdAndUserType(
            String cartId,
            String eventId,
            UserType userType
    ) throws CartIoMessageNotFoundException;
}
