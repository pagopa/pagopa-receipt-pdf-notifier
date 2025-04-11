package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.receipt.pdf.notifier.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;

/**
 * Client for the CosmosDB database
 */
public class ReceiptCosmosClientImpl implements ReceiptCosmosClient {

    private static ReceiptCosmosClientImpl instance;

    private final String databaseId = System.getenv("COSMOS_RECEIPT_DB_NAME");
    private final String containerId = System.getenv("COSMOS_RECEIPT_CONTAINER_NAME");
    private final String containerMessageId = System.getenv("COSMOS_RECEIPT_MESSAGE_CONTAINER_NAME");

    private final CosmosClient cosmosClient;

    private ReceiptCosmosClientImpl() {
        String azureKey = System.getenv("COSMOS_RECEIPT_KEY");
        String serviceEndpoint = System.getenv("COSMOS_RECEIPT_SERVICE_ENDPOINT");

        this.cosmosClient = new CosmosClientBuilder()
                .endpoint(serviceEndpoint)
                .key(azureKey)
                .buildClient();
    }

    ReceiptCosmosClientImpl(CosmosClient cosmosClient) {
        this.cosmosClient = cosmosClient;
    }

    public static ReceiptCosmosClientImpl getInstance() {
        if (instance == null) {
            instance = new ReceiptCosmosClientImpl();
        }

        return instance;
    }

    /**
     * Retrieve receipt document from CosmosDB database
     *
     * @param eventId Biz-event id
     * @return receipt document
     * @throws ReceiptNotFoundException in case no receipt has been found with the given idEvent
     */
    public Receipt getReceiptDocument(String eventId) throws ReceiptNotFoundException {
        CosmosDatabase cosmosDatabase = this.cosmosClient.getDatabase(databaseId);

        CosmosContainer cosmosContainer = cosmosDatabase.getContainer(containerId);

        //Build query
        String query = "SELECT * FROM c WHERE c.eventId = " + "'" + eventId + "'";

        //Query the container
        CosmosPagedIterable<Receipt> queryResponse = cosmosContainer
                .queryItems(query, new CosmosQueryRequestOptions(), Receipt.class);

        if (queryResponse.iterator().hasNext()) {
            return queryResponse.iterator().next();
        } else {
            throw new ReceiptNotFoundException("Document not found in the defined container");
        }
    }

    /**
     * Retrieve io message document from CosmosDB database with the provided event id and user type
     *
     * @param eventId  Receipt event id
     * @param userType Recipient of the IO message
     * @return io message document
     * @throws IoMessageNotFoundException in case no receipt has been found with the given eventId and user type
     */
    @Override
    public IOMessage findIOMessageWithEventIdAndUserType(String eventId, UserType userType) throws IoMessageNotFoundException {
        CosmosDatabase cosmosDatabase = this.cosmosClient.getDatabase(databaseId);
        CosmosContainer cosmosContainer = cosmosDatabase.getContainer(containerMessageId);

        //Build query
        String query = String.format("SELECT * FROM c WHERE c.eventId = '%s' AND c.userType = '%s'", eventId, userType);

        //Query the container
        CosmosPagedIterable<IOMessage> queryResponse = cosmosContainer
                .queryItems(query, new CosmosQueryRequestOptions(), IOMessage.class);

        if (queryResponse.iterator().hasNext()) {
            return queryResponse.iterator().next();
        }
        throw new IoMessageNotFoundException("Document not found in the defined container");
    }
}
