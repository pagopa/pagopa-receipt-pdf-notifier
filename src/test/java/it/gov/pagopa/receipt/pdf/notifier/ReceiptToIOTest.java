package it.gov.pagopa.receipt.pdf.notifier;

import com.azure.core.http.rest.Response;
import com.azure.storage.queue.models.SendMessageResult;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.NotifierQueueClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.EventData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.generated.client.ApiException;
import it.gov.pagopa.receipt.pdf.notifier.generated.client.ApiResponse;
import it.gov.pagopa.receipt.pdf.notifier.generated.client.api.IOClient;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.CreatedMessage;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.LimitedProfile;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;

@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
class ReceiptToIOTest {

    private static final String VALID_DEBTOR_MESSAGE_ID = "valid debtor message id";
    private static final String VALID_PAYER_MESSAGE_ID = "valid payer message id";
    private final String VALID_PAYER_CF = "a valid payer fiscal code";
    private final String VALID_DEBTOR_CF = "a valid debtor fiscal code";
    private final String EVENT_ID = "a valid id";

    private final int MAX_NUMBER_RETRY = Integer.parseInt(System.getenv().getOrDefault("NOTIFY_RECEIPT_MAX_RETRY", "5"));

    @Spy
    private Receipt receipt;

    @Spy
    private ReceiptToIO function;

    @Mock
    private ExecutionContext context;

    @Mock
    private IOClient client;

    @Mock
    private NotifierQueueClientImpl queueClient;

    @Captor
    private ArgumentCaptor<List<Receipt>> receiptCaptor;

    @Captor
    private ArgumentCaptor<String> queueCaptor;

    @Captor
    private ArgumentCaptor<List<IOMessage>> messageCaptor;

    @AfterEach
    public void teardown() throws Exception {
        // reset singleton
        tearDownInstance(IOClient.class);
        tearDownInstance(NotifierQueueClientImpl.class);
    }

    @Test
    void runOkWithDebtorAndPayerDifferentFiscalCodes() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(profile.getSenderAllowed()).thenReturn(true);
        when(getProfileResponse.getData()).thenReturn(profile);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        ///messages
        @SuppressWarnings("unchecked")
        ApiResponse<CreatedMessage> messageResponse = mock(ApiResponse.class);
        when(messageResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        CreatedMessage createdMessage = mock(CreatedMessage.class);
        when(createdMessage.getId()).thenReturn(VALID_DEBTOR_MESSAGE_ID, VALID_PAYER_MESSAGE_ID);
        when(messageResponse.getData()).thenReturn(createdMessage);
        when(client.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any())).thenReturn(messageResponse);

