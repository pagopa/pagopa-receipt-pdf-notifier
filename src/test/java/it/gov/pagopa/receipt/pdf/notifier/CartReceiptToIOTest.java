package it.gov.pagopa.receipt.pdf.notifier;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartPayment;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartStatusType;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.Payload;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.CartIOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.model.NotifyCartResult;
import it.gov.pagopa.receipt.pdf.notifier.service.CartReceiptToIOService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CartReceiptToIOTest {

    @Mock
    private CartReceiptToIOService cartReceiptToIOServiceMock;
    @Mock
    private OutputBinding<List<CartForReceipt>> documentCartReceiptsMock;
    @Mock
    private OutputBinding<List<CartIOMessage>> documentMessagesMock;
    @Mock
    private ExecutionContext executionContextMock;

    @InjectMocks
    private CartReceiptToIO sut;

    @ParameterizedTest
    @EnumSource(value = CartStatusType.class, names = {"GENERATED", "SIGNED", "IO_NOTIFIER_RETRY"})
    void processCartReceiptToIOSuccess(CartStatusType status) {
        doReturn(new NotifyCartResult()).when(cartReceiptToIOServiceMock).notifyCart(any());
        doReturn(Collections.singletonList(new IOMessage()))
                .when(cartReceiptToIOServiceMock).verifyNotificationResultAndUpdateCartReceipt(any(), any());

        CartForReceipt cart = CartForReceipt.builder()
                .payload(
                        Payload.builder()
                                .cart(Collections.singletonList(new CartPayment()))
                                .build()
                )
                .status(status)
                .build();

        assertDoesNotThrow(() -> sut.processCartReceiptToIO(
                Collections.singletonList(cart),
                documentCartReceiptsMock,
                documentMessagesMock,
                executionContextMock
        ));


        verify(cartReceiptToIOServiceMock).notifyCart(any());
        verify(cartReceiptToIOServiceMock).verifyNotificationResultAndUpdateCartReceipt(any(), any());
        verify(documentCartReceiptsMock).setValue(anyList());
        verify(documentMessagesMock).setValue(anyList());
    }

    @ParameterizedTest
    @EnumSource(value = CartStatusType.class, names = {"GENERATED", "SIGNED", "IO_NOTIFIER_RETRY"}, mode = EnumSource.Mode.EXCLUDE)
    void processCartReceiptToIOSuccessDiscardedStatusNotValid(CartStatusType status) {
        CartForReceipt cart = CartForReceipt.builder()
                .payload(
                        Payload.builder()
                                .cart(Collections.singletonList(new CartPayment()))
                                .build()
                )
                .status(status)
                .build();

        assertDoesNotThrow(() -> sut.processCartReceiptToIO(
                Collections.singletonList(cart),
                documentCartReceiptsMock,
                documentMessagesMock,
                executionContextMock
        ));


        verify(cartReceiptToIOServiceMock, never()).notifyCart(any());
        verify(cartReceiptToIOServiceMock, never()).verifyNotificationResultAndUpdateCartReceipt(any(), any());
        verify(documentCartReceiptsMock, never()).setValue(anyList());
        verify(documentMessagesMock, never()).setValue(anyList());
    }

    @Test
    void processCartReceiptToIOSuccessDiscardedEmptyList() {
        assertDoesNotThrow(() -> sut.processCartReceiptToIO(
                Collections.emptyList(),
                documentCartReceiptsMock,
                documentMessagesMock,
                executionContextMock
        ));


        verify(cartReceiptToIOServiceMock, never()).notifyCart(any());
        verify(cartReceiptToIOServiceMock, never()).verifyNotificationResultAndUpdateCartReceipt(any(), any());
        verify(documentCartReceiptsMock, never()).setValue(anyList());
        verify(documentMessagesMock, never()).setValue(anyList());
    }

    @Test
    void processCartReceiptToIOSuccessDiscardedPayloadNull() {
        CartForReceipt cart = CartForReceipt.builder()
                .status(CartStatusType.GENERATED)
                .build();

        assertDoesNotThrow(() -> sut.processCartReceiptToIO(
                Collections.singletonList(cart),
                documentCartReceiptsMock,
                documentMessagesMock,
                executionContextMock
        ));


        verify(cartReceiptToIOServiceMock, never()).notifyCart(any());
        verify(cartReceiptToIOServiceMock, never()).verifyNotificationResultAndUpdateCartReceipt(any(), any());
        verify(documentCartReceiptsMock, never()).setValue(anyList());
        verify(documentMessagesMock, never()).setValue(anyList());
    }

    @Test
    void processCartReceiptToIOSuccessDiscardedCartNull() {
        CartForReceipt cart = CartForReceipt.builder()
                .payload(new Payload())
                .status(CartStatusType.GENERATED)
                .build();

        assertDoesNotThrow(() -> sut.processCartReceiptToIO(
                Collections.singletonList(cart),
                documentCartReceiptsMock,
                documentMessagesMock,
                executionContextMock
        ));


        verify(cartReceiptToIOServiceMock, never()).notifyCart(any());
        verify(cartReceiptToIOServiceMock, never()).verifyNotificationResultAndUpdateCartReceipt(any(), any());
        verify(documentCartReceiptsMock, never()).setValue(anyList());
        verify(documentMessagesMock, never()).setValue(anyList());
    }
}