package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.models.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.notifier.client.NotifierCartQueueClient;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartPayment;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartStatusType;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.MessageData;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.Payload;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.CartIOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReasonErrorCode;
import it.gov.pagopa.receipt.pdf.notifier.exception.CartIoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.exception.ErrorToNotifyException;
import it.gov.pagopa.receipt.pdf.notifier.exception.IOAPIException;
import it.gov.pagopa.receipt.pdf.notifier.exception.MissingFieldsForNotificationException;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
import it.gov.pagopa.receipt.pdf.notifier.model.NotifyCartResult;
import it.gov.pagopa.receipt.pdf.notifier.model.NotifyUserResult;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.MessageContent;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.MessagePayload;
import it.gov.pagopa.receipt.pdf.notifier.service.IOService;
import it.gov.pagopa.receipt.pdf.notifier.service.NotificationMessageBuilder;
import it.gov.pagopa.receipt.pdf.notifier.service.PDVTokenizerServiceRetryWrapper;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.Collections;
import java.util.List;

import static it.gov.pagopa.receipt.pdf.notifier.utils.ReceiptToIOUtils.ANONIMO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
class CartReceiptToIOServiceImplTest {

    private static final String VALID_DEBTOR_1_MESSAGE_ID = "valid debtor 1 message id";
    private static final String VALID_DEBTOR_2_MESSAGE_ID = "valid debtor 2 message id";
    private static final String VALID_PAYER_MESSAGE_ID = "valid payer message id";
    private static final String PAYER_CF_TOKEN = "iounsnflkejhòasd";
    private static final String VALID_PAYER_CF = "JHNDOE80D45E507N";
    private static final String INVALID_CF = "an invalid fiscal code";
    private static final String DEBTOR_1_CF_TOKEN = "alskfjòaoiuernòlqnwer";
    private static final String DEBTOR_2_CF_TOKEN = "poiasdfnkjenlòansdfjd";
    private static final String VALID_DEBTOR_1_CF = "JHNDOE80D05B157Y";
    static final String VALID_DEBTOR_2_CF = "JHNDOE66D05B157K";
    private static final String EVENT_1_ID = "123";
    private static final String EVENT_2_ID = "456";
    private static final String ERROR_MESSAGE = "error message";
    public static final String CART_ID = "cartId";
    public static final String SUBJECT_PAYER = "subjectPayer";
    public static final String MARKDOWN_PAYER = "markdownPayer";
    public static final String SUBJECT_DEBTOR_1 = "subjectDebtor1";
    public static final String MARKDOWN_DEBTOR_1 = "markdownDebtor1";
    public static final String SUBJECT_DEBTOR_2 = "subjectDebtor2";
    public static final String MARKDOWN_DEBTOR_2 = "markdownDebtor2";

    @SystemStub
    private EnvironmentVariables environmentVariables =
            new EnvironmentVariables(
                    "CF_FILTER_NOTIFIER",
                    VALID_DEBTOR_1_CF + "," + VALID_DEBTOR_2_CF + "," + VALID_PAYER_CF
            );

    @Mock
    private IOService ioServiceMock;
    @Mock
    private NotifierCartQueueClient notifierCartQueueClientMock;
    @Mock
    private NotificationMessageBuilder notificationMessageBuilderMock;
    @Mock
    private PDVTokenizerServiceRetryWrapper pdvTokenizerServiceRetryWrapperMock;
    @Mock
    private CartReceiptCosmosClient cartReceiptCosmosClientMock;

    private CartReceiptToIOServiceImpl sut;

