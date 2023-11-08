package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.receipt.pdf.notifier.client.PDVTokenizerClient;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
import it.gov.pagopa.receipt.pdf.notifier.model.tokenizer.ErrorMessage;
import it.gov.pagopa.receipt.pdf.notifier.model.tokenizer.ErrorResponse;
import it.gov.pagopa.receipt.pdf.notifier.model.tokenizer.InvalidParam;
import it.gov.pagopa.receipt.pdf.notifier.model.tokenizer.PiiResource;
import it.gov.pagopa.receipt.pdf.notifier.model.tokenizer.TokenResource;
import it.gov.pagopa.receipt.pdf.notifier.service.PDVTokenizerService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class PDVTokenizerServiceImplTest {

    private static final String TOKEN = "token";
    private static final String FISCAL_CODE = "fiscalCode";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpResponse<String> httpResponseMock;
    private PDVTokenizerClient pdvTokenizerClientMock;

    private PDVTokenizerService sut;

    @BeforeEach
    void setUp() {
        httpResponseMock = mock(HttpResponse.class);
        pdvTokenizerClientMock = mock(PDVTokenizerClient.class);
        sut = spy(new PDVTokenizerServiceImpl(pdvTokenizerClientMock));
    }

    @Test
    void getTokenSuccess() throws JsonProcessingException, PDVTokenizerException {
        TokenResource tokenResource = TokenResource.builder().token(TOKEN).build();
        String responseBody = objectMapper.writeValueAsString(tokenResource);

        doReturn(HttpStatus.SC_OK).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(pdvTokenizerClientMock).searchTokenByPII(anyString());

        String token = sut.getToken(FISCAL_CODE);

        assertNotNull(token);
        assertEquals(TOKEN, token);

        verify(pdvTokenizerClientMock).searchTokenByPII(anyString());
    }

    @Test
    void getFiscalCodeSuccess() throws JsonProcessingException, PDVTokenizerException {
        PiiResource piiResource = PiiResource.builder().pii(FISCAL_CODE).build();
        String responseBody = objectMapper.writeValueAsString(piiResource);

        doReturn(HttpStatus.SC_OK).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(pdvTokenizerClientMock).findPIIByToken(anyString());

        String fiscalCode = sut.getFiscalCode(TOKEN);

        assertNotNull(fiscalCode);
        assertEquals(FISCAL_CODE, fiscalCode);

        verify(pdvTokenizerClientMock).findPIIByToken(anyString());
    }

    @Test
    void generateTokenForFiscalCodeSuccess() throws JsonProcessingException, PDVTokenizerException {
        TokenResource tokenResource = TokenResource.builder().token(TOKEN).build();
        String responseBody = objectMapper.writeValueAsString(tokenResource);

        doReturn(HttpStatus.SC_OK).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(pdvTokenizerClientMock).createToken(anyString());

        String token = sut.generateTokenForFiscalCode(FISCAL_CODE);

        assertNotNull(token);
        assertEquals(TOKEN, token);

        verify(pdvTokenizerClientMock).createToken(anyString());
    }

    @Test
    void getTokenFailClientThrowsPDVTokenizerException() throws PDVTokenizerException {
        doThrow(PDVTokenizerException.class).when(pdvTokenizerClientMock).searchTokenByPII(anyString());

        assertThrows(PDVTokenizerException.class, () -> sut.getToken(FISCAL_CODE));

        verify(pdvTokenizerClientMock).searchTokenByPII(anyString());
    }

    @Test
    void getTokenFailResponse400() throws PDVTokenizerException, JsonProcessingException {
        ErrorResponse errorResponse = buildErrorResponse();
        String responseBody = objectMapper.writeValueAsString(errorResponse);

        doReturn(HttpStatus.SC_BAD_REQUEST).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(pdvTokenizerClientMock).searchTokenByPII(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.getToken(FISCAL_CODE));

        assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());

        verify(pdvTokenizerClientMock).searchTokenByPII(anyString());
    }

    @Test
    void getTokenFailResponse429() throws PDVTokenizerException, JsonProcessingException {
        ErrorMessage errorResponse = ErrorMessage.builder().message("Too Many Requests").build();
        String responseBody = objectMapper.writeValueAsString(errorResponse);

        doReturn(429).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(pdvTokenizerClientMock).searchTokenByPII(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.getToken(FISCAL_CODE));

        assertEquals(429, e.getStatusCode());

        verify(pdvTokenizerClientMock).searchTokenByPII(anyString());
    }

    @Test
    void getFiscalCodeFailClientThrowsPDVTokenizerException() throws PDVTokenizerException {
        doThrow(PDVTokenizerException.class).when(pdvTokenizerClientMock).findPIIByToken(anyString());

        assertThrows(PDVTokenizerException.class, () -> sut.getFiscalCode(TOKEN));

        verify(pdvTokenizerClientMock).findPIIByToken(anyString());
    }

    @Test
    void getFiscalCodeFailResponse400() throws PDVTokenizerException, JsonProcessingException {
        ErrorResponse errorResponse = buildErrorResponse();
        String responseBody = objectMapper.writeValueAsString(errorResponse);

        doReturn(HttpStatus.SC_BAD_REQUEST).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(pdvTokenizerClientMock).findPIIByToken(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.getFiscalCode(TOKEN));

        assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());

        verify(pdvTokenizerClientMock).findPIIByToken(anyString());
    }

    @Test
    void getFiscalCodeFailResponse429() throws PDVTokenizerException, JsonProcessingException {
        ErrorMessage errorResponse = ErrorMessage.builder().message("Too Many Requests").build();
        String responseBody = objectMapper.writeValueAsString(errorResponse);

        doReturn(429).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(pdvTokenizerClientMock).findPIIByToken(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.getFiscalCode(TOKEN));

        assertEquals(429, e.getStatusCode());

        verify(pdvTokenizerClientMock).findPIIByToken(anyString());
    }

    @Test
    void generateTokenForFiscalCodeFailClientThrowsPDVTokenizerException() throws PDVTokenizerException {
        doThrow(PDVTokenizerException.class).when(pdvTokenizerClientMock).createToken(anyString());

        assertThrows(PDVTokenizerException.class, () -> sut.generateTokenForFiscalCode(FISCAL_CODE));

        verify(pdvTokenizerClientMock).createToken(anyString());
    }

    @Test
    void generateTokenForFiscalCodeFailResponse400() throws PDVTokenizerException, JsonProcessingException {
        ErrorResponse errorResponse = buildErrorResponse();
        String responseBody = objectMapper.writeValueAsString(errorResponse);

        doReturn(HttpStatus.SC_BAD_REQUEST).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(pdvTokenizerClientMock).createToken(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.generateTokenForFiscalCode(FISCAL_CODE));

        assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());

        verify(pdvTokenizerClientMock).createToken(anyString());
    }

    @Test
    void generateTokenForFiscalCodeFailResponse429() throws PDVTokenizerException, JsonProcessingException {
        ErrorMessage errorResponse = ErrorMessage.builder().message("Too Many Requests").build();
        String responseBody = objectMapper.writeValueAsString(errorResponse);

        doReturn(429).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(pdvTokenizerClientMock).createToken(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.generateTokenForFiscalCode(FISCAL_CODE));

        assertEquals(429, e.getStatusCode());

        verify(pdvTokenizerClientMock).createToken(anyString());
    }

    private ErrorResponse buildErrorResponse() {
        return ErrorResponse.builder()
                .title("Error")
                .detail("Error detail")
                .status(HttpStatus.SC_BAD_REQUEST)
                .invalidParams(Collections.singletonList(InvalidParam.builder()
                        .name("param name")
                        .reason("reason")
                        .build()))
                .instance("instance")
                .type("type")
                .build();
    }
}