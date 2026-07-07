package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import it.gov.pagopa.receipt.pdf.notifier.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;

import java.util.Arrays;

/**
 * {@inheritDoc}
 */
public class ReceiptCosmosClientImpl implements ReceiptCosmosClient {

    private static ReceiptCosmosClientImpl instance;

    private final String databaseId = System.getenv("COSMOS_RECEIPT_DB_NAME");
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
     * {@inheritDoc}
     */
    @Override
    public IOMessage findIOMessageWithEventIdAndUserType(
            String eventId,
            UserType userType
    ) throws IoMessageNotFoundException {
        CosmosDatabase cosmosDatabase = this.cosmosClient.getDatabase(databaseId);
        CosmosContainer cosmosContainer = cosmosDatabase.getContainer(containerMessageId);

        //Build query
        SqlQuerySpec querySpec = new SqlQuerySpec(
                "SELECT * FROM c WHERE c.eventId = @eventId AND c.userType = @userType ",
                Arrays.asList(
                        new SqlParameter("@eventId", eventId),
                        new SqlParameter("@userType", userType)
                )
        );
        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        options.setPartitionKey(new PartitionKey(eventId));

        //Query the container
        return cosmosContainer
                .queryItems(querySpec, options, IOMessage.class)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IoMessageNotFoundException("Document not found in the defined container"));
    }
}
