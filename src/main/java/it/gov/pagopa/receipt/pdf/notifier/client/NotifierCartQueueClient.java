package it.gov.pagopa.receipt.pdf.notifier.client;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.models.SendMessageResult;

/**
 * Client for the Cart Queue
 */
public interface NotifierCartQueueClient {

    /**
     * Send string message to the cart queue
     *
     * @param messageText Biz-event encoded to base64 string
     * @return response from the queue
     */
    Response<SendMessageResult> sendMessageToQueue(String messageText);
}
