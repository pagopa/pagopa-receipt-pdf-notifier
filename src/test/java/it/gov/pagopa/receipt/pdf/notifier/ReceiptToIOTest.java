package it.gov.pagopa.receipt.pdf.notifier;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.EventData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.service.ReceiptToIOService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
class ReceiptToIOTest {

    private final String VALID_PAYER_CF = "a valid payer fiscal code";
    private final String VALID_DEBTOR_CF = "a valid debtor fiscal code";

    private ReceiptToIOService receiptToIOServiceMock;

    private OutputBinding<List<Receipt>> documentReceiptsMock;
    private OutputBinding<List<IOMessage>> documentMessagesMock;

    private ExecutionContext executionContextMock;

    private ReceiptToIO sut;


    @BeforeEach
    void setUp() {
        receiptToIOServiceMock = mock(ReceiptToIOService.class);
        documentReceiptsMock = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        documentMessagesMock = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        executionContextMock = mock(ExecutionContext.class);
        sut = new ReceiptToIO(receiptToIOServiceMock);
    }

    @Test
    @SneakyThrows
    void receiptToIOSuccessWithDebtorAndStatusGenerated() {
        doReturn(UserNotifyStatus.NOTIFIED).when(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        doReturn(false).when(receiptToIOServiceMock).verifyMessagesNotification(any(), anyList(), any());

        Receipt receipt = new Receipt();
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        receipt.setEventData(eventData);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        sut.processReceiptToIO(Collections.singletonList(receipt), documentReceiptsMock, documentMessagesMock, executionContextMock);

        verify(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        verify(receiptToIOServiceMock).verifyMessagesNotification(any(), anyList(), any());
    }

    @Test
    @SneakyThrows
    void receiptToIOSuccessWithDebtorAndStatusSigned() {
        doReturn(UserNotifyStatus.NOTIFIED).when(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        doReturn(false).when(receiptToIOServiceMock).verifyMessagesNotification(any(), anyList(), any());

        Receipt receipt = new Receipt();
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        receipt.setEventData(eventData);
        receipt.setStatus(ReceiptStatusType.SIGNED);

        sut.processReceiptToIO(Collections.singletonList(receipt), documentReceiptsMock, documentMessagesMock, executionContextMock);

        verify(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        verify(receiptToIOServiceMock).verifyMessagesNotification(any(), anyList(), any());
    }

    @Test
    @SneakyThrows
    void receiptToIOSuccessWithDebtorAndStatusIONotifierRetry() {
        doReturn(UserNotifyStatus.NOTIFIED).when(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        doReturn(false).when(receiptToIOServiceMock).verifyMessagesNotification(any(), anyList(), any());

        Receipt receipt = new Receipt();
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        receipt.setEventData(eventData);
        receipt.setStatus(ReceiptStatusType.IO_NOTIFIER_RETRY);

        sut.processReceiptToIO(Collections.singletonList(receipt), documentReceiptsMock, documentMessagesMock, executionContextMock);

        verify(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        verify(receiptToIOServiceMock).verifyMessagesNotification(any(), anyList(), any());
    }

    @Test
    @SneakyThrows
    void receiptToIOSuccessWithDebtorAndPayer() {
        doReturn(UserNotifyStatus.NOTIFIED).when(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        doReturn(false).when(receiptToIOServiceMock).verifyMessagesNotification(any(), anyList(), any());

        Receipt receipt = new Receipt();
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        eventData.setPayerFiscalCode(VALID_PAYER_CF);
        receipt.setEventData(eventData);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        sut.processReceiptToIO(Collections.singletonList(receipt), documentReceiptsMock, documentMessagesMock, executionContextMock);

        verify(receiptToIOServiceMock, times(2)).notifyMessage(anyString(), any(), any());
        verify(receiptToIOServiceMock).verifyMessagesNotification(any(), anyList(), any());
    }

    @Test
    @SneakyThrows
    void receiptToIOFailWithOnlyPayer() {
        Receipt receipt = new Receipt();
        EventData eventData = new EventData();
        eventData.setPayerFiscalCode(VALID_PAYER_CF);
        receipt.setEventData(eventData);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        sut.processReceiptToIO(Collections.singletonList(receipt), documentReceiptsMock, documentMessagesMock, executionContextMock);

        verify(receiptToIOServiceMock, never()).notifyMessage(anyString(), any(), any());
        verify(receiptToIOServiceMock, never()).verifyMessagesNotification(any(), anyList(), any());
    }

    @Test
    @SneakyThrows
    void receiptToIOFailVerifyTriggerRequeue() {
        doReturn(UserNotifyStatus.NOT_NOTIFIED).when(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        doReturn(true).when(receiptToIOServiceMock).verifyMessagesNotification(any(), anyList(), any());

        Receipt receipt = new Receipt();
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        receipt.setEventData(eventData);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        sut.processReceiptToIO(Collections.singletonList(receipt), documentReceiptsMock, documentMessagesMock, executionContextMock);

        verify(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        verify(receiptToIOServiceMock).verifyMessagesNotification(any(), anyList(), any());
    }
}