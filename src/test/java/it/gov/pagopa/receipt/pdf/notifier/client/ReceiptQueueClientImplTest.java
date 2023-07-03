package it.gov.pagopa.receipt.pdf.notifier.client;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.models.SendMessageResult;
import com.microsoft.azure.functions.HttpStatus;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.NotifierQueueClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

class ReceiptQueueClientImplTest {

    @Test
    void testSingletonConnectionError() throws Exception {
        @SuppressWarnings("secrets:S6338")
        String mockKey = "mockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeyMK==";
        withEnvironmentVariables(
                "STORAGE_CONN_STRING", "DefaultEndpointsProtocol=https;AccountName=samplequeue;AccountKey="+mockKey+";EndpointSuffix=core.windows.net",
                "NOTIFIER_QUEUE_TOPIC", "validTopic"
        ).execute(() -> Assertions.assertDoesNotThrow(NotifierQueueClientImpl::getInstance)
        );
    }

    @Test
    void runOk() {
        String MESSAGE_TEXT = "a valid message text";

        Response<SendMessageResult> response = mock(Response.class);
        QueueClient mockClient = mock(QueueClient.class);

        when(response.getStatusCode()).thenReturn(HttpStatus.CREATED.value());
        when(mockClient.sendMessageWithResponse(eq(MESSAGE_TEXT), any(), eq(null), eq(null), eq(null)))
                .thenReturn(response);

        NotifierQueueClientImpl client = new NotifierQueueClientImpl(mockClient);

        Response<SendMessageResult> clientResponse = client.sendMessageToQueue(MESSAGE_TEXT);

        Assertions.assertEquals(HttpStatus.CREATED.value(), clientResponse.getStatusCode());
    }

    @Test
    void runKo() {
        String MESSAGE_TEXT = "an invalid message text";

        Response<SendMessageResult> response = mock(Response.class);
        QueueClient mockClient = mock(QueueClient.class);

        when(response.getStatusCode()).thenReturn(HttpStatus.NO_CONTENT.value());
        when(mockClient.sendMessageWithResponse(eq(MESSAGE_TEXT), any(), eq(null), eq(null), eq(null)))
                .thenReturn(response);

        NotifierQueueClientImpl client = new NotifierQueueClientImpl(mockClient);

        Response<SendMessageResult> clientResponse = client.sendMessageToQueue(MESSAGE_TEXT);

        Assertions.assertEquals(HttpStatus.NO_CONTENT.value(), clientResponse.getStatusCode());
    }
}