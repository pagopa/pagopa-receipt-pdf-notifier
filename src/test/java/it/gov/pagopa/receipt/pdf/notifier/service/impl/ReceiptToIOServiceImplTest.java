package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.models.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.receipt.pdf.notifier.client.NotifierQueueClient;
import it.gov.pagopa.receipt.pdf.notifier.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.EventData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.IOMessageData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReasonErrorCode;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.exception.ErrorToNotifyException;
import it.gov.pagopa.receipt.pdf.notifier.exception.IOAPIException;
import it.gov.pagopa.receipt.pdf.notifier.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.exception.MissingFieldsForNotificationException;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.model.io.IOProfileResponse;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.IOMessageResponse;
import it.gov.pagopa.receipt.pdf.notifier.service.IOService;
import it.gov.pagopa.receipt.pdf.notifier.service.NotificationMessageBuilder;
import it.gov.pagopa.receipt.pdf.notifier.service.PDVTokenizerServiceRetryWrapper;
import it.gov.pagopa.receipt.pdf.notifier.service.ReceiptToIOService;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
class ReceiptToIOServiceImplTest {

    private static final String VALID_DEBTOR_MESSAGE_ID = "valid debtor message id";
    private static final String VALID_PAYER_MESSAGE_ID = "valid payer message id";
    private static final String VALID_PAYER_CF = "JHNDOE80D45E507N";
    private static final String INVALID_CF = "an invalid fiscal code";
    private static final String VALID_DEBTOR_CF = "JHNDOE80D05B157Y";
    private static final String EVENT_ID = "123";
    private static final String ERROR_MESSAGE = "error message";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SystemStub
    private EnvironmentVariables environmentVariables = new EnvironmentVariables("CF_FILTER_NOTIFIER", VALID_DEBTOR_CF + "," + VALID_PAYER_CF);

    @Mock
    private IOService ioServiceMock;
    @Mock
    private NotifierQueueClient notifierQueueClientMock;
    @Mock
    private NotificationMessageBuilder notificationMessageBuilderMock;
    @Mock
    private PDVTokenizerServiceRetryWrapper pdvTokenizerServiceRetryWrapperMock;
    @Mock
    private ReceiptCosmosClient receiptCosmosClientMock;
    @InjectMocks
    private ReceiptToIOServiceImpl sut;

