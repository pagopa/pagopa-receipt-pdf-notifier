package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import it.gov.pagopa.receipt.pdf.notifier.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.CartIOMessage;
import it.gov.pagopa.receipt.pdf.notifier.exception.CartIoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;

import java.util.Arrays;

/**
 * {@inheritDoc}
 */
public class CartReceiptCosmosClientImpl implements CartReceiptCosmosClient {

    private final CosmosContainer cartIoMessageContainer;

    @SuppressWarnings("resource") // CosmosClient lifecycle == singleton lifecycle; never closed on purpose
    private CartReceiptCosmosClientImpl() {
        String azureKey = System.getenv("COSMOS_RECEIPT_KEY");
        String serviceEndpoint = System.getenv("COSMOS_RECEIPT_SERVICE_ENDPOINT");

        String databaseId = System.getenv("COSMOS_RECEIPT_DB_NAME");
        String containerMessageId = System.getenv("COSMOS_CART_RECEIPT_MESSAGE_CONTAINER_NAME");

        CosmosDatabase cosmosDatabase = new CosmosClientBuilder()
                .endpoint(serviceEndpoint)
                .key(azureKey)
                .buildClient()
                .getDatabase(databaseId);

        this.cartIoMessageContainer = cosmosDatabase.getContainer(containerMessageId);
    }

    CartReceiptCosmosClientImpl(CosmosContainer cartIoMessageContainer) {
        this.cartIoMessageContainer = cartIoMessageContainer;
    }

    public static CartReceiptCosmosClientImpl getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Bill Pugh singleton holder: the JVM guarantees that the class is loaded
     * (and therefore INSTANCE initialized) lazily and in a thread-safe way.
     */
    private static class SingletonHelper {
        private static final CartReceiptCosmosClientImpl INSTANCE = new CartReceiptCosmosClientImpl();
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
        //Build query
        SqlQuerySpec querySpec = buildQuery(cartId, eventId, userType);
        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        options.setPartitionKey(new PartitionKey(cartId));

        //Query the container
        return cartIoMessageContainer.queryItems(querySpec, options, CartIOMessage.class)
                .stream()
                .findFirst()
                .orElseThrow(() -> new CartIoMessageNotFoundException("Document not found in the defined container"));
    }

    private SqlQuerySpec buildQuery(String cartId, String eventId, UserType userType) {
        SqlQuerySpec querySpec;
        if (UserType.PAYER.equals(userType)) {
            querySpec = new SqlQuerySpec(
                    "SELECT * FROM c WHERE c.cartId = @cartId AND c.userType = @userType ",
                    Arrays.asList(
                            new SqlParameter("@cartId", cartId),
                            new SqlParameter("@userType", userType)
                    )
            );
        } else {
            querySpec = new SqlQuerySpec(
                    "SELECT * FROM c WHERE c.cartId = @cartId AND c.eventId = @eventId AND c.userType = @userType ",
                    Arrays.asList(
                            new SqlParameter("@cartId", cartId),
                            new SqlParameter("@eventId", eventId),
                            new SqlParameter("@userType", userType)
                    )
            );
        }
        return querySpec;
    }
}
