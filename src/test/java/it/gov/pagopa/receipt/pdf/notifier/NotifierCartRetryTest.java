package it.gov.pagopa.receipt.pdf.notifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartStatusType;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotifierCartRetryTest {

    @Spy
    NotifierCartRetry function;

    @Mock
    private ExecutionContext context;

    @Captor
    private ArgumentCaptor<List<CartForReceipt>> receiptCaptor;

    @Test
    void runOk() throws JsonProcessingException {
        @SuppressWarnings("unchecked")
        OutputBinding<List<CartForReceipt>> documentReceipts = (OutputBinding<List<CartForReceipt>>) spy(OutputBinding.class);

        CartForReceipt receipt = new CartForReceipt();
        receipt.setStatus(CartStatusType.IO_ERROR_TO_NOTIFY);

        String queueReceipt = ObjectMapperUtils.writeValueAsString(receipt);

        assertDoesNotThrow(() -> function.processNotifierRetry(queueReceipt, documentReceipts, context));

        verify(documentReceipts).setValue(receiptCaptor.capture());
        CartForReceipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertEquals(CartStatusType.IO_NOTIFIER_RETRY, updatedReceipt.getStatus());
    }

    @Test
    void runReceiptWrongStatus() throws JsonProcessingException {
        @SuppressWarnings("unchecked")
        OutputBinding<List<CartForReceipt>> documentReceipts = (OutputBinding<List<CartForReceipt>>) spy(OutputBinding.class);

        CartForReceipt receipt = new CartForReceipt();
        receipt.setStatus(CartStatusType.UNABLE_TO_SEND);

        String queueReceipt = ObjectMapperUtils.writeValueAsString(receipt);

        assertDoesNotThrow(() -> function.processNotifierRetry(queueReceipt, documentReceipts, context));
        verify(documentReceipts, never()).setValue(any());
    }
}