    @Test
    @SneakyThrows
    void notifyDebtorWithSuccess() {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());
        doThrow(IoMessageNotFoundException.class).when(receiptCosmosClientMock).findIOMessageWithEventIdAndUserType(anyString(),eq(UserType.DEBTOR));
        doReturn(true).when(ioServiceMock).isNotifyToIOUserAllowed(any());
        doReturn(VALID_DEBTOR_MESSAGE_ID).when(ioServiceMock).sendNotificationToIOUser(any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOTIFIED, userNotifyStatus);
        assertNotNull(receipt.getIoMessageData());
        assertNotNull(receipt.getIoMessageData().getIdMessageDebtor());
        assertEquals(VALID_DEBTOR_MESSAGE_ID, receipt.getIoMessageData().getIdMessageDebtor());
        assertNull(receipt.getReasonErr());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void notifyPayerWithSuccess() {
        doReturn(VALID_PAYER_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());
        doThrow(IoMessageNotFoundException.class).when(receiptCosmosClientMock).findIOMessageWithEventIdAndUserType(anyString(),eq(UserType.PAYER));
        doReturn(true).when(ioServiceMock).isNotifyToIOUserAllowed(any());
        doReturn(VALID_PAYER_MESSAGE_ID).when(ioServiceMock).sendNotificationToIOUser(any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_PAYER_CF, UserType.PAYER, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOTIFIED, userNotifyStatus);
        assertNotNull(receipt.getIoMessageData());
        assertNotNull(receipt.getIoMessageData().getIdMessagePayer());
        assertEquals(VALID_PAYER_MESSAGE_ID, receipt.getIoMessageData().getIdMessagePayer());
        assertNull(receipt.getReasonErr());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void notifyDebtorWithSuccessWithMessageData() {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());
        doThrow(IoMessageNotFoundException.class).when(receiptCosmosClientMock).findIOMessageWithEventIdAndUserType(anyString(),eq(UserType.DEBTOR));
        doReturn(true).when(ioServiceMock).isNotifyToIOUserAllowed(any());
        doReturn(VALID_DEBTOR_MESSAGE_ID).when(ioServiceMock).sendNotificationToIOUser(any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);
        receipt.setIoMessageData(new IOMessageData());

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOTIFIED, userNotifyStatus);
        assertNotNull(receipt.getIoMessageData());
        assertNotNull(receipt.getIoMessageData().getIdMessageDebtor());
        assertEquals(VALID_DEBTOR_MESSAGE_ID, receipt.getIoMessageData().getIdMessageDebtor());
        assertNull(receipt.getReasonErr());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void notifyPayerWithSuccessWithMessageData() {
        doReturn(VALID_PAYER_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());
        doThrow(IoMessageNotFoundException.class).when(receiptCosmosClientMock).findIOMessageWithEventIdAndUserType(anyString(),eq(UserType.PAYER));
        doReturn(true).when(ioServiceMock).isNotifyToIOUserAllowed(any());
        doReturn(VALID_PAYER_MESSAGE_ID).when(ioServiceMock).sendNotificationToIOUser(any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);
        receipt.setIoMessageData(new IOMessageData());

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_PAYER_CF, UserType.PAYER, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOTIFIED, userNotifyStatus);
        assertNotNull(receipt.getIoMessageData());
        assertNotNull(receipt.getIoMessageData().getIdMessagePayer());
        assertEquals(VALID_PAYER_MESSAGE_ID, receipt.getIoMessageData().getIdMessagePayer());
        assertNull(receipt.getReasonErr());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    void notifyFailCallToTokenizerThrowsPDVTokenizerException() throws PDVTokenizerException, JsonProcessingException {
        doThrow(new PDVTokenizerException("", HttpStatus.SC_BAD_REQUEST)).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNotNull(receipt.getReasonErr());
        assertEquals(HttpStatus.SC_BAD_REQUEST, receipt.getReasonErr().getCode());
        assertNotNull(receipt.getReasonErr().getMessage());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    void notifyFailCallToTokenizerThrowsJsonProcessingException() throws PDVTokenizerException, JsonProcessingException {
        doThrow(JsonProcessingException.class).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNotNull(receipt.getReasonErr());
        assertEquals(ReasonErrorCode.ERROR_PDV_MAPPING.getCode(), receipt.getReasonErr().getCode());
        assertNotNull(receipt.getReasonErr().getMessage());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void notifySkipBecauseAlreadyNotified() {
        IOMessage ioMessage = IOMessage.builder().messageId(VALID_DEBTOR_MESSAGE_ID).build();
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());
        doReturn(ioMessage).when(receiptCosmosClientMock).findIOMessageWithEventIdAndUserType(anyString(),eq(UserType.DEBTOR));

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.ALREADY_NOTIFIED, userNotifyStatus);
        assertNotNull(receipt.getIoMessageData().getIdMessageDebtor());
        assertEquals(VALID_DEBTOR_MESSAGE_ID, receipt.getIoMessageData().getIdMessageDebtor());
        assertNull(receipt.getReasonErr());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    void notifyFailNotToBeNotifiedFilterBlock() throws PDVTokenizerException, JsonProcessingException {
        doReturn(INVALID_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(INVALID_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_TO_BE_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNull(receipt.getReasonErr());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void notifyFailDebtorNotNotifiedGetProfileThrowsIOAPIException() {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());
        doThrow(IoMessageNotFoundException.class).when(receiptCosmosClientMock)
                .findIOMessageWithEventIdAndUserType(anyString(),eq(UserType.DEBTOR));
        doThrow(new IOAPIException(ERROR_MESSAGE, ReasonErrorCode.ERROR_IO_API_IO.getCode()))
                .when(ioServiceMock).isNotifyToIOUserAllowed(any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNotNull(receipt.getReasonErr());
        assertEquals(ReasonErrorCode.ERROR_IO_API_IO.getCode(), receipt.getReasonErr().getCode());
        assertNotNull(receipt.getReasonErr().getMessage());
        assertEquals(ERROR_MESSAGE, receipt.getReasonErr().getMessage());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void notifyFailNotNotifiedGetProfileThrowsErrorToNotifyException() {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());
        doThrow(IoMessageNotFoundException.class).when(receiptCosmosClientMock)
                .findIOMessageWithEventIdAndUserType(anyString(),eq(UserType.DEBTOR));
        doThrow(new ErrorToNotifyException(ERROR_MESSAGE)).when(ioServiceMock).isNotifyToIOUserAllowed(any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNotNull(receipt.getReasonErr());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, receipt.getReasonErr().getCode());
        assertEquals(ERROR_MESSAGE, receipt.getReasonErr().getMessage());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void notifyFailNotToBeNotifiedGetProfileResponseNotAllowed() {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());
        doThrow(IoMessageNotFoundException.class).when(receiptCosmosClientMock)
                .findIOMessageWithEventIdAndUserType(anyString(),eq(UserType.DEBTOR));
        doReturn(false).when(ioServiceMock).isNotifyToIOUserAllowed(any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_TO_BE_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNull(receipt.getReasonErr());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void notifyFailNotNotifiedBuildMessageException() {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());
        doThrow(IoMessageNotFoundException.class).when(receiptCosmosClientMock)
                .findIOMessageWithEventIdAndUserType(anyString(),eq(UserType.DEBTOR));
        doReturn(true).when(ioServiceMock).isNotifyToIOUserAllowed(any());
        doThrow(new MissingFieldsForNotificationException(ERROR_MESSAGE))
                .when(notificationMessageBuilderMock).buildMessagePayload(anyString(), any(), any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNotNull(receipt.getReasonErr());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, receipt.getReasonErr().getCode());
        assertEquals(ERROR_MESSAGE, receipt.getReasonErr().getMessage());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void notifyFailNotNotifiedNotifyReturnIOAPIException() {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());
        doThrow(IoMessageNotFoundException.class).when(receiptCosmosClientMock)
                .findIOMessageWithEventIdAndUserType(anyString(),eq(UserType.DEBTOR));
        doReturn(true).when(ioServiceMock).isNotifyToIOUserAllowed(any());
        doThrow(new IOAPIException(ERROR_MESSAGE, ReasonErrorCode.ERROR_IO_API_UNEXPECTED.getCode()))
                .when(ioServiceMock).sendNotificationToIOUser(any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNotNull(receipt.getReasonErr());
        assertEquals(ReasonErrorCode.ERROR_IO_API_UNEXPECTED.getCode(), receipt.getReasonErr().getCode());
        assertNotNull(receipt.getReasonErr().getMessage());
        assertEquals(ERROR_MESSAGE, receipt.getReasonErr().getMessage());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void notifyFailNotNotifiedNotifyThrowsErrorToNotifyException() {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());
        doThrow(IoMessageNotFoundException.class).when(receiptCosmosClientMock).findIOMessageWithEventIdAndUserType(anyString(),eq(UserType.DEBTOR));
        doReturn(true).when(ioServiceMock).isNotifyToIOUserAllowed(any());
        doThrow(new ErrorToNotifyException(ERROR_MESSAGE)).when(ioServiceMock).sendNotificationToIOUser(any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNotNull(receipt.getReasonErr());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, receipt.getReasonErr().getCode());
        assertEquals(ERROR_MESSAGE, receipt.getReasonErr().getMessage());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void verifyWithSuccessDebtorPayerNotified() {
        EnumMap<UserType, UserNotifyStatus> usersToBeVerified = new EnumMap<>(UserType.class);
        usersToBeVerified.put(UserType.DEBTOR, UserNotifyStatus.NOTIFIED);
        usersToBeVerified.put(UserType.PAYER, UserNotifyStatus.NOTIFIED);

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        eventData.setPayerFiscalCode(VALID_PAYER_CF);
        receipt.setEventData(eventData);
        IOMessageData messageData = new IOMessageData();
        messageData.setIdMessageDebtor(VALID_DEBTOR_MESSAGE_ID);
        messageData.setIdMessagePayer(VALID_PAYER_MESSAGE_ID);
        receipt.setIoMessageData(messageData);

        ArrayList<IOMessage> messagesNotified = new ArrayList<>();

        boolean result = sut.verifyMessagesNotification(usersToBeVerified, messagesNotified, receipt);

        assertFalse(result);
        assertEquals(ReceiptStatusType.IO_NOTIFIED, receipt.getStatus());
        assertEquals(2, messagesNotified.size());
    }

    @Test
    @SneakyThrows
    void verifyFailDebtorNotToBeNotified() {
        EnumMap<UserType, UserNotifyStatus> usersToBeVerified = new EnumMap<>(UserType.class);
        usersToBeVerified.put(UserType.DEBTOR, UserNotifyStatus.NOT_TO_BE_NOTIFIED);

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        receipt.setEventData(eventData);

        ArrayList<IOMessage> messagesNotified = new ArrayList<>();

        boolean result = sut.verifyMessagesNotification(usersToBeVerified, messagesNotified, receipt);

        assertFalse(result);
        assertEquals(ReceiptStatusType.NOT_TO_NOTIFY, receipt.getStatus());
        assertTrue(messagesNotified.isEmpty());
    }

    @Test
    @SneakyThrows
    void verifyFailDebtorNotNotified() {
        Response<SendMessageResult> queueResponse = mockRequeueResponse(com.microsoft.azure.functions.HttpStatus.CREATED.value());
        doReturn(queueResponse).when(notifierQueueClientMock).sendMessageToQueue(anyString());

        EnumMap<UserType, UserNotifyStatus> usersToBeVerified = new EnumMap<>(UserType.class);
        usersToBeVerified.put(UserType.DEBTOR, UserNotifyStatus.NOT_NOTIFIED);

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        receipt.setEventData(eventData);

        ArrayList<IOMessage> messagesNotified = new ArrayList<>();

        boolean result = sut.verifyMessagesNotification(usersToBeVerified, messagesNotified, receipt);

        assertTrue(result);
        assertEquals(ReceiptStatusType.IO_ERROR_TO_NOTIFY, receipt.getStatus());
        assertTrue(messagesNotified.isEmpty());
        assertEquals(1, receipt.getNotificationNumRetry());
    }

    @Test
    @SneakyThrows
    void verifyFailDebtorNotNotifiedPayerNotified() {
        Response<SendMessageResult> queueResponse = mockRequeueResponse(com.microsoft.azure.functions.HttpStatus.CREATED.value());
        doReturn(queueResponse).when(notifierQueueClientMock).sendMessageToQueue(anyString());

        EnumMap<UserType, UserNotifyStatus> usersToBeVerified = new EnumMap<>(UserType.class);
        usersToBeVerified.put(UserType.DEBTOR, UserNotifyStatus.NOT_NOTIFIED);
        usersToBeVerified.put(UserType.PAYER, UserNotifyStatus.NOTIFIED);

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        eventData.setPayerFiscalCode(VALID_PAYER_CF);
        receipt.setEventData(eventData);
        IOMessageData messageData = new IOMessageData();
        messageData.setIdMessagePayer(VALID_PAYER_MESSAGE_ID);
        receipt.setIoMessageData(messageData);

        ArrayList<IOMessage> messagesNotified = new ArrayList<>();

        boolean result = sut.verifyMessagesNotification(usersToBeVerified, messagesNotified, receipt);

        assertTrue(result);
        assertEquals(ReceiptStatusType.IO_ERROR_TO_NOTIFY, receipt.getStatus());
        assertEquals(1, messagesNotified.size());
        assertEquals(receipt.getEventId(), messagesNotified.get(0).getEventId());
        assertEquals(VALID_PAYER_MESSAGE_ID, messagesNotified.get(0).getMessageId());
        assertEquals(1, receipt.getNotificationNumRetry());
    }

    @Test
    @SneakyThrows
    void verifyFailPayerNotNotifiedDebtorNotified() {
        Response<SendMessageResult> queueResponse = mockRequeueResponse(com.microsoft.azure.functions.HttpStatus.CREATED.value());
        doReturn(queueResponse).when(notifierQueueClientMock).sendMessageToQueue(anyString());

        EnumMap<UserType, UserNotifyStatus> usersToBeVerified = new EnumMap<>(UserType.class);
        usersToBeVerified.put(UserType.DEBTOR, UserNotifyStatus.NOTIFIED);
        usersToBeVerified.put(UserType.PAYER, UserNotifyStatus.NOT_NOTIFIED);

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        eventData.setPayerFiscalCode(VALID_PAYER_CF);
        receipt.setEventData(eventData);
        IOMessageData messageData = new IOMessageData();
        messageData.setIdMessageDebtor(VALID_DEBTOR_MESSAGE_ID);
        receipt.setIoMessageData(messageData);

        ArrayList<IOMessage> messagesNotified = new ArrayList<>();

        boolean result = sut.verifyMessagesNotification(usersToBeVerified, messagesNotified, receipt);

        assertTrue(result);
        assertEquals(ReceiptStatusType.IO_ERROR_TO_NOTIFY, receipt.getStatus());
        assertEquals(1, messagesNotified.size());
        assertEquals(receipt.getEventId(), messagesNotified.get(0).getEventId());
        assertEquals(VALID_DEBTOR_MESSAGE_ID, messagesNotified.get(0).getMessageId());
        assertEquals(1, receipt.getNotificationNumRetry());
    }

    @Test
    @SneakyThrows
    void verifyFailDebtorNotNotifiedMaxRetryReached() {
        EnumMap<UserType, UserNotifyStatus> usersToBeVerified = new EnumMap<>(UserType.class);
        usersToBeVerified.put(UserType.DEBTOR, UserNotifyStatus.NOT_NOTIFIED);

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        receipt.setEventData(eventData);
        receipt.setNotificationNumRetry(6);

        ArrayList<IOMessage> messagesNotified = new ArrayList<>();

        boolean result = sut.verifyMessagesNotification(usersToBeVerified, messagesNotified, receipt);

        assertFalse(result);
        assertEquals(ReceiptStatusType.UNABLE_TO_SEND, receipt.getStatus());
        assertTrue(messagesNotified.isEmpty());

        verify(notifierQueueClientMock, never()).sendMessageToQueue(anyString());
    }

    @Test
    @SneakyThrows
    void verifyFailDebtorNotNotifiedRequeueRespond500() {
        Response<SendMessageResult> queueResponse = mockRequeueResponse(com.microsoft.azure.functions.HttpStatus.INTERNAL_SERVER_ERROR.value());
        doReturn(queueResponse).when(notifierQueueClientMock).sendMessageToQueue(anyString());

        EnumMap<UserType, UserNotifyStatus> usersToBeVerified = new EnumMap<>(UserType.class);
        usersToBeVerified.put(UserType.DEBTOR, UserNotifyStatus.NOT_NOTIFIED);

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        receipt.setEventData(eventData);

        ArrayList<IOMessage> messagesNotified = new ArrayList<>();

        boolean result = sut.verifyMessagesNotification(usersToBeVerified, messagesNotified, receipt);

        assertFalse(result);
        assertEquals(ReceiptStatusType.UNABLE_TO_SEND, receipt.getStatus());
        assertTrue(messagesNotified.isEmpty());
    }

    @Test
    @SneakyThrows
    void verifyFailDebtorNotNotifiedRequeueThrowException() {
        doThrow(RuntimeException.class).when(notifierQueueClientMock).sendMessageToQueue(anyString());

        EnumMap<UserType, UserNotifyStatus> usersToBeVerified = new EnumMap<>(UserType.class);
        usersToBeVerified.put(UserType.DEBTOR, UserNotifyStatus.NOT_NOTIFIED);

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);
        EventData eventData = new EventData();
        eventData.setDebtorFiscalCode(VALID_DEBTOR_CF);
        receipt.setEventData(eventData);

        ArrayList<IOMessage> messagesNotified = new ArrayList<>();

        boolean result = sut.verifyMessagesNotification(usersToBeVerified, messagesNotified, receipt);

        assertFalse(result);
        assertEquals(ReceiptStatusType.UNABLE_TO_SEND, receipt.getStatus());
        assertTrue(messagesNotified.isEmpty());
    }

    @NotNull
    private static Response<SendMessageResult> mockRequeueResponse(int status) {
        @SuppressWarnings("unchecked")
        Response<SendMessageResult> queueResponse = mock(Response.class);
        when(queueResponse.getStatusCode()).thenReturn(status);
        return queueResponse;
    }

    @SneakyThrows
    @NotNull
    private HttpResponse<String> mockNotifyResponse(int status, String messageId) {
        @SuppressWarnings("unchecked")
        HttpResponse<String> messageResponse = mock(HttpResponse.class);
        when(messageResponse.statusCode()).thenReturn(status);
        IOMessageResponse ioMessageResponse = IOMessageResponse.builder().id(messageId).build();
        when(messageResponse.body()).thenReturn(objectMapper.writeValueAsString(ioMessageResponse));
        return messageResponse;
    }

    @SneakyThrows
    @NotNull
    private HttpResponse<String> mockGetProfileResponse(int status, boolean allowed) {
        @SuppressWarnings("unchecked")
        HttpResponse<String> getProfileResponse = mock(HttpResponse.class);
        when(getProfileResponse.statusCode()).thenReturn(status);
        IOProfileResponse profile = IOProfileResponse.builder().senderAllowed(allowed).build();
        when(getProfileResponse.body()).thenReturn(objectMapper.writeValueAsString(profile));
        return getProfileResponse;
    }
}