        setMock(IOClient.class, client);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);
        when(eventData.getPayerFiscalCode()).thenReturn(VALID_PAYER_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertEquals(VALID_DEBTOR_MESSAGE_ID, updatedReceipt.getIoMessageData().getIdMessageDebtor());
        assertEquals(VALID_PAYER_MESSAGE_ID, updatedReceipt.getIoMessageData().getIdMessagePayer());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(0, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.IO_NOTIFIED, updatedReceipt.getStatus());
        assertNull(updatedReceipt.getReasonErr());

        verify(documentMessages).setValue(messageCaptor.capture());
        List<IOMessage> messageList = messageCaptor.getValue();
        assertEquals(2, messageList.size());
        for(IOMessage message : messageList){
            assertEquals(EVENT_ID,message.getEventId());

            if(!(message.getMessageId().equals(VALID_PAYER_MESSAGE_ID) || message.getMessageId().equals(VALID_DEBTOR_MESSAGE_ID))){
                fail();
            }
        }
    }

    @Test
    void runOkWithDebtorAndPayerSameFiscalCodes() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(profile.getSenderAllowed()).thenReturn(true);
        when(getProfileResponse.getData()).thenReturn(profile);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        ///messages
        @SuppressWarnings("unchecked")
        ApiResponse<CreatedMessage> messageResponse = mock(ApiResponse.class);
        when(messageResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        CreatedMessage createdMessage = mock(CreatedMessage.class);
        when(createdMessage.getId()).thenReturn(VALID_DEBTOR_MESSAGE_ID);
        when(messageResponse.getData()).thenReturn(createdMessage);
        when(client.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any())).thenReturn(messageResponse);

        setMock(IOClient.class, client);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);
        when(eventData.getPayerFiscalCode()).thenReturn(VALID_DEBTOR_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertEquals(VALID_DEBTOR_MESSAGE_ID, updatedReceipt.getIoMessageData().getIdMessageDebtor());
        assertNull(updatedReceipt.getIoMessageData().getIdMessagePayer());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(0, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.IO_NOTIFIED, updatedReceipt.getStatus());
        assertNull(updatedReceipt.getReasonErr());

        verify(documentMessages).setValue(messageCaptor.capture());
        List<IOMessage> messageList = messageCaptor.getValue();
        assertEquals(1, messageList.size());
        for(IOMessage message : messageList){
            assertEquals(EVENT_ID,message.getEventId());

            if(!(message.getMessageId().equals(VALID_DEBTOR_MESSAGE_ID))){
                fail();
            }
        }
    }

    @Test
    void runOkWithDebtorIOUserPayerNull() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(profile.getSenderAllowed()).thenReturn(true);
        when(getProfileResponse.getData()).thenReturn(profile);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        ///messages
        @SuppressWarnings("unchecked")
        ApiResponse<CreatedMessage> messageResponse = mock(ApiResponse.class);
        when(messageResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        CreatedMessage createdMessage = mock(CreatedMessage.class);
        when(createdMessage.getId()).thenReturn(VALID_DEBTOR_MESSAGE_ID);
        when(messageResponse.getData()).thenReturn(createdMessage);
        when(client.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any())).thenReturn(messageResponse);

        setMock(IOClient.class, client);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);
        when(eventData.getPayerFiscalCode()).thenReturn(null);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertEquals(VALID_DEBTOR_MESSAGE_ID, updatedReceipt.getIoMessageData().getIdMessageDebtor());
        assertNull(updatedReceipt.getIoMessageData().getIdMessagePayer());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(0, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.IO_NOTIFIED, updatedReceipt.getStatus());
        assertNull(updatedReceipt.getReasonErr());

        verify(documentMessages).setValue(messageCaptor.capture());
        List<IOMessage> messageList = messageCaptor.getValue();
        assertEquals(1, messageList.size());
        for(IOMessage message : messageList){
            assertEquals(EVENT_ID,message.getEventId());

            if(!(message.getMessageId().equals(VALID_DEBTOR_MESSAGE_ID))){
                fail();
            }
        }

    }

    @Test
    void runOkWithDebtorNullPayerIOUser() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(profile.getSenderAllowed()).thenReturn(true);
        when(getProfileResponse.getData()).thenReturn(profile);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        ///messages
        @SuppressWarnings("unchecked")
        ApiResponse<CreatedMessage> messageResponse = mock(ApiResponse.class);
        when(messageResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        CreatedMessage createdMessage = mock(CreatedMessage.class);
        when(createdMessage.getId()).thenReturn(VALID_PAYER_MESSAGE_ID);
        when(messageResponse.getData()).thenReturn(createdMessage);
        when(client.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any())).thenReturn(messageResponse);

        setMock(IOClient.class, client);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(null);
        when(eventData.getPayerFiscalCode()).thenReturn(VALID_PAYER_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertEquals(VALID_PAYER_MESSAGE_ID, updatedReceipt.getIoMessageData().getIdMessagePayer());
        assertNull(updatedReceipt.getIoMessageData().getIdMessageDebtor());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(0, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.IO_NOTIFIED, updatedReceipt.getStatus());
        assertNull(updatedReceipt.getReasonErr());

        verify(documentMessages).setValue(messageCaptor.capture());
        List<IOMessage> messageList = messageCaptor.getValue();
        assertEquals(1, messageList.size());
        for(IOMessage message : messageList){
            assertEquals(EVENT_ID,message.getEventId());

            if(!(message.getMessageId().equals(VALID_PAYER_MESSAGE_ID))){
                fail();
            }
        }
    }

    @Test
    void runOkWithDebtorAndPayerNotIOUser() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(profile.getSenderAllowed()).thenReturn(false);
        when(getProfileResponse.getData()).thenReturn(profile);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        setMock(IOClient.class, client);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);
        when(eventData.getPayerFiscalCode()).thenReturn(VALID_PAYER_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertNull(updatedReceipt.getIoMessageData());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(0, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.NOT_TO_NOTIFY, updatedReceipt.getStatus());
        assertNull(updatedReceipt.getReasonErr());

        verify(documentMessages, never()).setValue(any());
    }

    @Test
    void runOkWithDebtorNullAndPayerNull() {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(null);
        when(eventData.getPayerFiscalCode()).thenReturn(null);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertNull(updatedReceipt.getIoMessageData());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(0, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.NOT_TO_NOTIFY, updatedReceipt.getStatus());
        assertNull(updatedReceipt.getReasonErr());

        verify(documentMessages, never()).setValue(any());
    }

    @Test
    void runOkWithStatusSIGNED() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(profile.getSenderAllowed()).thenReturn(true);
        when(getProfileResponse.getData()).thenReturn(profile);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        ///messages
        @SuppressWarnings("unchecked")
        ApiResponse<CreatedMessage> messageResponse = mock(ApiResponse.class);
        when(messageResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        CreatedMessage createdMessage = mock(CreatedMessage.class);
        when(createdMessage.getId()).thenReturn(VALID_DEBTOR_MESSAGE_ID, VALID_PAYER_MESSAGE_ID);
        when(messageResponse.getData()).thenReturn(createdMessage);
        when(client.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any())).thenReturn(messageResponse);

        setMock(IOClient.class, client);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);
        when(eventData.getPayerFiscalCode()).thenReturn(VALID_PAYER_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.SIGNED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertEquals(VALID_DEBTOR_MESSAGE_ID, updatedReceipt.getIoMessageData().getIdMessageDebtor());
        assertEquals(VALID_PAYER_MESSAGE_ID, updatedReceipt.getIoMessageData().getIdMessagePayer());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(0, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.IO_NOTIFIED, updatedReceipt.getStatus());
        assertNull(updatedReceipt.getReasonErr());

        verify(documentMessages).setValue(messageCaptor.capture());
        List<IOMessage> messageList = messageCaptor.getValue();
        assertEquals(2, messageList.size());
        for(IOMessage message : messageList){
            assertEquals(EVENT_ID,message.getEventId());

            if(!(message.getMessageId().equals(VALID_PAYER_MESSAGE_ID) || message.getMessageId().equals(VALID_DEBTOR_MESSAGE_ID))){
                fail();
            }
        }
    }

    @Test
    void runOkWithStatusIO_NOTIFIER_RETRY() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(profile.getSenderAllowed()).thenReturn(true);
        when(getProfileResponse.getData()).thenReturn(profile);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        ///messages
        @SuppressWarnings("unchecked")
        ApiResponse<CreatedMessage> messageResponse = mock(ApiResponse.class);
        when(messageResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        CreatedMessage createdMessage = mock(CreatedMessage.class);
        when(createdMessage.getId()).thenReturn(VALID_DEBTOR_MESSAGE_ID, VALID_PAYER_MESSAGE_ID);
        when(messageResponse.getData()).thenReturn(createdMessage);
        when(client.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any())).thenReturn(messageResponse);

        setMock(IOClient.class, client);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);
        when(eventData.getPayerFiscalCode()).thenReturn(VALID_PAYER_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.IO_NOTIFIER_RETRY);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertEquals(VALID_DEBTOR_MESSAGE_ID, updatedReceipt.getIoMessageData().getIdMessageDebtor());
        assertEquals(VALID_PAYER_MESSAGE_ID, updatedReceipt.getIoMessageData().getIdMessagePayer());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(0, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.IO_NOTIFIED, updatedReceipt.getStatus());
        assertNull(updatedReceipt.getReasonErr());

        verify(documentMessages).setValue(messageCaptor.capture());
        List<IOMessage> messageList = messageCaptor.getValue();
        assertEquals(2, messageList.size());
        for(IOMessage message : messageList){
            assertEquals(EVENT_ID,message.getEventId());

            if(!(message.getMessageId().equals(VALID_PAYER_MESSAGE_ID) || message.getMessageId().equals(VALID_DEBTOR_MESSAGE_ID))){
                fail();
            }
        }
    }

    @Test
    void runKoErrorResponseProfileBothDebtorAndPayer() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(getProfileResponse.getData()).thenReturn(profile);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        setMock(IOClient.class, client);

        @SuppressWarnings("unchecked")
        Response<SendMessageResult> queueResponse = mock(Response.class);
        when(queueResponse.getStatusCode()).thenReturn(com.microsoft.azure.functions.HttpStatus.CREATED.value());
        when(queueClient.sendMessageToQueue(anyString())).thenReturn(queueResponse);

        setMock(NotifierQueueClientImpl.class, queueClient);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);
        when(eventData.getPayerFiscalCode()).thenReturn(VALID_PAYER_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertNull(updatedReceipt.getIoMessageData());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(1, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.IO_ERROR_TO_NOTIFY, updatedReceipt.getStatus());
        assertNotNull(updatedReceipt.getReasonErr());

        verify(documentMessages, never()).setValue(any());
    }

    @Test
    void runKoErrorResponseProfileOnDebtor() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(profile.getSenderAllowed()).thenReturn(true);
        when(getProfileResponse.getData()).thenReturn(profile);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        ///messages
        @SuppressWarnings("unchecked")
        ApiResponse<CreatedMessage> messageResponse = mock(ApiResponse.class);
        when(messageResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        CreatedMessage createdMessage = mock(CreatedMessage.class);
        when(createdMessage.getId()).thenReturn(VALID_DEBTOR_MESSAGE_ID);
        when(messageResponse.getData()).thenReturn(createdMessage);
        when(client.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any())).thenReturn(messageResponse);

        setMock(IOClient.class, client);

        @SuppressWarnings("unchecked")
        Response<SendMessageResult> queueResponse = mock(Response.class);
        when(queueResponse.getStatusCode()).thenReturn(com.microsoft.azure.functions.HttpStatus.CREATED.value());
        when(queueClient.sendMessageToQueue(anyString())).thenReturn(queueResponse);

        setMock(NotifierQueueClientImpl.class, queueClient);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);
        when(eventData.getPayerFiscalCode()).thenReturn(VALID_PAYER_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertEquals(VALID_DEBTOR_MESSAGE_ID, updatedReceipt.getIoMessageData().getIdMessageDebtor());
        assertNull(updatedReceipt.getIoMessageData().getIdMessagePayer());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(1, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.IO_ERROR_TO_NOTIFY, updatedReceipt.getStatus());
        assertNotNull(updatedReceipt.getReasonErr());

        verify(documentMessages).setValue(messageCaptor.capture());
        List<IOMessage> messageList = messageCaptor.getValue();
        assertEquals(1, messageList.size());
        for(IOMessage message : messageList){
            assertEquals(EVENT_ID,message.getEventId());

            if(!(message.getMessageId().equals(VALID_DEBTOR_MESSAGE_ID))){
                fail();
            }
        }
    }

    @Test
    void runKoErrorResponseProfileOnPayer() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR, HttpStatus.SC_OK);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(profile.getSenderAllowed()).thenReturn(true);
        when(getProfileResponse.getData()).thenReturn(profile);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        ///messages
        @SuppressWarnings("unchecked")
        ApiResponse<CreatedMessage> messageResponse = mock(ApiResponse.class);
        when(messageResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        CreatedMessage createdMessage = mock(CreatedMessage.class);
        when(createdMessage.getId()).thenReturn(VALID_PAYER_MESSAGE_ID);
        when(messageResponse.getData()).thenReturn(createdMessage);
        when(client.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any())).thenReturn(messageResponse);

        setMock(IOClient.class, client);

        @SuppressWarnings("unchecked")
        Response<SendMessageResult> queueResponse = mock(Response.class);
        when(queueResponse.getStatusCode()).thenReturn(com.microsoft.azure.functions.HttpStatus.CREATED.value());
        when(queueClient.sendMessageToQueue(anyString())).thenReturn(queueResponse);

        setMock(NotifierQueueClientImpl.class, queueClient);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);
        when(eventData.getPayerFiscalCode()).thenReturn(VALID_PAYER_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertNull(updatedReceipt.getIoMessageData().getIdMessageDebtor());
        assertEquals(VALID_PAYER_MESSAGE_ID, updatedReceipt.getIoMessageData().getIdMessagePayer());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(1, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.IO_ERROR_TO_NOTIFY, updatedReceipt.getStatus());
        assertNotNull(updatedReceipt.getReasonErr());

        verify(documentMessages).setValue(messageCaptor.capture());
        List<IOMessage> messageList = messageCaptor.getValue();
        assertEquals(1, messageList.size());
        for(IOMessage message : messageList){
            assertEquals(EVENT_ID,message.getEventId());

            if(!(message.getMessageId().equals(VALID_PAYER_MESSAGE_ID))){
                fail();
            }
        }
    }

    @Test
    void runKoErrorResponseMessagesBothDebtorAndPayer() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(profile.getSenderAllowed()).thenReturn(true);
        when(getProfileResponse.getData()).thenReturn(profile);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        @SuppressWarnings("unchecked")
        ApiResponse<CreatedMessage> messageResponse = mock(ApiResponse.class);
        when(messageResponse.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(client.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any())).thenReturn(messageResponse);

        setMock(IOClient.class, client);

        @SuppressWarnings("unchecked")
        Response<SendMessageResult> queueResponse = mock(Response.class);
        when(queueResponse.getStatusCode()).thenReturn(com.microsoft.azure.functions.HttpStatus.CREATED.value());
        when(queueClient.sendMessageToQueue(anyString())).thenReturn(queueResponse);

        setMock(NotifierQueueClientImpl.class, queueClient);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);
        when(eventData.getPayerFiscalCode()).thenReturn(VALID_PAYER_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertNull(updatedReceipt.getIoMessageData());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(1, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.IO_ERROR_TO_NOTIFY, updatedReceipt.getStatus());
        assertNotNull(updatedReceipt.getReasonErr());

        verify(documentMessages, never()).setValue(any());
    }

    @Test
    void runKoErrorResponseMessagesOnDebtor() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(profile.getSenderAllowed()).thenReturn(true);
        when(getProfileResponse.getData()).thenReturn(profile);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        @SuppressWarnings("unchecked")
        ApiResponse<CreatedMessage> messageResponse = mock(ApiResponse.class);
        when(messageResponse.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR, HttpStatus.SC_OK);
        CreatedMessage createdMessage = mock(CreatedMessage.class);
        when(createdMessage.getId()).thenReturn(VALID_PAYER_MESSAGE_ID);
        when(messageResponse.getData()).thenReturn(createdMessage);
        when(client.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any())).thenReturn(messageResponse);

        setMock(IOClient.class, client);

        @SuppressWarnings("unchecked")
        Response<SendMessageResult> queueResponse = mock(Response.class);
        when(queueResponse.getStatusCode()).thenReturn(com.microsoft.azure.functions.HttpStatus.CREATED.value());
        when(queueClient.sendMessageToQueue(anyString())).thenReturn(queueResponse);

        setMock(NotifierQueueClientImpl.class, queueClient);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);
        when(eventData.getPayerFiscalCode()).thenReturn(VALID_PAYER_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertEquals(VALID_PAYER_MESSAGE_ID, updatedReceipt.getIoMessageData().getIdMessagePayer());
        assertNull(updatedReceipt.getIoMessageData().getIdMessageDebtor());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(1, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.IO_ERROR_TO_NOTIFY, updatedReceipt.getStatus());
        assertNotNull(updatedReceipt.getReasonErr());

        verify(documentMessages).setValue(messageCaptor.capture());
        List<IOMessage> messageList = messageCaptor.getValue();
        assertEquals(1, messageList.size());
        for(IOMessage message : messageList){
            assertEquals(EVENT_ID,message.getEventId());

            if(!(message.getMessageId().equals(VALID_PAYER_MESSAGE_ID))){
                fail();
            }
        }
    }

    @Test
    void runKoErrorResponseMessagesOnPayer() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(profile.getSenderAllowed()).thenReturn(true);
        when(getProfileResponse.getData()).thenReturn(profile);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        @SuppressWarnings("unchecked")
        ApiResponse<CreatedMessage> messageResponse = mock(ApiResponse.class);
        when(messageResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        CreatedMessage createdMessage = mock(CreatedMessage.class);
        when(createdMessage.getId()).thenReturn(VALID_DEBTOR_MESSAGE_ID);
        when(messageResponse.getData()).thenReturn(createdMessage);
        when(client.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any())).thenReturn(messageResponse);

        setMock(IOClient.class, client);

        @SuppressWarnings("unchecked")
        Response<SendMessageResult> queueResponse = mock(Response.class);
        when(queueResponse.getStatusCode()).thenReturn(com.microsoft.azure.functions.HttpStatus.CREATED.value());
        when(queueClient.sendMessageToQueue(anyString())).thenReturn(queueResponse);

        setMock(NotifierQueueClientImpl.class, queueClient);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);
        when(eventData.getPayerFiscalCode()).thenReturn(VALID_PAYER_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertEquals(VALID_DEBTOR_MESSAGE_ID, updatedReceipt.getIoMessageData().getIdMessageDebtor());
        assertNull(updatedReceipt.getIoMessageData().getIdMessagePayer());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(1, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.IO_ERROR_TO_NOTIFY, updatedReceipt.getStatus());
        assertNotNull(updatedReceipt.getReasonErr());

        verify(documentMessages).setValue(messageCaptor.capture());
        List<IOMessage> messageList = messageCaptor.getValue();
        assertEquals(1, messageList.size());
        for(IOMessage message : messageList){
            assertEquals(EVENT_ID,message.getEventId());

            if(!(message.getMessageId().equals(VALID_DEBTOR_MESSAGE_ID))){
                fail();
            }
        }
    }

    @Test
    void runKoTooManyRetry() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        setMock(IOClient.class, client);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);
        receipt.setNotificationNumRetry(MAX_NUMBER_RETRY);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertNull(updatedReceipt.getIoMessageData());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(MAX_NUMBER_RETRY+1, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.UNABLE_TO_SEND, updatedReceipt.getStatus());
        assertNotNull(updatedReceipt.getReasonErr());

        verify(documentMessages, never()).setValue(any());
    }

    @Test
    void runKoErrorSendingToQueue() throws ApiException {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);

        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        setMock(IOClient.class, client);

        @SuppressWarnings("unchecked")
        Response<SendMessageResult> queueResponse = mock(Response.class);
        when(queueResponse.getStatusCode()).thenReturn(com.microsoft.azure.functions.HttpStatus.I_AM_A_TEAPOT.value());
        when(queueClient.sendMessageToQueue(anyString())).thenReturn(queueResponse);

        setMock(NotifierQueueClientImpl.class, queueClient);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);

        Assertions.assertDoesNotThrow(() ->
                function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                ));

        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertNull(updatedReceipt.getIoMessageData());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(ReceiptStatusType.UNABLE_TO_SEND, updatedReceipt.getStatus());
        assertNotNull(updatedReceipt.getReasonErr());

        verify(documentMessages, never()).setValue(any());
    }

    @Test
    void runOkWithDebtorAndPayerSameFiscalCodesInFiltered() throws Exception {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);


        ///profile
        @SuppressWarnings("unchecked")
        ApiResponse<LimitedProfile> getProfileResponse = mock(ApiResponse.class);
        when(getProfileResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        LimitedProfile profile = mock(LimitedProfile.class);
        when(profile.getSenderAllowed()).thenReturn(true);
        when(getProfileResponse.getData()).thenReturn(profile);

        when(client.getProfileByPOSTWithHttpInfo(any())).thenReturn(getProfileResponse);

        ///messages
        @SuppressWarnings("unchecked")
        ApiResponse<CreatedMessage> messageResponse = mock(ApiResponse.class);
        when(messageResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        CreatedMessage createdMessage = mock(CreatedMessage.class);
        when(createdMessage.getId()).thenReturn(VALID_DEBTOR_MESSAGE_ID);
        when(messageResponse.getData()).thenReturn(createdMessage);
        when(client.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(any())).thenReturn(messageResponse);

        setMock(IOClient.class, client);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn(VALID_DEBTOR_CF);
        when(eventData.getPayerFiscalCode()).thenReturn(VALID_DEBTOR_CF);

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);


        withEnvironmentVariable("CF_FILTER_NOTIFIER", VALID_DEBTOR_CF.concat(",").concat(VALID_PAYER_CF))
                .and("CF_FILTER_ENABLED", "true")
                .execute(() -> Assertions.assertDoesNotThrow(() ->
                        function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                        )));


        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertEquals(VALID_DEBTOR_MESSAGE_ID, updatedReceipt.getIoMessageData().getIdMessageDebtor());
        assertNull(updatedReceipt.getIoMessageData().getIdMessagePayer());
        assertEquals(EVENT_ID, updatedReceipt.getEventId());
        assertEquals(0, updatedReceipt.getNotificationNumRetry());
        assertEquals(ReceiptStatusType.IO_NOTIFIED, updatedReceipt.getStatus());
        assertNull(updatedReceipt.getReasonErr());

        verify(documentMessages).setValue(messageCaptor.capture());
        List<IOMessage> messageList = messageCaptor.getValue();
        assertEquals(1, messageList.size());
        for(IOMessage message : messageList){
            assertEquals(EVENT_ID,message.getEventId());

            if(!(message.getMessageId().equals(VALID_DEBTOR_MESSAGE_ID))){
                fail();
            }
        }
    }

    @Test
    void runOkWithDebtorAndPayerSameFiscalCodesInFilteredKO() throws Exception {
        Logger logger = Logger.getLogger("ReceiptToIO-test-logger");
        when(context.getLogger()).thenReturn(logger);


        setMock(IOClient.class, client);

        List<Receipt> receiptList = new ArrayList<>();
        EventData eventData = mock(EventData.class);
        when(eventData.getDebtorFiscalCode()).thenReturn("A");
        when(eventData.getPayerFiscalCode()).thenReturn("A");

        receipt.setEventData(eventData);
        receipt.setEventId(EVENT_ID);
        receipt.setStatus(ReceiptStatusType.GENERATED);

        receiptList.add(receipt);

        @SuppressWarnings("unchecked")
        OutputBinding<List<Receipt>> documentReceipts = (OutputBinding<List<Receipt>>) spy(OutputBinding.class);
        @SuppressWarnings("unchecked")
        OutputBinding<List<IOMessage>> documentMessages = (OutputBinding<List<IOMessage>>) spy(OutputBinding.class);


        withEnvironmentVariable("CF_FILTER_NOTIFIER", VALID_DEBTOR_CF.concat(",").concat(VALID_PAYER_CF))
                .and("CF_FILTER_ENABLED", "true")
                .execute(() -> Assertions.assertDoesNotThrow(() ->
                        function.processReceiptToIO(receiptList, documentReceipts, documentMessages, context
                        )));


        //Verify receipts update
        verify(documentReceipts).setValue(receiptCaptor.capture());
        Receipt updatedReceipt = receiptCaptor.getValue().get(0);
        assertEquals(ReceiptStatusType.NOT_TO_NOTIFY, updatedReceipt.getStatus());
        assertNull(updatedReceipt.getReasonErr());

    }

    private <T> void tearDownInstance(Class<T> classInstanced) throws IllegalAccessException, NoSuchFieldException {
        Field instance = classInstanced.getDeclaredField("instance");

        instance.setAccessible(true);
        instance.set(null, null);
    }

    private static <T> void setMock(Class<T> classToMock, T mock) {
        try {
            Field instance = classToMock.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}