package it.gov.pagopa.receipt.pdf.notifier.client;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.models.SendMessageResult;

public interface NotifierQueueClient {

    Response<SendMessageResult> sendMessageToQueue(String messageText);
}
