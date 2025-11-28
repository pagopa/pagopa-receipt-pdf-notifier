package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.receipt.pdf.notifier.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.CartIOMessage;
import it.gov.pagopa.receipt.pdf.notifier.exception.CartIoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;

/**
 * {@inheritDoc}
 */
public class CartReceiptCosmosClientImpl implements CartReceiptCosmosClient {

    private static CartReceiptCosmosClientImpl instance;

    private final String databaseId = System.getenv("COSMOS_RECEIPT_DB_NAME");
    private final String containerMessageId = System.getenv("COSMOS_CART_RECEIPT_MESSAGE_CONTAINER_NAME");

    private final CosmosClient cosmosClient;

    private CartReceiptCosmosClientImpl() {
        String azureKey = System.getenv("COSMOS_RECEIPT_KEY");
        String serviceEndpoint = System.getenv("COSMOS_RECEIPT_SERVICE_ENDPOINT");

        this.cosmosClient = new CosmosClientBuilder()
                .endpoint(serviceEndpoint)
                .key(azureKey)
                .buildClient();
    }

    CartReceiptCosmosClientImpl(CosmosClient cosmosClient) {
        this.cosmosClient = cosmosClient;
    }

    public static CartReceiptCosmosClientImpl getInstance() {
        if (instance == null) {
            instance = new CartReceiptCosmosClientImpl();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CartIOMessage findIOMessageWithCartIdAndEventIdAndUserType(
            String cartId,
            String eventId,
            UserType userType
    ) throws CartIoMessageNotFoundException {
        CosmosDatabase cosmosDatabase = this.cosmosClient.getDatabase(databaseId);
        CosmosContainer cosmosContainer = cosmosDatabase.getContainer(containerMessageId);

        //Build query
        String query;
        if (UserType.PAYER.equals(userType)) {
            query = String.format(
                    "SELECT * FROM c WHERE  c.cartId = '%s' AND c.userType = '%s'",
                    cartId, userType);
        } else {
            query = String.format(
                    "SELECT * FROM c WHERE  c.cartId = '%s' AND c.eventId = '%s' AND c.userType = '%s'",
                    cartId, eventId, userType);
        }

        //Query the container
        CosmosPagedIterable<CartIOMessage> queryResponse = cosmosContainer
                .queryItems(query, new CosmosQueryRequestOptions(), CartIOMessage.class);

        if (queryResponse.iterator().hasNext()) {
            return queryResponse.iterator().next();
        }
        throw new CartIoMessageNotFoundException("Document not found in the defined container");
    }
}