    @Test
    @SneakyThrows
    void notifyCartSuccessPayerNull() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .cart(List.of(
                                        buildCartPayment(EVENT_1_ID, DEBTOR_1_CF_TOKEN),
                                        buildCartPayment(EVENT_2_ID, DEBTOR_2_CF_TOKEN)))
                                .build()
                )
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_1_CF_TOKEN)).thenReturn(VALID_DEBTOR_1_CF);
        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_2_CF_TOKEN)).thenReturn(VALID_DEBTOR_2_CF);
        when(cartReceiptCosmosClientMock.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, EVENT_1_ID, UserType.DEBTOR))
                .thenThrow(CartIoMessageNotFoundException.class);
        when(cartReceiptCosmosClientMock.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, EVENT_2_ID, UserType.DEBTOR))
                .thenThrow(CartIoMessageNotFoundException.class);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_DEBTOR_1_CF)).thenReturn(true);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_DEBTOR_2_CF)).thenReturn(true);
        when(notificationMessageBuilderMock.buildCartDebtorMessagePayload(anyString(), any(), anyString()))
                .thenReturn(
                        buildMessagePayload(MARKDOWN_DEBTOR_1, SUBJECT_DEBTOR_1),
                        buildMessagePayload(MARKDOWN_DEBTOR_2, SUBJECT_DEBTOR_2)
                );
        when(ioServiceMock.sendNotificationToIOUser(any())).thenReturn(VALID_DEBTOR_1_MESSAGE_ID, VALID_DEBTOR_2_MESSAGE_ID);

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNull(result.getPayerNotifyResult());
        assertNotNull(result.getDebtorNotifyResultMap());
        assertEquals(2, result.getDebtorNotifyResultMap().size());
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID));
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_2_ID));
        assertEquals(UserNotifyStatus.NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getNotifyStatus());
        assertEquals(VALID_DEBTOR_1_MESSAGE_ID, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage().getId());
        assertEquals(SUBJECT_DEBTOR_1, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage().getSubject());
        assertEquals(MARKDOWN_DEBTOR_1, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage().getMarkdown());
        assertEquals(UserNotifyStatus.NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_2_ID).getNotifyStatus());
        assertEquals(VALID_DEBTOR_2_MESSAGE_ID, result.getDebtorNotifyResultMap().get(EVENT_2_ID).getMessage().getId());
        assertEquals(SUBJECT_DEBTOR_2, result.getDebtorNotifyResultMap().get(EVENT_2_ID).getMessage().getSubject());
        assertEquals(MARKDOWN_DEBTOR_2, result.getDebtorNotifyResultMap().get(EVENT_2_ID).getMessage().getMarkdown());

        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());
        cart.getPayload().getCart().forEach(cartPayment -> {
            if (cartPayment.getBizEventId().equals(EVENT_1_ID)) {
                assertEquals(VALID_DEBTOR_1_MESSAGE_ID, cartPayment.getMessageDebtor().getId());
                assertEquals(SUBJECT_DEBTOR_1, cartPayment.getMessageDebtor().getSubject());
                assertEquals(MARKDOWN_DEBTOR_1, cartPayment.getMessageDebtor().getMarkdown());
            } else if (cartPayment.getBizEventId().equals(EVENT_2_ID)) {
                assertEquals(VALID_DEBTOR_2_MESSAGE_ID, cartPayment.getMessageDebtor().getId());
                assertEquals(SUBJECT_DEBTOR_2, cartPayment.getMessageDebtor().getSubject());
                assertEquals(MARKDOWN_DEBTOR_2, cartPayment.getMessageDebtor().getMarkdown());
            }
            assertNull(cartPayment.getReasonErrDebtor());
        });
    }

    @Test
    @SneakyThrows
    void notifyCartSuccessPayerNullAndDebtorAnonimo() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .cart(List.of(buildCartPayment(EVENT_1_ID, ANONIMO)))
                                .build()
                )
                .build();

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNull(result.getPayerNotifyResult());
        assertNull(result.getDebtorNotifyResultMap());

        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());
        cart.getPayload().getCart().forEach(cartPayment -> {
            assertNull(cartPayment.getMessageDebtor());
            assertNull(cartPayment.getReasonErrDebtor());
        });

        verify(pdvTokenizerServiceRetryWrapperMock, never()).getFiscalCodeWithRetry(anyString());
        verify(cartReceiptCosmosClientMock, never())
                .findIOMessageWithCartIdAndEventIdAndUserType(anyString(), anyString(), any());
        verify(ioServiceMock, never()).isNotifyToIOUserAllowed(anyString());
        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    @SneakyThrows
    void notifyCartSuccessSameDebtorPayer() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(
                                        buildCartPayment(EVENT_1_ID, PAYER_CF_TOKEN),
                                        buildCartPayment(EVENT_2_ID, PAYER_CF_TOKEN)))
                                .build()
                )
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(PAYER_CF_TOKEN)).thenReturn(VALID_PAYER_CF);
        when(cartReceiptCosmosClientMock.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, null, UserType.PAYER))
                .thenThrow(CartIoMessageNotFoundException.class);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_PAYER_CF)).thenReturn(true);
        when(notificationMessageBuilderMock.buildCartPayerMessagePayload(anyString(), any()))
                .thenReturn(buildMessagePayload(MARKDOWN_PAYER, SUBJECT_PAYER));
        when(ioServiceMock.sendNotificationToIOUser(any())).thenReturn(VALID_PAYER_MESSAGE_ID);

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNotNull(result.getPayerNotifyResult());
        assertEquals(UserNotifyStatus.NOTIFIED, result.getPayerNotifyResult().getNotifyStatus());
        assertEquals(VALID_PAYER_MESSAGE_ID, result.getPayerNotifyResult().getMessage().getId());
        assertEquals(MARKDOWN_PAYER, result.getPayerNotifyResult().getMessage().getMarkdown());
        assertEquals(SUBJECT_PAYER, result.getPayerNotifyResult().getMessage().getSubject());
        assertNull(result.getDebtorNotifyResultMap());

        assertEquals(VALID_PAYER_MESSAGE_ID, cart.getPayload().getMessagePayer().getId());
        assertEquals(SUBJECT_PAYER, cart.getPayload().getMessagePayer().getSubject());
        assertEquals(MARKDOWN_PAYER, cart.getPayload().getMessagePayer().getMarkdown());
        assertNull(cart.getPayload().getReasonErrPayer());
        cart.getPayload().getCart().forEach(cartPayment -> {
            assertNull(cartPayment.getMessageDebtor());
            assertNull(cartPayment.getReasonErrDebtor());
        });
    }

    @Test
    @SneakyThrows
    void notifyCartSkippedSameDebtorPayerAndPayerDisabled() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(
                                        buildCartPayment(EVENT_1_ID, PAYER_CF_TOKEN),
                                        buildCartPayment(EVENT_2_ID, PAYER_CF_TOKEN)))
                                .build()
                )
                .build();

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "true")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNull(result.getPayerNotifyResult());
        assertNull(result.getDebtorNotifyResultMap());

        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());
        cart.getPayload().getCart().forEach(cartPayment -> {
            assertNull(cartPayment.getMessageDebtor());
            assertNull(cartPayment.getReasonErrDebtor());
        });

        verify(pdvTokenizerServiceRetryWrapperMock, never()).getFiscalCodeWithRetry(anyString());
        verify(cartReceiptCosmosClientMock, never())
                .findIOMessageWithCartIdAndEventIdAndUserType(anyString(), anyString(), any());
        verify(ioServiceMock, never()).isNotifyToIOUserAllowed(anyString());
        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    @SneakyThrows
    void notifyCartSuccessDifferentDebtorPayer() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(
                                        buildCartPayment(EVENT_1_ID, DEBTOR_1_CF_TOKEN),
                                        buildCartPayment(EVENT_2_ID, DEBTOR_2_CF_TOKEN)))
                                .build()
                )
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(PAYER_CF_TOKEN)).thenReturn(VALID_PAYER_CF);
        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_1_CF_TOKEN)).thenReturn(VALID_DEBTOR_1_CF);
        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_2_CF_TOKEN)).thenReturn(VALID_DEBTOR_2_CF);
        when(cartReceiptCosmosClientMock.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, null, UserType.PAYER))
                .thenThrow(CartIoMessageNotFoundException.class);
        when(cartReceiptCosmosClientMock.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, EVENT_1_ID, UserType.DEBTOR))
                .thenThrow(CartIoMessageNotFoundException.class);
        when(cartReceiptCosmosClientMock.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, EVENT_2_ID, UserType.DEBTOR))
                .thenThrow(CartIoMessageNotFoundException.class);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_PAYER_CF)).thenReturn(true);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_DEBTOR_1_CF)).thenReturn(true);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_DEBTOR_2_CF)).thenReturn(true);
        when(notificationMessageBuilderMock.buildCartPayerMessagePayload(anyString(), any()))
                .thenReturn(buildMessagePayload(MARKDOWN_PAYER, SUBJECT_PAYER));
        when(notificationMessageBuilderMock.buildCartDebtorMessagePayload(anyString(), any(), anyString()))
                .thenReturn(
                        buildMessagePayload(MARKDOWN_DEBTOR_1, SUBJECT_DEBTOR_1),
                        buildMessagePayload(MARKDOWN_DEBTOR_2, SUBJECT_DEBTOR_2)
                );
        when(ioServiceMock.sendNotificationToIOUser(any()))
                .thenReturn(VALID_PAYER_MESSAGE_ID, VALID_DEBTOR_1_MESSAGE_ID, VALID_DEBTOR_2_MESSAGE_ID);

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNotNull(result.getPayerNotifyResult());
        assertEquals(UserNotifyStatus.NOTIFIED, result.getPayerNotifyResult().getNotifyStatus());
        assertEquals(VALID_PAYER_MESSAGE_ID, result.getPayerNotifyResult().getMessage().getId());
        assertEquals(MARKDOWN_PAYER, result.getPayerNotifyResult().getMessage().getMarkdown());
        assertEquals(SUBJECT_PAYER, result.getPayerNotifyResult().getMessage().getSubject());
        assertNotNull(result.getDebtorNotifyResultMap());
        assertEquals(2, result.getDebtorNotifyResultMap().size());
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID));
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_2_ID));
        assertEquals(UserNotifyStatus.NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getNotifyStatus());
        assertEquals(VALID_DEBTOR_1_MESSAGE_ID, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage().getId());
        assertEquals(SUBJECT_DEBTOR_1, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage().getSubject());
        assertEquals(MARKDOWN_DEBTOR_1, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage().getMarkdown());
        assertEquals(UserNotifyStatus.NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_2_ID).getNotifyStatus());
        assertEquals(VALID_DEBTOR_2_MESSAGE_ID, result.getDebtorNotifyResultMap().get(EVENT_2_ID).getMessage().getId());
        assertEquals(SUBJECT_DEBTOR_2, result.getDebtorNotifyResultMap().get(EVENT_2_ID).getMessage().getSubject());
        assertEquals(MARKDOWN_DEBTOR_2, result.getDebtorNotifyResultMap().get(EVENT_2_ID).getMessage().getMarkdown());

        assertEquals(VALID_PAYER_MESSAGE_ID, cart.getPayload().getMessagePayer().getId());
        assertEquals(MARKDOWN_PAYER, cart.getPayload().getMessagePayer().getMarkdown());
        assertEquals(SUBJECT_PAYER, cart.getPayload().getMessagePayer().getSubject());
        assertNull(cart.getPayload().getReasonErrPayer());
        cart.getPayload().getCart().forEach(cartPayment -> {
            if (cartPayment.getBizEventId().equals(EVENT_1_ID)) {
                assertEquals(VALID_DEBTOR_1_MESSAGE_ID, cartPayment.getMessageDebtor().getId());
                assertEquals(SUBJECT_DEBTOR_1, cartPayment.getMessageDebtor().getSubject());
                assertEquals(MARKDOWN_DEBTOR_1, cartPayment.getMessageDebtor().getMarkdown());
            } else if (cartPayment.getBizEventId().equals(EVENT_2_ID)) {
                assertEquals(VALID_DEBTOR_2_MESSAGE_ID, cartPayment.getMessageDebtor().getId());
                assertEquals(SUBJECT_DEBTOR_2, cartPayment.getMessageDebtor().getSubject());
                assertEquals(MARKDOWN_DEBTOR_2, cartPayment.getMessageDebtor().getMarkdown());
            }
            assertNull(cartPayment.getReasonErrDebtor());
        });
    }

    @Test
    @SneakyThrows
    void notifyCartSkippedPayerNullAndDebtorCFNotValid() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .cart(List.of(buildCartPayment(EVENT_1_ID, DEBTOR_1_CF_TOKEN)))
                                .build()
                )
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_1_CF_TOKEN)).thenReturn(INVALID_CF);

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNull(result.getPayerNotifyResult());
        assertNotNull(result.getDebtorNotifyResultMap());
        assertEquals(1, result.getDebtorNotifyResultMap().size());
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID));
        assertEquals(UserNotifyStatus.NOT_TO_BE_NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getNotifyStatus());
        assertNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage());

        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());
        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNull(cartPayment.getReasonErrDebtor());
        assertNull(cartPayment.getMessageDebtor());

        verify(cartReceiptCosmosClientMock, never())
                .findIOMessageWithCartIdAndEventIdAndUserType(anyString(), anyString(), any());
        verify(ioServiceMock, never()).isNotifyToIOUserAllowed(anyString());
        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    @SneakyThrows
    void notifyCartSkippedPayerNullAndDebtorIdMessageAlreadyPresent() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .cart(List.of(
                                        CartPayment.builder()
                                                .bizEventId(EVENT_1_ID)
                                                .debtorFiscalCode(DEBTOR_1_CF_TOKEN)
                                                .messageDebtor(MessageData.builder().id(VALID_DEBTOR_1_MESSAGE_ID).build())
                                                .build()))
                                .build()
                )
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_1_CF_TOKEN)).thenReturn(VALID_DEBTOR_1_CF);

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNull(result.getPayerNotifyResult());
        assertNotNull(result.getDebtorNotifyResultMap());
        assertEquals(1, result.getDebtorNotifyResultMap().size());
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID));
        assertEquals(UserNotifyStatus.NOT_TO_BE_NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getNotifyStatus());
        assertNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage());

        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());
        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNull(cartPayment.getReasonErrDebtor());
        assertEquals(VALID_DEBTOR_1_MESSAGE_ID, cartPayment.getMessageDebtor().getId());

        verify(cartReceiptCosmosClientMock, never())
                .findIOMessageWithCartIdAndEventIdAndUserType(anyString(), anyString(), any());
        verify(ioServiceMock, never()).isNotifyToIOUserAllowed(anyString());
        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    @SneakyThrows
    void notifyCartPayerNullAndDebtorFailCallToTokenizerThrowsPDVTokenizerException() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .cart(List.of(buildCartPayment(EVENT_1_ID, DEBTOR_1_CF_TOKEN)))
                                .build())
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_1_CF_TOKEN))
                .thenThrow(new PDVTokenizerException(ERROR_MESSAGE, HttpStatus.SC_BAD_REQUEST));

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNull(result.getPayerNotifyResult());
        assertNotNull(result.getDebtorNotifyResultMap());
        assertEquals(1, result.getDebtorNotifyResultMap().size());
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID));
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getNotifyStatus());
        assertNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage());
        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());

        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNotNull(cartPayment.getReasonErrDebtor());
        assertEquals(HttpStatus.SC_BAD_REQUEST, cartPayment.getReasonErrDebtor().getCode());
        assertEquals(ERROR_MESSAGE, cartPayment.getReasonErrDebtor().getMessage());
        assertNull(cartPayment.getMessageDebtor());

        verify(cartReceiptCosmosClientMock, never())
                .findIOMessageWithCartIdAndEventIdAndUserType(anyString(), anyString(), any());
        verify(ioServiceMock, never()).isNotifyToIOUserAllowed(anyString());
        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    @SneakyThrows
    void notifyCartPayerNullAndDebtorFailCallToTokenizerThrowsJsonProcessingException() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .cart(List.of(buildCartPayment(EVENT_1_ID, DEBTOR_1_CF_TOKEN)))
                                .build())
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_1_CF_TOKEN))
                .thenThrow(JsonProcessingException.class);

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNull(result.getPayerNotifyResult());
        assertNotNull(result.getDebtorNotifyResultMap());
        assertEquals(1, result.getDebtorNotifyResultMap().size());
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID));
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getNotifyStatus());
        assertNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage());
        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());

        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNotNull(cartPayment.getReasonErrDebtor());
        assertEquals(ReasonErrorCode.ERROR_PDV_MAPPING.getCode(), cartPayment.getReasonErrDebtor().getCode());
        assertNotNull(cartPayment.getReasonErrDebtor().getMessage());
        assertNull(cartPayment.getMessageDebtor());

        verify(cartReceiptCosmosClientMock, never())
                .findIOMessageWithCartIdAndEventIdAndUserType(anyString(), anyString(), any());
        verify(ioServiceMock, never()).isNotifyToIOUserAllowed(anyString());
        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    @SneakyThrows
    void notifyCartPayerNullAndDebtorFailGetProfileThrowsIOAPIException() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .cart(List.of(buildCartPayment(EVENT_1_ID, DEBTOR_1_CF_TOKEN)))
                                .build())
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_1_CF_TOKEN))
                .thenReturn(VALID_DEBTOR_1_CF);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_DEBTOR_1_CF))
                .thenThrow(new IOAPIException(ERROR_MESSAGE, ReasonErrorCode.ERROR_IO_API_IO.getCode()));

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNull(result.getPayerNotifyResult());
        assertNotNull(result.getDebtorNotifyResultMap());
        assertEquals(1, result.getDebtorNotifyResultMap().size());
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID));
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getNotifyStatus());
        assertNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage());
        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());

        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNotNull(cartPayment.getReasonErrDebtor());
        assertEquals(ReasonErrorCode.ERROR_IO_API_IO.getCode(), cartPayment.getReasonErrDebtor().getCode());
        assertEquals(ERROR_MESSAGE, cartPayment.getReasonErrDebtor().getMessage());
        assertNull(cartPayment.getMessageDebtor());

        verify(cartReceiptCosmosClientMock, never())
                .findIOMessageWithCartIdAndEventIdAndUserType(anyString(), anyString(), any());
        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    @SneakyThrows
    void notifyCartPayerNullAndDebtorFailGetProfileThrowsErrorToNotifyException() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .cart(List.of(buildCartPayment(EVENT_1_ID, DEBTOR_1_CF_TOKEN)))
                                .build())
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_1_CF_TOKEN))
                .thenReturn(VALID_DEBTOR_1_CF);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_DEBTOR_1_CF))
                .thenThrow(new ErrorToNotifyException(ERROR_MESSAGE));

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNull(result.getPayerNotifyResult());
        assertNotNull(result.getDebtorNotifyResultMap());
        assertEquals(1, result.getDebtorNotifyResultMap().size());
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID));
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getNotifyStatus());
        assertNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage());
        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());

        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNotNull(cartPayment.getReasonErrDebtor());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, cartPayment.getReasonErrDebtor().getCode());
        assertEquals(ERROR_MESSAGE, cartPayment.getReasonErrDebtor().getMessage());
        assertNull(cartPayment.getMessageDebtor());

        verify(cartReceiptCosmosClientMock, never())
                .findIOMessageWithCartIdAndEventIdAndUserType(anyString(), anyString(), any());
        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    @SneakyThrows
    void notifyCartPayerNullAndDebtorSkippedNotifyNotAllowed() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .cart(List.of(buildCartPayment(EVENT_1_ID, DEBTOR_1_CF_TOKEN)))
                                .build())
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_1_CF_TOKEN))
                .thenReturn(VALID_DEBTOR_1_CF);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_DEBTOR_1_CF))
                .thenReturn(false);

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNull(result.getPayerNotifyResult());
        assertNotNull(result.getDebtorNotifyResultMap());
        assertEquals(1, result.getDebtorNotifyResultMap().size());
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID));
        assertEquals(UserNotifyStatus.NOT_TO_BE_NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getNotifyStatus());
        assertNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage());
        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());

        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNull(cartPayment.getReasonErrDebtor());
        assertNull(cartPayment.getMessageDebtor());

        verify(cartReceiptCosmosClientMock, never())
                .findIOMessageWithCartIdAndEventIdAndUserType(anyString(), anyString(), any());
        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    @SneakyThrows
    void notifyCartPayerNullAndDebtorFailBuildNotificationPayload() {
        CartPayment payment = buildCartPayment(EVENT_1_ID, DEBTOR_1_CF_TOKEN);
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .cart(List.of(payment))
                                .build())
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_1_CF_TOKEN))
                .thenReturn(VALID_DEBTOR_1_CF);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_DEBTOR_1_CF))
                .thenReturn(true);
        when(cartReceiptCosmosClientMock.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, EVENT_1_ID, UserType.DEBTOR))
                .thenThrow(CartIoMessageNotFoundException.class);
        when(notificationMessageBuilderMock.buildCartDebtorMessagePayload(VALID_DEBTOR_1_CF, payment, CART_ID))
                .thenThrow(new MissingFieldsForNotificationException(ERROR_MESSAGE));

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNull(result.getPayerNotifyResult());
        assertNotNull(result.getDebtorNotifyResultMap());
        assertEquals(1, result.getDebtorNotifyResultMap().size());
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID));
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getNotifyStatus());
        assertNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage());
        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());

        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNotNull(cartPayment.getReasonErrDebtor());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, cartPayment.getReasonErrDebtor().getCode());
        assertEquals(ERROR_MESSAGE, cartPayment.getReasonErrDebtor().getMessage());
        assertNull(cartPayment.getMessageDebtor());

        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    @SneakyThrows
    void notifyCartPayerNullAndDebtorFailNotifyThrowsIOAPIException() {
        CartPayment payment = buildCartPayment(EVENT_1_ID, DEBTOR_1_CF_TOKEN);
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .cart(List.of(payment))
                                .build())
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_1_CF_TOKEN))
                .thenReturn(VALID_DEBTOR_1_CF);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_DEBTOR_1_CF))
                .thenReturn(true);
        when(cartReceiptCosmosClientMock.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, EVENT_1_ID, UserType.DEBTOR))
                .thenThrow(CartIoMessageNotFoundException.class);
        when(ioServiceMock.sendNotificationToIOUser(any()))
                .thenThrow(new IOAPIException(ERROR_MESSAGE, ReasonErrorCode.ERROR_IO_API_UNEXPECTED.getCode()));

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNull(result.getPayerNotifyResult());
        assertNotNull(result.getDebtorNotifyResultMap());
        assertEquals(1, result.getDebtorNotifyResultMap().size());
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID));
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getNotifyStatus());
        assertNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage());
        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());

        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNotNull(cartPayment.getReasonErrDebtor());
        assertEquals(ReasonErrorCode.ERROR_IO_API_UNEXPECTED.getCode(), cartPayment.getReasonErrDebtor().getCode());
        assertEquals(ERROR_MESSAGE, cartPayment.getReasonErrDebtor().getMessage());
        assertNull(cartPayment.getMessageDebtor());
    }

    @Test
    @SneakyThrows
    void notifyCartPayerNullAndDebtorFailNotifyThrowsErrorToNotifyException() {
        CartPayment payment = buildCartPayment(EVENT_1_ID, DEBTOR_1_CF_TOKEN);
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .cart(List.of(payment))
                                .build())
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_1_CF_TOKEN))
                .thenReturn(VALID_DEBTOR_1_CF);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_DEBTOR_1_CF))
                .thenReturn(true);
        when(cartReceiptCosmosClientMock.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, EVENT_1_ID, UserType.DEBTOR))
                .thenThrow(CartIoMessageNotFoundException.class);
        when(ioServiceMock.sendNotificationToIOUser(any()))
                .thenThrow(new ErrorToNotifyException(ERROR_MESSAGE));

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNull(result.getPayerNotifyResult());
        assertNotNull(result.getDebtorNotifyResultMap());
        assertEquals(1, result.getDebtorNotifyResultMap().size());
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID));
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getNotifyStatus());
        assertNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage());
        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());

        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNotNull(cartPayment.getReasonErrDebtor());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, cartPayment.getReasonErrDebtor().getCode());
        assertEquals(ERROR_MESSAGE, cartPayment.getReasonErrDebtor().getMessage());
        assertNull(cartPayment.getMessageDebtor());
    }

    @Test
    @SneakyThrows
    void notifyCartPayerNullAndDebtorSkippedAlreadyNotified() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .cart(List.of(buildCartPayment(EVENT_1_ID, DEBTOR_1_CF_TOKEN)))
                                .build())
                .build();
        CartIOMessage ioMessage = CartIOMessage.builder().messageId(VALID_DEBTOR_1_MESSAGE_ID).build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(DEBTOR_1_CF_TOKEN))
                .thenReturn(VALID_DEBTOR_1_CF);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_DEBTOR_1_CF))
                .thenReturn(true);
        when(cartReceiptCosmosClientMock.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, EVENT_1_ID, UserType.DEBTOR))
                .thenReturn(ioMessage);

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNull(result.getPayerNotifyResult());
        assertNotNull(result.getDebtorNotifyResultMap());
        assertEquals(1, result.getDebtorNotifyResultMap().size());
        assertNotNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID));
        assertEquals(UserNotifyStatus.ALREADY_NOTIFIED, result.getDebtorNotifyResultMap().get(EVENT_1_ID).getNotifyStatus());
        assertNull(result.getDebtorNotifyResultMap().get(EVENT_1_ID).getMessage());
        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());

        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNull(cartPayment.getReasonErrDebtor());
        assertEquals(VALID_DEBTOR_1_MESSAGE_ID, cartPayment.getMessageDebtor().getId());

        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    @SneakyThrows
    void notifyCartSamePayerDebtorSkippedCFNotValid() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(buildCartPayment(EVENT_1_ID, PAYER_CF_TOKEN)))
                                .build()
                )
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(PAYER_CF_TOKEN)).thenReturn(INVALID_CF);

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNotNull(result.getPayerNotifyResult());
        assertEquals(UserNotifyStatus.NOT_TO_BE_NOTIFIED, result.getPayerNotifyResult().getNotifyStatus());
        assertNull(result.getPayerNotifyResult().getMessage());
        assertNull(result.getDebtorNotifyResultMap());

        assertNull(cart.getPayload().getMessagePayer());
        assertNull(cart.getPayload().getReasonErrPayer());
        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNull(cartPayment.getReasonErrDebtor());
        assertNull(cartPayment.getMessageDebtor());

        verify(cartReceiptCosmosClientMock, never())
                .findIOMessageWithCartIdAndEventIdAndUserType(anyString(), anyString(), any());
        verify(ioServiceMock, never()).isNotifyToIOUserAllowed(anyString());
        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    @SneakyThrows
    void notifyCartSamePayerDebtorFailGetProfileThrowsErrorToNotifyException() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(buildCartPayment(EVENT_1_ID, PAYER_CF_TOKEN)))
                                .build()
                )
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(PAYER_CF_TOKEN)).thenReturn(VALID_PAYER_CF);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_PAYER_CF))
                .thenThrow(new ErrorToNotifyException(ERROR_MESSAGE));

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNotNull(result.getPayerNotifyResult());
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, result.getPayerNotifyResult().getNotifyStatus());
        assertNull(result.getPayerNotifyResult().getMessage());
        assertNull(result.getDebtorNotifyResultMap());

        assertNull(cart.getPayload().getMessagePayer());
        assertNotNull(cart.getPayload().getReasonErrPayer());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, cart.getPayload().getReasonErrPayer().getCode());
        assertEquals(ERROR_MESSAGE, cart.getPayload().getReasonErrPayer().getMessage());
        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNull(cartPayment.getReasonErrDebtor());
        assertNull(cartPayment.getMessageDebtor());

        verify(cartReceiptCosmosClientMock, never())
                .findIOMessageWithCartIdAndEventIdAndUserType(anyString(), anyString(), any());
        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    @SneakyThrows
    void notifyCartSamePayerDebtorSkippedAlreadyNotified() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(buildCartPayment(EVENT_1_ID, PAYER_CF_TOKEN)))
                                .build()
                )
                .build();

        CartIOMessage ioMessage = CartIOMessage.builder().messageId(VALID_PAYER_MESSAGE_ID).build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(PAYER_CF_TOKEN)).thenReturn(VALID_PAYER_CF);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_PAYER_CF))
                .thenReturn(true);
        when(cartReceiptCosmosClientMock.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, null, UserType.PAYER))
                .thenReturn(ioMessage);

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNotNull(result.getPayerNotifyResult());
        assertEquals(UserNotifyStatus.ALREADY_NOTIFIED, result.getPayerNotifyResult().getNotifyStatus());
        assertNull(result.getPayerNotifyResult().getMessage());
        assertNull(result.getDebtorNotifyResultMap());

        assertEquals(VALID_PAYER_MESSAGE_ID, cart.getPayload().getMessagePayer().getId());
        assertNull(cart.getPayload().getReasonErrPayer());
        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNull(cartPayment.getReasonErrDebtor());
        assertNull(cartPayment.getMessageDebtor());

        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    @SneakyThrows
    void notifyCartSamePayerDebtorFailBuildMessagePayload() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(buildCartPayment(EVENT_1_ID, PAYER_CF_TOKEN)))
                                .build()
                )
                .build();

        when(pdvTokenizerServiceRetryWrapperMock.getFiscalCodeWithRetry(PAYER_CF_TOKEN)).thenReturn(VALID_PAYER_CF);
        when(ioServiceMock.isNotifyToIOUserAllowed(VALID_PAYER_CF))
                .thenReturn(true);
        when(cartReceiptCosmosClientMock.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, null, UserType.PAYER))
                .thenThrow(CartIoMessageNotFoundException.class);
        when(notificationMessageBuilderMock.buildCartPayerMessagePayload(VALID_PAYER_CF, cart))
                .thenThrow(new MissingFieldsForNotificationException(ERROR_MESSAGE));

        NotifyCartResult result = withEnvironmentVariables("PAYER_NOTIFY_DISABLED", "false")
                .execute(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.notifyCart(cart);
                });

        assertNotNull(result);
        assertNotNull(result.getPayerNotifyResult());
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, result.getPayerNotifyResult().getNotifyStatus());
        assertNull(result.getPayerNotifyResult().getMessage());
        assertNull(result.getDebtorNotifyResultMap());

        assertNull(cart.getPayload().getMessagePayer());
        assertNotNull(cart.getPayload().getReasonErrPayer());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, cart.getPayload().getReasonErrPayer().getCode());
        assertEquals(ERROR_MESSAGE, cart.getPayload().getReasonErrPayer().getMessage());
        CartPayment cartPayment = cart.getPayload().getCart().get(0);
        assertEquals(EVENT_1_ID, cartPayment.getBizEventId());
        assertNull(cartPayment.getReasonErrDebtor());
        assertNull(cartPayment.getMessageDebtor());

        verify(ioServiceMock, never()).sendNotificationToIOUser(any());
    }

    @Test
    void verifyNotificationPayerNullSuccess() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .cart(List.of(buildCartPayment(EVENT_1_ID, DEBTOR_1_CF_TOKEN)))
                                .build()
                )
                .build();

        NotifyCartResult notifyCartResult = NotifyCartResult.builder()
                .debtorNotifyResultMap(
                        Collections.singletonMap(EVENT_1_ID, buildNotifiedResult(VALID_DEBTOR_1_MESSAGE_ID)))
                .build();

        List<CartIOMessage> result = assertDoesNotThrow(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.verifyNotificationResultAndUpdateCartReceipt(notifyCartResult, cart);
                }
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CART_ID, result.get(0).getCartId());
        assertEquals(VALID_DEBTOR_1_MESSAGE_ID, result.get(0).getMessageId());
        assertEquals(EVENT_1_ID, result.get(0).getEventId());
        assertEquals(UserType.DEBTOR, result.get(0).getUserType());

        assertEquals(CartStatusType.IO_NOTIFIED, cart.getStatus());
        assertTrue(cart.getNotified_at() > 0);
    }

    @Test
    void verifyNotificationSameDebtorPayerSuccess() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(buildCartPayment(EVENT_1_ID, PAYER_CF_TOKEN)))
                                .build()
                )
                .build();

        NotifyCartResult notifyCartResult = NotifyCartResult.builder()
                .payerNotifyResult(buildNotifiedResult(VALID_PAYER_MESSAGE_ID))
                .build();

        List<CartIOMessage> result = assertDoesNotThrow(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.verifyNotificationResultAndUpdateCartReceipt(notifyCartResult, cart);
                }
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CART_ID, result.get(0).getCartId());
        assertEquals(VALID_PAYER_MESSAGE_ID, result.get(0).getMessageId());
        assertNull(result.get(0).getEventId());
        assertEquals(UserType.PAYER, result.get(0).getUserType());

        assertEquals(CartStatusType.IO_NOTIFIED, cart.getStatus());
        assertTrue(cart.getNotified_at() > 0);
    }

    @Test
    void verifyNotificationDifferentDebtorPayerSuccess() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(buildCartPayment(EVENT_1_ID, VALID_DEBTOR_1_CF)))
                                .build()
                )
                .build();

        NotifyCartResult notifyCartResult = NotifyCartResult.builder()
                .payerNotifyResult(buildNotifiedResult(VALID_PAYER_MESSAGE_ID))
                .debtorNotifyResultMap(
                        Collections.singletonMap(EVENT_1_ID, buildNotifiedResult(VALID_DEBTOR_1_MESSAGE_ID)))
                .build();

        List<CartIOMessage> result = assertDoesNotThrow(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.verifyNotificationResultAndUpdateCartReceipt(notifyCartResult, cart);
                }
        );

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(CART_ID, result.get(0).getCartId());
        assertEquals(VALID_PAYER_MESSAGE_ID, result.get(0).getMessageId());
        assertNull(result.get(0).getEventId());
        assertEquals(UserType.PAYER, result.get(0).getUserType());

        assertEquals(CART_ID, result.get(1).getCartId());
        assertEquals(VALID_DEBTOR_1_MESSAGE_ID, result.get(1).getMessageId());
        assertEquals(EVENT_1_ID, result.get(1).getEventId());
        assertEquals(UserType.DEBTOR, result.get(1).getUserType());

        assertEquals(CartStatusType.IO_NOTIFIED, cart.getStatus());
        assertTrue(cart.getNotified_at() > 0);
    }

    @Test
    void verifyNotificationDifferentDebtorPayerPayerNotToBeNotified() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(buildCartPayment(EVENT_1_ID, VALID_DEBTOR_1_CF)))
                                .build()
                )
                .build();

        NotifyCartResult notifyCartResult = NotifyCartResult.builder()
                .payerNotifyResult(buildNotSuccessResult(UserNotifyStatus.NOT_TO_BE_NOTIFIED))
                .debtorNotifyResultMap(
                        Collections.singletonMap(EVENT_1_ID, buildNotifiedResult(VALID_DEBTOR_1_MESSAGE_ID)))
                .build();

        List<CartIOMessage> result = assertDoesNotThrow(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.verifyNotificationResultAndUpdateCartReceipt(notifyCartResult, cart);
                }
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CART_ID, result.get(0).getCartId());
        assertEquals(VALID_DEBTOR_1_MESSAGE_ID, result.get(0).getMessageId());
        assertEquals(EVENT_1_ID, result.get(0).getEventId());
        assertEquals(UserType.DEBTOR, result.get(0).getUserType());

        assertEquals(CartStatusType.IO_NOTIFIED, cart.getStatus());
        assertTrue(cart.getNotified_at() > 0);
    }

    @Test
    void verifyNotificationDifferentDebtorPayerDebtorNotToBeNotified() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(buildCartPayment(EVENT_1_ID, VALID_DEBTOR_1_CF)))
                                .build()
                )
                .build();

        NotifyCartResult notifyCartResult = NotifyCartResult.builder()
                .payerNotifyResult(buildNotifiedResult(VALID_PAYER_MESSAGE_ID))
                .debtorNotifyResultMap(
                        Collections.singletonMap(EVENT_1_ID, buildNotSuccessResult(UserNotifyStatus.NOT_TO_BE_NOTIFIED)))
                .build();

        List<CartIOMessage> result = assertDoesNotThrow(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.verifyNotificationResultAndUpdateCartReceipt(notifyCartResult, cart);
                }
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CART_ID, result.get(0).getCartId());
        assertEquals(VALID_PAYER_MESSAGE_ID, result.get(0).getMessageId());
        assertNull(result.get(0).getEventId());
        assertEquals(UserType.PAYER, result.get(0).getUserType());

        assertEquals(CartStatusType.IO_NOTIFIED, cart.getStatus());
        assertTrue(cart.getNotified_at() > 0);
    }

    @Test
    void verifyNotificationDifferentDebtorPayerAllNotToBeNotified() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(buildCartPayment(EVENT_1_ID, VALID_DEBTOR_1_CF)))
                                .build()
                )
                .build();

        NotifyCartResult notifyCartResult = NotifyCartResult.builder()
                .payerNotifyResult(buildNotSuccessResult(UserNotifyStatus.NOT_TO_BE_NOTIFIED))
                .debtorNotifyResultMap(
                        Collections.singletonMap(EVENT_1_ID, buildNotSuccessResult(UserNotifyStatus.NOT_TO_BE_NOTIFIED)))
                .build();

        List<CartIOMessage> result = assertDoesNotThrow(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.verifyNotificationResultAndUpdateCartReceipt(notifyCartResult, cart);
                }
        );

        assertNotNull(result);
        assertTrue(result.isEmpty());

        assertEquals(CartStatusType.NOT_TO_NOTIFY, cart.getStatus());
        assertEquals(0, cart.getNotified_at());
    }

    @Test
    void verifyNotificationDifferentDebtorPayerPayerNotNotified() {
        Response<SendMessageResult> queueResponse = mockRequeueResponse(com.microsoft.azure.functions.HttpStatus.CREATED.value());
        when(notifierCartQueueClientMock.sendMessageToQueue(anyString())).thenReturn(queueResponse);

        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(buildCartPayment(EVENT_1_ID, VALID_DEBTOR_1_CF)))
                                .build()
                )
                .build();

        NotifyCartResult notifyCartResult = NotifyCartResult.builder()
                .payerNotifyResult(buildNotSuccessResult(UserNotifyStatus.NOT_NOTIFIED))
                .debtorNotifyResultMap(
                        Collections.singletonMap(EVENT_1_ID, buildNotifiedResult(VALID_DEBTOR_1_MESSAGE_ID)))
                .build();

        List<CartIOMessage> result = assertDoesNotThrow(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.verifyNotificationResultAndUpdateCartReceipt(notifyCartResult, cart);
                }
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CART_ID, result.get(0).getCartId());
        assertEquals(VALID_DEBTOR_1_MESSAGE_ID, result.get(0).getMessageId());
        assertEquals(EVENT_1_ID, result.get(0).getEventId());
        assertEquals(UserType.DEBTOR, result.get(0).getUserType());

        assertEquals(CartStatusType.IO_ERROR_TO_NOTIFY, cart.getStatus());
        assertEquals(0, cart.getNotified_at());
    }

    @Test
    void verifyNotificationDifferentDebtorPayerDebtorNotNotified() {
        Response<SendMessageResult> queueResponse = mockRequeueResponse(HttpStatus.SC_CREATED);
        when(notifierCartQueueClientMock.sendMessageToQueue(anyString())).thenReturn(queueResponse);

        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(buildCartPayment(EVENT_1_ID, VALID_DEBTOR_1_CF)))
                                .build()
                )
                .build();

        NotifyCartResult notifyCartResult = NotifyCartResult.builder()
                .payerNotifyResult(buildNotifiedResult(VALID_PAYER_MESSAGE_ID))
                .debtorNotifyResultMap(
                        Collections.singletonMap(EVENT_1_ID, buildNotSuccessResult(UserNotifyStatus.NOT_NOTIFIED)))
                .build();

        List<CartIOMessage> result = assertDoesNotThrow(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.verifyNotificationResultAndUpdateCartReceipt(notifyCartResult, cart);
                }
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CART_ID, result.get(0).getCartId());
        assertEquals(VALID_PAYER_MESSAGE_ID, result.get(0).getMessageId());
        assertNull(result.get(0).getEventId());
        assertEquals(UserType.PAYER, result.get(0).getUserType());

        assertEquals(CartStatusType.IO_ERROR_TO_NOTIFY, cart.getStatus());
        assertEquals(0, cart.getNotified_at());
    }

    @Test
    void verifyNotificationDifferentDebtorPayerDebtorNotNotifiedMaxRetryReached() {
        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(buildCartPayment(EVENT_1_ID, VALID_DEBTOR_1_CF)))
                                .build()
                )
                .notificationNumRetry(6)
                .build();

        NotifyCartResult notifyCartResult = NotifyCartResult.builder()
                .payerNotifyResult(buildNotifiedResult(VALID_PAYER_MESSAGE_ID))
                .debtorNotifyResultMap(
                        Collections.singletonMap(EVENT_1_ID, buildNotSuccessResult(UserNotifyStatus.NOT_NOTIFIED)))
                .build();

        List<CartIOMessage> result = assertDoesNotThrow(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.verifyNotificationResultAndUpdateCartReceipt(notifyCartResult, cart);
                }
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CART_ID, result.get(0).getCartId());
        assertEquals(VALID_PAYER_MESSAGE_ID, result.get(0).getMessageId());
        assertNull(result.get(0).getEventId());
        assertEquals(UserType.PAYER, result.get(0).getUserType());

        assertEquals(CartStatusType.UNABLE_TO_SEND, cart.getStatus());
        assertEquals(0, cart.getNotified_at());

        verify(notifierCartQueueClientMock, never()).sendMessageToQueue(anyString());
    }

    @Test
    void verifyNotificationDifferentDebtorPayerDebtorNotNotifiedRequeueReturn500() {
        Response<SendMessageResult> queueResponse = mockRequeueResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(notifierCartQueueClientMock.sendMessageToQueue(anyString())).thenReturn(queueResponse);

        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(buildCartPayment(EVENT_1_ID, VALID_DEBTOR_1_CF)))
                                .build()
                )
                .numRetry(6)
                .build();

        NotifyCartResult notifyCartResult = NotifyCartResult.builder()
                .payerNotifyResult(buildNotifiedResult(VALID_PAYER_MESSAGE_ID))
                .debtorNotifyResultMap(
                        Collections.singletonMap(EVENT_1_ID, buildNotSuccessResult(UserNotifyStatus.NOT_NOTIFIED)))
                .build();

        List<CartIOMessage> result = assertDoesNotThrow(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.verifyNotificationResultAndUpdateCartReceipt(notifyCartResult, cart);
                }
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CART_ID, result.get(0).getCartId());
        assertEquals(VALID_PAYER_MESSAGE_ID, result.get(0).getMessageId());
        assertNull(result.get(0).getEventId());
        assertEquals(UserType.PAYER, result.get(0).getUserType());

        assertEquals(CartStatusType.UNABLE_TO_SEND, cart.getStatus());
        assertEquals(0, cart.getNotified_at());
    }

    @Test
    void verifyNotificationDifferentDebtorPayerDebtorNotNotifiedRequeueThrowsException() {
        when(notifierCartQueueClientMock.sendMessageToQueue(anyString())).thenThrow(RuntimeException.class);

        CartForReceipt cart = CartForReceipt.builder()
                .eventId(CART_ID)
                .payload(
                        Payload.builder()
                                .payerFiscalCode(PAYER_CF_TOKEN)
                                .cart(List.of(buildCartPayment(EVENT_1_ID, VALID_DEBTOR_1_CF)))
                                .build()
                )
                .numRetry(6)
                .build();

        NotifyCartResult notifyCartResult = NotifyCartResult.builder()
                .payerNotifyResult(buildNotifiedResult(VALID_PAYER_MESSAGE_ID))
                .debtorNotifyResultMap(
                        Collections.singletonMap(EVENT_1_ID, buildNotSuccessResult(UserNotifyStatus.NOT_NOTIFIED)))
                .build();

        List<CartIOMessage> result = assertDoesNotThrow(() -> {
                    sut = new CartReceiptToIOServiceImpl(
                            ioServiceMock,
                            notifierCartQueueClientMock,
                            notificationMessageBuilderMock,
                            pdvTokenizerServiceRetryWrapperMock,
                            cartReceiptCosmosClientMock
                    );
                    return sut.verifyNotificationResultAndUpdateCartReceipt(notifyCartResult, cart);
                }
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CART_ID, result.get(0).getCartId());
        assertEquals(VALID_PAYER_MESSAGE_ID, result.get(0).getMessageId());
        assertNull(result.get(0).getEventId());
        assertEquals(UserType.PAYER, result.get(0).getUserType());

        assertEquals(CartStatusType.UNABLE_TO_SEND, cart.getStatus());
        assertEquals(0, cart.getNotified_at());
    }

    private NotifyUserResult buildNotSuccessResult(UserNotifyStatus userNotifyStatus) {
        return NotifyUserResult.builder()
                .notifyStatus(userNotifyStatus)
                .build();
    }

    private NotifyUserResult buildNotifiedResult(String messageId) {
        return NotifyUserResult.builder()
                .notifyStatus(UserNotifyStatus.NOTIFIED)
                .message(MessageData.builder().id(messageId).build())
                .build();
    }

    private CartPayment buildCartPayment(String eventId, String cfToken) {
        return CartPayment.builder()
                .bizEventId(eventId)
                .debtorFiscalCode(cfToken)
                .build();
    }

    @NotNull
    private static Response<SendMessageResult> mockRequeueResponse(int status) {
        @SuppressWarnings("unchecked")
        Response<SendMessageResult> queueResponse = mock(Response.class);
        when(queueResponse.getStatusCode()).thenReturn(status);
        return queueResponse;
    }

    private MessagePayload buildMessagePayload(String markdown, String subject) {
        return MessagePayload.builder()
                .content(MessageContent.builder().markdown(markdown).subject(subject).build())
                .build();
    }
}