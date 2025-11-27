package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.receipt.pdf.notifier.client.IOClient;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReasonErrorCode;
import it.gov.pagopa.receipt.pdf.notifier.exception.ErrorToNotifyException;
import it.gov.pagopa.receipt.pdf.notifier.exception.IOAPIException;
import it.gov.pagopa.receipt.pdf.notifier.model.io.IOProfileResponse;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.IOMessageResponse;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IOServiceImplTest {

    private static final String CF = "JHNDOE80D05B157Y";
    private static final String MESSAGE_ID = "message id";
    private static final String ERROR_MESSAGE = "error message";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private HttpResponse<String> getProfileResponse;
    @Mock
    private HttpResponse<String> notifyResponse;


    @Mock
    private IOClient ioClientMock;

    @InjectMocks
    private IOServiceImpl sut;

    @Test
    @SneakyThrows
    void isNotifyToIOUserAllowedOK() {
        IOProfileResponse profile = IOProfileResponse.builder().senderAllowed(true).build();

        when(getProfileResponse.statusCode()).thenReturn(HttpStatus.SC_OK);
        when(getProfileResponse.body()).thenReturn(objectMapper.writeValueAsString(profile));
        when(ioClientMock.getProfile(anyString())).thenReturn(getProfileResponse);

        Boolean result = assertDoesNotThrow(() -> sut.isNotifyToIOUserAllowed(CF));

        assertTrue(result);
    }

    @Test
    @SneakyThrows
    void isNotifyToIOUserAllowedOKNotAllowed() {
        IOProfileResponse profile = IOProfileResponse.builder().senderAllowed(false).build();

        when(getProfileResponse.statusCode()).thenReturn(HttpStatus.SC_OK);
        when(getProfileResponse.body()).thenReturn(objectMapper.writeValueAsString(profile));
        when(ioClientMock.getProfile(anyString())).thenReturn(getProfileResponse);

        Boolean result = assertDoesNotThrow(() -> sut.isNotifyToIOUserAllowed(CF));

        assertFalse(result);
    }

    @Test
    @SneakyThrows
    void isNotifyToIOUserAllowedKOUnexpectedError() {
        when(ioClientMock.getProfile(anyString()))
                .thenThrow(new IOAPIException(ERROR_MESSAGE, ReasonErrorCode.ERROR_IO_API_IO.getCode()));

        IOAPIException e = assertThrows(IOAPIException.class, () -> sut.isNotifyToIOUserAllowed(CF));

        assertNotNull(e);
        assertEquals(ERROR_MESSAGE, e.getMessage());
        assertEquals(ReasonErrorCode.ERROR_IO_API_IO.getCode(), e.getStatusCode());
    }

    @Test
    @SneakyThrows
    void isNotifyToIOUserAllowedKOResponseNull() {
        when(ioClientMock.getProfile(anyString())).thenReturn(null);

        ErrorToNotifyException e = assertThrows(ErrorToNotifyException.class, () -> sut.isNotifyToIOUserAllowed(CF));

        assertNotNull(e);
        assertNotNull(e.getMessage());
        assertEquals("IO /profiles failed to respond", e.getMessage());
    }

    @Test
    @SneakyThrows
    void isNotifyToIOUserAllowedKOResponseNot2xx() {
        when(getProfileResponse.statusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        when(ioClientMock.getProfile(anyString())).thenReturn(getProfileResponse);

        ErrorToNotifyException e = assertThrows(ErrorToNotifyException.class, () -> sut.isNotifyToIOUserAllowed(CF));

        assertNotNull(e);
        assertNotNull(e.getMessage());
        assertTrue(e.getMessage().startsWith("IO /profiles responded with code"));
    }

    @Test
    @SneakyThrows
    void isNotifyToIOUserAllowedKOFailToParseResponse() {
        when(getProfileResponse.statusCode()).thenReturn(HttpStatus.SC_OK);
        when(getProfileResponse.body()).thenReturn("][");
        when(ioClientMock.getProfile(anyString())).thenReturn(getProfileResponse);

        ErrorToNotifyException e = assertThrows(ErrorToNotifyException.class, () -> sut.isNotifyToIOUserAllowed(CF));

        assertNotNull(e);
        assertNotNull(e.getMessage());
        assertEquals("Failed to deserialize response of IO API invocation", e.getMessage());
    }

    @Test
    @SneakyThrows
    void sendNotificationToIOUserOK() {
        when(notifyResponse.statusCode()).thenReturn(HttpStatus.SC_CREATED);
        IOMessageResponse ioMessageResponse = IOMessageResponse.builder().id(MESSAGE_ID).build();
        when(notifyResponse.body()).thenReturn(objectMapper.writeValueAsString(ioMessageResponse));
        when(ioClientMock.submitMessage(anyString())).thenReturn(notifyResponse);

        String result = assertDoesNotThrow(() -> sut.sendNotificationToIOUser(any()));

        assertNotNull(result);
        assertEquals(MESSAGE_ID, result);
    }

    @Test
    @SneakyThrows
    void sendNotificationToIOUserKOUnexpectedError() {
        when(ioClientMock.submitMessage(anyString()))
                .thenThrow(new IOAPIException(ERROR_MESSAGE, ReasonErrorCode.ERROR_IO_API_IO.getCode()));

        IOAPIException e = assertThrows(IOAPIException.class, () -> sut.sendNotificationToIOUser(any()));

        assertNotNull(e);
        assertEquals(ERROR_MESSAGE, e.getMessage());
        assertEquals(ReasonErrorCode.ERROR_IO_API_IO.getCode(), e.getStatusCode());
    }

    @Test
    @SneakyThrows
    void sendNotificationToIOUserKOResponseNull() {
        when(ioClientMock.submitMessage(anyString())).thenReturn(null);

        ErrorToNotifyException e = assertThrows(ErrorToNotifyException.class, () -> sut.sendNotificationToIOUser(any()));

        assertNotNull(e);
        assertNotNull(e.getMessage());
        assertEquals("IO /messages failed to respond", e.getMessage());
    }

    @Test
    @SneakyThrows
    void sendNotificationToIOUserKOResponseNot2xx() {
        when(notifyResponse.statusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        when(ioClientMock.submitMessage(anyString())).thenReturn(notifyResponse);

        ErrorToNotifyException e = assertThrows(ErrorToNotifyException.class, () -> sut.sendNotificationToIOUser(any()));

        assertNotNull(e);
        assertNotNull(e.getMessage());
        assertTrue(e.getMessage().startsWith("IO /messages responded with code"));
    }

    @Test
    @SneakyThrows
    void sendNotificationToIOUserKOFailToParseResponse() {
        when(notifyResponse.statusCode()).thenReturn(HttpStatus.SC_CREATED);
        when(notifyResponse.body()).thenReturn("][");
        when(ioClientMock.submitMessage(anyString())).thenReturn(notifyResponse);

        ErrorToNotifyException e = assertThrows(ErrorToNotifyException.class, () -> sut.sendNotificationToIOUser(any()));

        assertNotNull(e);
        assertNotNull(e.getMessage());
        assertEquals("Failed to deserialize response of IO API invocation", e.getMessage());
    }
}