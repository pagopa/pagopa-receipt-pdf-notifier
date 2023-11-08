package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerUnexpectedException;
import it.gov.pagopa.receipt.pdf.notifier.service.PDVTokenizerService;
import it.gov.pagopa.receipt.pdf.notifier.service.PDVTokenizerServiceRetryWrapper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PDVTokenizerServiceRetryWrapperImplTest {

    private static final String FISCAL_CODE = "fiscalCode";
    private static final String TOKEN = "token";
    private static final int MAX_ATTEMPTS = 3;

    private PDVTokenizerService pdvTokenizerServiceMock;

    private PDVTokenizerServiceRetryWrapper sut;

    @BeforeEach
    void setUp() {
        pdvTokenizerServiceMock = mock(PDVTokenizerService.class);

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(MAX_ATTEMPTS)
                .retryOnException(e -> (e instanceof PDVTokenizerException tokenizerException) && tokenizerException.getStatusCode() == 429)
                .build();
        Retry retry = Retry.of("id", config);

        sut = spy(new PDVTokenizerServiceRetryWrapperImpl(pdvTokenizerServiceMock, retry));
    }

    @Test
    void getTokenRetryForPDVTokenizerExceptionWithStatus429() throws PDVTokenizerException, JsonProcessingException {
        String errMsg = "Error";
        doThrow(new PDVTokenizerException(errMsg, 429)).when(pdvTokenizerServiceMock).getToken(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.getTokenWithRetry(FISCAL_CODE));

        assertNotNull(e);
        assertEquals(429, e.getStatusCode());
        assertEquals(errMsg, e.getMessage());

        verify(pdvTokenizerServiceMock, times(MAX_ATTEMPTS)).getToken(anyString());
    }

    @Test
    void getTokenNotRetryForPDVTokenizerExceptionWithoutStatus429() throws PDVTokenizerException, JsonProcessingException {
        String errMsg = "Error";
        doThrow(new PDVTokenizerException(errMsg, HttpStatus.SC_INTERNAL_SERVER_ERROR)).when(pdvTokenizerServiceMock).getToken(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.getTokenWithRetry(FISCAL_CODE));

        assertNotNull(e);
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getStatusCode());
        assertEquals(errMsg, e.getMessage());

        verify(pdvTokenizerServiceMock).getToken(anyString());
    }

    @Test
    void getTokenNotRetryForJsonProcessingException() throws PDVTokenizerException, JsonProcessingException {
        doThrow(JsonProcessingException.class).when(pdvTokenizerServiceMock).getToken(anyString());

        JsonProcessingException e = assertThrows(JsonProcessingException.class, () -> sut.getTokenWithRetry(FISCAL_CODE));

        assertNotNull(e);
        verify(pdvTokenizerServiceMock).getToken(anyString());
    }

    @Test
    void getTokenNotRetryForPDVTokenizerUnexpectedException() throws PDVTokenizerException, JsonProcessingException {
        doThrow(RuntimeException.class).when(pdvTokenizerServiceMock).getToken(anyString());

        PDVTokenizerUnexpectedException e = assertThrows(PDVTokenizerUnexpectedException.class, () -> sut.getTokenWithRetry(FISCAL_CODE));

        assertNotNull(e);
        verify(pdvTokenizerServiceMock).getToken(anyString());
    }

    @Test
    void getTokenSuccessNotRetry() throws PDVTokenizerException, JsonProcessingException {
        doReturn(TOKEN).when(pdvTokenizerServiceMock).getToken(anyString());

        String token = sut.getTokenWithRetry(FISCAL_CODE);

        assertEquals(TOKEN, token);
        verify(pdvTokenizerServiceMock).getToken(anyString());
    }

    @Test
    void getFiscalCodeRetryForPDVTokenizerExceptionWithStatus429() throws PDVTokenizerException, JsonProcessingException {
        String errMsg = "Error";
        doThrow(new PDVTokenizerException(errMsg, 429)).when(pdvTokenizerServiceMock).getFiscalCode(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.getFiscalCodeWithRetry(TOKEN));

        assertNotNull(e);
        assertEquals(429, e.getStatusCode());
        assertEquals(errMsg, e.getMessage());

        verify(pdvTokenizerServiceMock, times(MAX_ATTEMPTS)).getFiscalCode(anyString());
    }

    @Test
    void getFiscalCodeNotRetryForPDVTokenizerExceptionWithoutStatus429() throws PDVTokenizerException, JsonProcessingException {
        String errMsg = "Error";
        doThrow(new PDVTokenizerException(errMsg, HttpStatus.SC_INTERNAL_SERVER_ERROR)).when(pdvTokenizerServiceMock).getFiscalCode(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.getFiscalCodeWithRetry(TOKEN));

        assertNotNull(e);
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getStatusCode());
        assertEquals(errMsg, e.getMessage());

        verify(pdvTokenizerServiceMock).getFiscalCode(anyString());
    }

    @Test
    void getFiscalCodeNotRetryForJsonProcessingException() throws PDVTokenizerException, JsonProcessingException {
        doThrow(JsonProcessingException.class).when(pdvTokenizerServiceMock).getFiscalCode(anyString());

        JsonProcessingException e = assertThrows(JsonProcessingException.class, () -> sut.getFiscalCodeWithRetry(TOKEN));

        assertNotNull(e);
        verify(pdvTokenizerServiceMock).getFiscalCode(anyString());
    }

    @Test
    void getFiscalCodeNotRetryForPDVTokenizerUnexpectedException() throws PDVTokenizerException, JsonProcessingException {
        doThrow(RuntimeException.class).when(pdvTokenizerServiceMock).getFiscalCode(anyString());

        PDVTokenizerUnexpectedException e = assertThrows(PDVTokenizerUnexpectedException.class, () -> sut.getFiscalCodeWithRetry(TOKEN));

        assertNotNull(e);
        verify(pdvTokenizerServiceMock).getFiscalCode(anyString());
    }

    @Test
    void getFiscalCodeSuccessNotRetry() throws PDVTokenizerException, JsonProcessingException {
        doReturn(FISCAL_CODE).when(pdvTokenizerServiceMock).getFiscalCode(anyString());

        String token = sut.getFiscalCodeWithRetry(TOKEN);

        assertEquals(FISCAL_CODE, token);
        verify(pdvTokenizerServiceMock).getFiscalCode(anyString());
    }

    @Test
    void generateTokenForFiscalCodeRetryForPDVTokenizerExceptionWithStatus429() throws PDVTokenizerException, JsonProcessingException {
        String errMsg = "Error";
        doThrow(new PDVTokenizerException(errMsg, 429)).when(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.generateTokenForFiscalCodeWithRetry(FISCAL_CODE));

        assertNotNull(e);
        assertEquals(429, e.getStatusCode());
        assertEquals(errMsg, e.getMessage());

        verify(pdvTokenizerServiceMock, times(MAX_ATTEMPTS)).generateTokenForFiscalCode(anyString());
    }

    @Test
    void generateTokenForFiscalCodeNotRetryForPDVTokenizerExceptionWithoutStatus429() throws PDVTokenizerException, JsonProcessingException {
        String errMsg = "Error";
        doThrow(new PDVTokenizerException(errMsg, HttpStatus.SC_INTERNAL_SERVER_ERROR)).when(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.generateTokenForFiscalCodeWithRetry(FISCAL_CODE));

        assertNotNull(e);
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getStatusCode());
        assertEquals(errMsg, e.getMessage());

        verify(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());
    }

    @Test
    void generateTokenForFiscalCodeNotRetryForJsonProcessingException() throws PDVTokenizerException, JsonProcessingException {
        doThrow(JsonProcessingException.class).when(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());

        JsonProcessingException e = assertThrows(JsonProcessingException.class, () -> sut.generateTokenForFiscalCodeWithRetry(FISCAL_CODE));

        assertNotNull(e);
        verify(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());
    }

    @Test
    void generateTokenForFiscalCodeNotRetryForPDVTokenizerUnexpectedException() throws PDVTokenizerException, JsonProcessingException {
        doThrow(RuntimeException.class).when(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());

        PDVTokenizerUnexpectedException e = assertThrows(PDVTokenizerUnexpectedException.class, () -> sut.generateTokenForFiscalCodeWithRetry(FISCAL_CODE));

        assertNotNull(e);
        verify(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());
    }

    @Test
    void generateTokenForFiscalCodeSuccessNotRetry() throws PDVTokenizerException, JsonProcessingException {
        doReturn(TOKEN).when(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());

        String token = sut.generateTokenForFiscalCodeWithRetry(FISCAL_CODE);

        assertEquals(TOKEN, token);
        verify(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());
    }
}