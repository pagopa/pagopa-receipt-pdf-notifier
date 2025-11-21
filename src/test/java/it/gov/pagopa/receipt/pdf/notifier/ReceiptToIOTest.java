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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.Collections;
import java.util.List;

import static it.gov.pagopa.receipt.pdf.notifier.utils.ReceiptToIOUtils.ANONIMO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
class ReceiptToIOTest {

    private static final String VALID_PAYER_CF = "a valid payer fiscal code";
    private static final String VALID_DEBTOR_CF = "a valid debtor fiscal code";

    @Mock
    private ReceiptToIOService receiptToIOServiceMock;
    @Mock
    private OutputBinding<List<Receipt>> documentReceiptsMock;
    @Mock
    private OutputBinding<List<IOMessage>> documentMessagesMock;
    @Mock
    private ExecutionContext executionContextMock;

    private ReceiptToIO sut;

    @ParameterizedTest
    @EnumSource(value = ReceiptStatusType.class, names = {"GENERATED", "SIGNED", "IO_NOTIFIER_RETRY"})
    @SneakyThrows
    void receiptToIOSuccessWithDebtorAndStatusGenerated(ReceiptStatusType status) {
        doReturn(UserNotifyStatus.NOTIFIED).when(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        doReturn(Collections.singletonList(new IOMessage()))
                .when(receiptToIOServiceMock).verifyMessagesNotification(any(), any());

        Receipt receipt = new Receipt();
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        receipt.setEventData(eventData);
        receipt.setStatus(status);

        withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new ReceiptToIO(receiptToIOServiceMock);
                    sut.processReceiptToIO(Collections.singletonList(receipt), documentReceiptsMock, documentMessagesMock, executionContextMock);
                });

        verify(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        verify(receiptToIOServiceMock).verifyMessagesNotification(any(), any());
        verify(documentReceiptsMock).setValue(anyList());
        verify(documentMessagesMock).setValue(anyList());
    }

    @Test
    @SneakyThrows
    void receiptToIOSuccessWithDebtorAndPayer() {
        doReturn(UserNotifyStatus.NOTIFIED).when(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        doReturn(List.of(new IOMessage(), new IOMessage()))
                .when(receiptToIOServiceMock).verifyMessagesNotification(any(), any());

        Receipt receipt = new Receipt();
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        eventData.setPayerFiscalCode(VALID_PAYER_CF);
        receipt.setEventData(eventData);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new ReceiptToIO(receiptToIOServiceMock);
                    sut.processReceiptToIO(Collections.singletonList(receipt), documentReceiptsMock, documentMessagesMock, executionContextMock);
                });

        verify(receiptToIOServiceMock, times(2)).notifyMessage(anyString(), any(), any());
        verify(receiptToIOServiceMock).verifyMessagesNotification(any(), any());
        verify(documentReceiptsMock).setValue(anyList());
        verify(documentMessagesMock).setValue(anyList());
    }

    @Test
    @SneakyThrows
    void receiptToIOSuccessWithDebtorAnonimo() {
        doReturn(Collections.emptyList()).when(receiptToIOServiceMock).verifyMessagesNotification(any(), any());

        Receipt receipt = new Receipt();
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(ANONIMO);
        receipt.setEventData(eventData);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new ReceiptToIO(receiptToIOServiceMock);
                    sut.processReceiptToIO(Collections.singletonList(receipt), documentReceiptsMock, documentMessagesMock, executionContextMock);
                });

        verify(receiptToIOServiceMock, never()).notifyMessage(anyString(), any(), any());
        verify(receiptToIOServiceMock).verifyMessagesNotification(any(), any());
        verify(documentReceiptsMock).setValue(anyList());
        verify(documentMessagesMock, never()).setValue(anyList());
    }

    @Test
    @SneakyThrows
    void receiptToIOFailWithOnlyPayer() {
        Receipt receipt = new Receipt();
        EventData eventData = new EventData();
        eventData.setPayerFiscalCode(VALID_PAYER_CF);
        receipt.setEventData(eventData);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new ReceiptToIO(receiptToIOServiceMock);
                    sut.processReceiptToIO(Collections.singletonList(receipt), documentReceiptsMock, documentMessagesMock, executionContextMock);
                });

        verify(receiptToIOServiceMock, never()).notifyMessage(anyString(), any(), any());
        verify(receiptToIOServiceMock, never()).verifyMessagesNotification(any(), any());
        verify(documentReceiptsMock, never()).setValue(anyList());
        verify(documentMessagesMock, never()).setValue(anyList());
    }

    @Test
    @SneakyThrows
    void receiptToIOFailVerifyTriggerRequeue() {
        doReturn(UserNotifyStatus.NOT_NOTIFIED).when(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        doReturn(Collections.emptyList()).when(receiptToIOServiceMock).verifyMessagesNotification(any(), any());

        Receipt receipt = new Receipt();
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        receipt.setEventData(eventData);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new ReceiptToIO(receiptToIOServiceMock);
                    sut.processReceiptToIO(Collections.singletonList(receipt), documentReceiptsMock, documentMessagesMock, executionContextMock);
                });

        verify(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        verify(receiptToIOServiceMock).verifyMessagesNotification(any(), any());
        verify(documentReceiptsMock).setValue(anyList());
        verify(documentMessagesMock, never()).setValue(anyList());
    }

    @Test
    @SneakyThrows
    void receiptToIOPayerNotNotifiedBecauseDisabled() {
        doReturn(UserNotifyStatus.NOT_NOTIFIED).when(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        doReturn(Collections.emptyList()).when(receiptToIOServiceMock).verifyMessagesNotification(any(), any());

        Receipt receipt = new Receipt();
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        eventData.setPayerFiscalCode(VALID_PAYER_CF);
        receipt.setEventData(eventData);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "true")
                .execute(() -> {
                    sut = new ReceiptToIO(receiptToIOServiceMock);
                    sut.processReceiptToIO(Collections.singletonList(receipt), documentReceiptsMock, documentMessagesMock, executionContextMock);
                });

        verify(receiptToIOServiceMock).notifyMessage(anyString(), any(), any());
        verify(receiptToIOServiceMock).verifyMessagesNotification(any(), any());
        verify(documentReceiptsMock).setValue(anyList());
        verify(documentMessagesMock, never()).setValue(anyList());
    }

    @Test
    @SneakyThrows
    void receiptToIONotNotifiedBecausePayerEqualDebtor() {
        doReturn(Collections.emptyList()).when(receiptToIOServiceMock).verifyMessagesNotification(any(), any());

        Receipt receipt = new Receipt();
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        eventData.setPayerFiscalCode(VALID_DEBTOR_CF);
        receipt.setEventData(eventData);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "true")
                .execute(() -> {
                    sut = new ReceiptToIO(receiptToIOServiceMock);
                    sut.processReceiptToIO(Collections.singletonList(receipt), documentReceiptsMock, documentMessagesMock, executionContextMock);
                });

        verify(receiptToIOServiceMock, never()).notifyMessage(anyString(), any(), any());
        verify(receiptToIOServiceMock).verifyMessagesNotification(any(), any());
        verify(documentReceiptsMock).setValue(anyList());
        verify(documentMessagesMock, never()).setValue(anyList());
    }
}