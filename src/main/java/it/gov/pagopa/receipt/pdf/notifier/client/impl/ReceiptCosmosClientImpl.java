package it.gov.pagopa.receipt.pdf.notifier.client.impl;

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

    private final CosmosContainer ioMessageContainer;

    @SuppressWarnings("resource") // CosmosClient lifecycle == singleton lifecycle; never closed on purpose
    private ReceiptCosmosClientImpl() {
        String azureKey = System.getenv("COSMOS_RECEIPT_KEY");
        String serviceEndpoint = System.getenv("COSMOS_RECEIPT_SERVICE_ENDPOINT");

        String databaseId = System.getenv("COSMOS_RECEIPT_DB_NAME");
        String containerMessageId = System.getenv("COSMOS_RECEIPT_MESSAGE_CONTAINER_NAME");

        CosmosDatabase cosmosDatabase = new CosmosClientBuilder()
                .endpoint(serviceEndpoint)
                .key(azureKey)
                .buildClient()
                .getDatabase(databaseId);

        this.ioMessageContainer = cosmosDatabase.getContainer(containerMessageId);
    }

    ReceiptCosmosClientImpl(CosmosContainer ioMessageContainer) {
        this.ioMessageContainer = ioMessageContainer;
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
        return ioMessageContainer
                .queryItems(querySpec, options, IOMessage.class)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IoMessageNotFoundException("Document not found in the defined container"));
    }
}
