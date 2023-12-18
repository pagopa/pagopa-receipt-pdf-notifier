package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.models.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.client.NotifierQueueClient;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.EventData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.IOMessageData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReasonErrorCode;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.exception.MissingFieldsForNotificationException;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
import it.gov.pagopa.receipt.pdf.notifier.generated.client.ApiException;
import it.gov.pagopa.receipt.pdf.notifier.generated.client.ApiResponse;
import it.gov.pagopa.receipt.pdf.notifier.generated.client.api.IOClient;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.CreatedMessage;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.LimitedProfile;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.service.IOMessageService;
import it.gov.pagopa.receipt.pdf.notifier.service.PDVTokenizerServiceRetryWrapper;
import it.gov.pagopa.receipt.pdf.notifier.service.ReceiptToIOService;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.ArrayList;
import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
class ReceiptToIOServiceImplTest {

    private static final String VALID_DEBTOR_MESSAGE_ID = "valid debtor message id";
    private static final String VALID_PAYER_MESSAGE_ID = "valid payer message id";
    private static final String VALID_PAYER_CF = "JHNDOE80D45E507N";
    private static final String INVALID_CF = "an invalid fiscal code";
    private static final String VALID_DEBTOR_CF = "JHNDOE80D05B157Y";
    private static final String EVENT_ID = "123";

    @SystemStub
    private EnvironmentVariables environmentVariables = new EnvironmentVariables("CF_FILTER_NOTIFIER", VALID_DEBTOR_CF + "," + VALID_PAYER_CF);

    private IOClient ioClientMock;

    private NotifierQueueClient notifierQueueClientMock;

    private IOMessageService ioMessageServiceMock;

    private PDVTokenizerServiceRetryWrapper pdvTokenizerServiceRetryWrapperMock;

    private ReceiptToIOService sut;

    @BeforeEach
    void setUp() {
        ioClientMock = mock(IOClient.class);
        notifierQueueClientMock = mock(NotifierQueueClient.class);
        ioMessageServiceMock = mock(IOMessageService.class);
        pdvTokenizerServiceRetryWrapperMock = mock(PDVTokenizerServiceRetryWrapper.class);
        sut = new ReceiptToIOServiceImpl(ioClientMock, notifierQueueClientMock, ioMessageServiceMock, pdvTokenizerServiceRetryWrapperMock);
    }

    @Test
    @SneakyThrows
    void notifyDebtorWithSuccess() {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());

        ApiResponse<LimitedProfile> getProfileResponse = mockGetProfileResponse(HttpStatus.SC_OK, true);
        doReturn(getProfileResponse).when(ioClientMock).getProfileByPOSTWithHttpInfo(any());

