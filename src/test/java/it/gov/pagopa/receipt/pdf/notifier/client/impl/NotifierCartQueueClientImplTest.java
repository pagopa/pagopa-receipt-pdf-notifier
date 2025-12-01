package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.models.SendMessageResult;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

@ExtendWith(MockitoExtension.class)
class NotifierCartQueueClientImplTest {

    private static final String MESSAGE_TEXT = "a valid message text";

    @Mock
    private QueueClient queueClient;

    @Mock
    private Response<SendMessageResult> queueResponseMock;

    @InjectMocks
    private NotifierCartQueueClientImpl sut;

    @Test
    void testSingletonConnectionError() throws Exception {
        @SuppressWarnings("secrets:S6338")
        String mockKey = "mockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeyMK==";
        withEnvironmentVariables(
                "STORAGE_CONN_STRING", "DefaultEndpointsProtocol=https;AccountName=samplequeue;AccountKey=" + mockKey + ";EndpointSuffix=core.windows.net",
                "NOTIFIER_CART_QUEUE_TOPIC", "validTopic"
        ).execute(() -> assertDoesNotThrow(NotifierCartQueueClientImpl::getInstance)
        );
    }

    @Test
    void runOk() {
        when(queueResponseMock.getStatusCode()).thenReturn(HttpStatus.CREATED.value());
        when(queueClient.sendMessageWithResponse(eq(MESSAGE_TEXT), any(), eq(null), eq(null), eq(null)))
                .thenReturn(queueResponseMock);

        Response<SendMessageResult> result = assertDoesNotThrow(() -> sut.sendMessageToQueue(MESSAGE_TEXT));

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED.value(), result.getStatusCode());
    }

    @Test
    void runKo() {
        when(queueResponseMock.getStatusCode()).thenReturn(HttpStatus.NO_CONTENT.value());
        when(queueClient.sendMessageWithResponse(eq(MESSAGE_TEXT), any(), eq(null), eq(null), eq(null)))
                .thenReturn(queueResponseMock);

        Response<SendMessageResult> result = assertDoesNotThrow(() -> sut.sendMessageToQueue(MESSAGE_TEXT));

        assertNotNull(result);
        assertEquals(HttpStatus.NO_CONTENT.value(), result.getStatusCode());
    }
}