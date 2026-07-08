package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.SendMessageResult;
import it.gov.pagopa.receipt.pdf.notifier.client.NotifierCartQueueClient;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * {@inheritDoc}
 */
public class NotifierCartQueueClientImpl implements NotifierCartQueueClient {

    private final int cartReceiptQueueDelay = Integer.parseInt(System.getenv().getOrDefault("NOTIFIER_CART_QUEUE_DELAY", "1"));

    private final QueueClient queueClient;

    private NotifierCartQueueClientImpl() {
        String receiptQueueConnString = System.getenv("STORAGE_CONN_STRING");
        String cartReceiptQueueTopic = System.getenv("NOTIFIER_CART_QUEUE_TOPIC");

        this.queueClient = new QueueClientBuilder()
                .connectionString(receiptQueueConnString)
                .queueName(cartReceiptQueueTopic)
                .buildClient();
    }

    NotifierCartQueueClientImpl(QueueClient queueClient) {
        this.queueClient = queueClient;
    }

    public static NotifierCartQueueClientImpl getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Bill Pugh singleton holder: the JVM guarantees that the class is loaded
     * (and therefore INSTANCE initialized) lazily and in a thread-safe way.
     */
    private static class SingletonHelper {
        private static final NotifierCartQueueClientImpl INSTANCE = new NotifierCartQueueClientImpl();
    }

    /**
     * {@inheritDoc}
     */
    public Response<SendMessageResult> sendMessageToQueue(String messageText) {
        return this.queueClient.sendMessageWithResponse(
                messageText, Duration.of(cartReceiptQueueDelay, ChronoUnit.SECONDS),
                null, null, null);

    }
}
