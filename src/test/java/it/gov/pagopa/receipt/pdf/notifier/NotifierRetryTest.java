package it.gov.pagopa.receipt.pdf.notifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.utils.ObjectMapperUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotifierRetryTest {

    @Spy
    NotifierRetry function;

    @Mock
    private ExecutionContext context;

    @Captor
    private ArgumentCaptor<List<Receipt>> receiptCaptor;

    @Test
    void runOk() throws JsonProcessingException {
        Logger logger = Logger.getLogger("NotifierRetry-test-logger");
        when(context.getLogger()).thenReturn(logger);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);

        Receipt receipt = new Receipt();
        receipt.setStatus(ReceiptStatusType.IO_ERROR_TO_NOTIFY);

        String queueReceipt = ObjectMapperUtils.writeValueAsString(receipt);

        assertDoesNotThrow(() -> function.processNotifierRetry(queueReceipt, documentReceipts, context));

        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertEquals(ReceiptStatusType.IO_NOTIFIER_RETRY, updatedReceipt.getStatus());
    }

    @Test
    void runReceiptWrongStatus() throws JsonProcessingException {
        Logger logger = Logger.getLogger("NotifierRetry-test-logger");
        when(context.getLogger()).thenReturn(logger);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);

        Receipt receipt = new Receipt();
        receipt.setStatus(ReceiptStatusType.UNABLE_TO_SEND);

        String queueReceipt = ObjectMapperUtils.writeValueAsString(receipt);

        assertDoesNotThrow(() -> function.processNotifierRetry(queueReceipt, documentReceipts, context));

        verify(documentReceipts, never()).setValue(any());
    }

}