        ApiResponse<CreatedMessage> messageResponse = mockNotifyResponse(HttpStatus.SC_CREATED, VALID_DEBTOR_MESSAGE_ID);
        doReturn(messageResponse).when(ioClientMock).submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any());

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

        ApiResponse<LimitedProfile> getProfileResponse = mockGetProfileResponse(HttpStatus.SC_OK, true);
        doReturn(getProfileResponse).when(ioClientMock).getProfileByPOSTWithHttpInfo(any());

        ApiResponse<CreatedMessage> messageResponse = mockNotifyResponse(HttpStatus.SC_CREATED, VALID_PAYER_MESSAGE_ID);
        doReturn(messageResponse).when(ioClientMock).submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any());

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

        ApiResponse<LimitedProfile> getProfileResponse = mockGetProfileResponse(HttpStatus.SC_OK, true);
        doReturn(getProfileResponse).when(ioClientMock).getProfileByPOSTWithHttpInfo(any());

        ApiResponse<CreatedMessage> messageResponse = mockNotifyResponse(HttpStatus.SC_CREATED, VALID_DEBTOR_MESSAGE_ID);
        doReturn(messageResponse).when(ioClientMock).submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any());

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

        ApiResponse<LimitedProfile> getProfileResponse = mockGetProfileResponse(HttpStatus.SC_OK, true);
        doReturn(getProfileResponse).when(ioClientMock).getProfileByPOSTWithHttpInfo(any());

        ApiResponse<CreatedMessage> messageResponse = mockNotifyResponse(HttpStatus.SC_CREATED, VALID_PAYER_MESSAGE_ID);
        doReturn(messageResponse).when(ioClientMock).submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any());

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
    void notifyFailDebtorNotNotifiedGetProfileResponse404() throws PDVTokenizerException, JsonProcessingException, ApiException {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());

        doThrow(new ApiException(HttpStatus.SC_NOT_FOUND, "not found"))
                .when(ioClientMock).getProfileByPOSTWithHttpInfo(any());

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
    void notifyFailDebtorNotNotifiedGetProfileResponse400() throws PDVTokenizerException, JsonProcessingException, ApiException {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());

        doThrow(new ApiException(HttpStatus.SC_BAD_REQUEST, "bad request"))
                .when(ioClientMock).getProfileByPOSTWithHttpInfo(any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNotNull(receipt.getReasonErr());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, receipt.getReasonErr().getCode());
        assertNotNull(receipt.getReasonErr().getMessage());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    void notifyFailDebtorNotNotifiedGetProfileResponseNull() throws PDVTokenizerException, JsonProcessingException {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNotNull(receipt.getReasonErr());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, receipt.getReasonErr().getCode());
        assertNotNull(receipt.getReasonErr().getMessage());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    void notifyFailPayerNotNotifiedGetProfileResponseNull() throws PDVTokenizerException, JsonProcessingException {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_PAYER_CF, UserType.PAYER, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNull(receipt.getReasonErr());
        assertNotNull(receipt.getReasonErrPayer());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, receipt.getReasonErrPayer().getCode());
        assertNotNull(receipt.getReasonErrPayer().getMessage());
    }

    @Test
    @SneakyThrows
    void notifyFailNotNotifiedGetProfileResponseStatusNotOK() {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());

        ApiResponse<LimitedProfile> getProfileResponse = mockGetProfileResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, false);
        doReturn(getProfileResponse).when(ioClientMock).getProfileByPOSTWithHttpInfo(any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNotNull(receipt.getReasonErr());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, receipt.getReasonErr().getCode());
        assertNotNull(receipt.getReasonErr().getMessage());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void notifyFailNotToBeNotifiedGetProfileResponseNotAllowed() {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());

        ApiResponse<LimitedProfile> getProfileResponse = mockGetProfileResponse(HttpStatus.SC_OK, false);
        doReturn(getProfileResponse).when(ioClientMock).getProfileByPOSTWithHttpInfo(any());

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

        ApiResponse<LimitedProfile> getProfileResponse = mockGetProfileResponse(HttpStatus.SC_OK, true);
        doReturn(getProfileResponse).when(ioClientMock).getProfileByPOSTWithHttpInfo(any());

        String errorMessage = "error message";
        doThrow(new MissingFieldsForNotificationException(errorMessage)).when(ioMessageServiceMock).buildNewMessage(anyString(), any(), any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNotNull(receipt.getReasonErr());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, receipt.getReasonErr().getCode());
        assertEquals(errorMessage, receipt.getReasonErr().getMessage());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void notifyFailNotNotifiedNotifyReturnException() {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());

        ApiResponse<LimitedProfile> getProfileResponse = mockGetProfileResponse(HttpStatus.SC_OK, true);
        doReturn(getProfileResponse).when(ioClientMock).getProfileByPOSTWithHttpInfo(any());

        doThrow(ApiException.class).when(ioClientMock).submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNotNull(receipt.getReasonErr());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, receipt.getReasonErr().getCode());
        assertNotNull(receipt.getReasonErr().getMessage());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void notifyFailNotNotifiedNotifyResponseNull() {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());

        ApiResponse<LimitedProfile> getProfileResponse = mockGetProfileResponse(HttpStatus.SC_OK, true);
        doReturn(getProfileResponse).when(ioClientMock).getProfileByPOSTWithHttpInfo(any());

        doReturn(null).when(ioClientMock).submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNotNull(receipt.getReasonErr());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, receipt.getReasonErr().getCode());
        assertNotNull(receipt.getReasonErr().getMessage());
        assertNull(receipt.getReasonErrPayer());
    }

    @Test
    @SneakyThrows
    void notifyFailNotNotifiedNotifyResponseStatusNotCreated() {
        doReturn(VALID_DEBTOR_CF).when(pdvTokenizerServiceRetryWrapperMock).getFiscalCodeWithRetry(anyString());

        ApiResponse<LimitedProfile> getProfileResponse = mockGetProfileResponse(HttpStatus.SC_OK, true);
        doReturn(getProfileResponse).when(ioClientMock).getProfileByPOSTWithHttpInfo(any());

        ApiResponse<CreatedMessage> messageResponse = mockNotifyResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, null);
        doReturn(messageResponse).when(ioClientMock).submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any());

        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        UserNotifyStatus userNotifyStatus = sut.notifyMessage(VALID_DEBTOR_CF, UserType.DEBTOR, receipt);

        assertNotNull(userNotifyStatus);
        assertEquals(UserNotifyStatus.NOT_NOTIFIED, userNotifyStatus);
        assertNull(receipt.getIoMessageData());
        assertNotNull(receipt.getReasonErr());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, receipt.getReasonErr().getCode());
        assertNotNull(receipt.getReasonErr().getMessage());
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
    void verifyFailDebtorNotNotifiedWithMessageDataNull() {
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
        Response<SendMessageResult> queueResponse = mockRequeueResponse(com.microsoft.azure.functions.HttpStatus.CREATED.value());
        doReturn(queueResponse).when(notifierQueueClientMock).sendMessageToQueue(anyString());

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

    @NotNull
    private ApiResponse<CreatedMessage> mockNotifyResponse(int status, String messageId) {
        @SuppressWarnings("unchecked")
        ApiResponse<CreatedMessage> messageResponse = mock(ApiResponse.class);
        when(messageResponse.getStatusCode()).thenReturn(status);
        CreatedMessage createdMessage = mock(CreatedMessage.class);
        when(createdMessage.getId()).thenReturn(messageId);
        when(messageResponse.getData()).thenReturn(createdMessage);
        return messageResponse;
    }

    @NotNull
    private ApiResponse<LimitedProfile> mockGetProfileResponse(int status, boolean allowed) {
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(status);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(profile.getSenderAllowed()).thenReturn(allowed);
        when(getProfileResponse.getData()).thenReturn(profile);
        return getProfileResponse;
    }
}