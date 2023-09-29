package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.SendMessageResult;
import it.gov.pagopa.receipt.pdf.notifier.client.NotifierQueueClient;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Client for the Queue
 */
public class NotifierQueueClientImpl implements NotifierQueueClient {

    private static NotifierQueueClientImpl instance;

    private final int receiptQueueDelay = Integer.parseInt(System.getenv().getOrDefault("NOTIFIER_QUEUE_DELAY", "1"));

    private final QueueClient queueClient;

    private NotifierQueueClientImpl() {
        String receiptQueueConnString = System.getenv("STORAGE_CONN_STRING");
        String receiptQueueTopic = System.getenv("NOTIFIER_QUEUE_TOPIC");

        this.queueClient = new QueueClientBuilder()
                .connectionString(receiptQueueConnString)
                .queueName(receiptQueueTopic)
                .buildClient();
    }

    NotifierQueueClientImpl(QueueClient queueClient) {
        this.queueClient = queueClient;
    }

    public static NotifierQueueClientImpl getInstance() {
        if (instance == null) {
            instance = new NotifierQueueClientImpl();
        }

        return instance;
    }

    /**
     * Send string message to the queue
     *
     * @param messageText Biz-event encoded to base64 string
     * @return response from the queue
     */
    public Response<SendMessageResult> sendMessageToQueue(String messageText) {

        return this.queueClient.sendMessageWithResponse(
                messageText, Duration.of(receiptQueueDelay, ChronoUnit.SECONDS),
                null, null, null);

    }
}
