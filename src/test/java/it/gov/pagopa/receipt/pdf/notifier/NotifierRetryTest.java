package it.gov.pagopa.receipt.pdf.notifier;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.ReceiptCosmosClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.exception.ReceiptNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotifierRetryTest {

    private final static String VALID_QUEUE_MESSAGE_BASE64 = "dmFsaWQgYml6IGV2ZW50IGlk";
    private final static String INVALID_BIZ_EVENT_ID_QUEUE_MESSAGE = "aW52YWxpZCBiaXogZXZlbnQgaWQ=";

    @Spy
    NotifierRetry function;

    @Mock
    private ExecutionContext context;

    @Mock
    private ReceiptCosmosClientImpl client;

    @Captor
    private ArgumentCaptor<List<Receipt>> receiptCaptor;

    @AfterEach
    public void teardown() throws Exception {
        // reset singleton
        Field instance = ReceiptCosmosClientImpl.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    void runOk() throws ReceiptNotFoundException {
        Logger logger = Logger.getLogger("NotifierRetry-test-logger");
        when(context.getLogger()).thenReturn(logger);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);

        Receipt receipt = new Receipt();
        receipt.setStatus(ReceiptStatusType.IO_ERROR_TO_NOTIFY);

        when(client.getReceiptDocument(anyString())).thenReturn(receipt);

        setMock(client);

        assertDoesNotThrow(() -> function.processNotifierRetry(VALID_QUEUE_MESSAGE_BASE64, documentReceipts, context));

        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertEquals(ReceiptStatusType.IO_NOTIFIER_RETRY, updatedReceipt.getStatus());
    }

    @Test
    void runReceiptNotFound() throws ReceiptNotFoundException {
        Logger logger = Logger.getLogger("NotifierRetry-test-logger");
        when(context.getLogger()).thenReturn(logger);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);

        when(client.getReceiptDocument(anyString())).thenThrow(ReceiptNotFoundException.class);

        setMock(client);

        assertThrows(ReceiptNotFoundException.class,
                () -> function.processNotifierRetry(INVALID_BIZ_EVENT_ID_QUEUE_MESSAGE, documentReceipts, context));

        verify(documentReceipts, never()).setValue(any());
    }

    @Test
    void runReceiptWrongStatus() throws ReceiptNotFoundException {
        Logger logger = Logger.getLogger("NotifierRetry-test-logger");
        when(context.getLogger()).thenReturn(logger);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);

        Receipt receipt = new Receipt();
        receipt.setStatus(ReceiptStatusType.UNABLE_TO_SEND);

        when(client.getReceiptDocument(anyString())).thenReturn(receipt);

        setMock(client);

        assertDoesNotThrow(() -> function.processNotifierRetry(VALID_QUEUE_MESSAGE_BASE64, documentReceipts, context));

        verify(documentReceipts, never()).setValue(any());
    }

    private static void setMock(ReceiptCosmosClientImpl mock) {
        try {
            Field instance = ReceiptCosmosClientImpl.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}