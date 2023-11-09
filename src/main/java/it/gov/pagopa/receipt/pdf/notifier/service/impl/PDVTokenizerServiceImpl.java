package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.client.PDVTokenizerClient;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.PDVTokenizerClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
import it.gov.pagopa.receipt.pdf.notifier.model.tokenizer.ErrorMessage;
import it.gov.pagopa.receipt.pdf.notifier.model.tokenizer.ErrorResponse;
import it.gov.pagopa.receipt.pdf.notifier.model.tokenizer.PiiResource;
import it.gov.pagopa.receipt.pdf.notifier.model.tokenizer.TokenResource;
import it.gov.pagopa.receipt.pdf.notifier.service.PDVTokenizerService;
import it.gov.pagopa.receipt.pdf.notifier.utils.ObjectMapperUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;

/**
 * {@inheritDoc}
 */
public class PDVTokenizerServiceImpl implements PDVTokenizerService {

    private final Logger logger = LoggerFactory.getLogger(PDVTokenizerServiceImpl.class);

    private final PDVTokenizerClient pdvTokenizerClient;

    public PDVTokenizerServiceImpl() {
        this.pdvTokenizerClient = PDVTokenizerClientImpl.getInstance();
    }

    PDVTokenizerServiceImpl(PDVTokenizerClient pdvTokenizerClient) {
        this.pdvTokenizerClient = pdvTokenizerClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToken(String fiscalCode) throws JsonProcessingException, PDVTokenizerException {
        logger.debug("PDV Tokenizer getToken called");
        PiiResource piiResource = PiiResource.builder().pii(fiscalCode).build();
        String tokenizerBody = ObjectMapperUtils.writeValueAsString(piiResource);

        HttpResponse<String> httpResponse = pdvTokenizerClient.searchTokenByPII(tokenizerBody);

        handleErrorResponse(httpResponse, "getToken");

        TokenResource tokenResource = ObjectMapperUtils.mapString(httpResponse.body(), TokenResource.class);
        logger.debug("PDV Tokenizer getToken invocation completed");
        return tokenResource.getToken();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFiscalCode(String token) throws PDVTokenizerException, JsonProcessingException {
        logger.debug("PDV Tokenizer getFiscalCode called");
        HttpResponse<String> httpResponse = pdvTokenizerClient.findPIIByToken(token);

        handleErrorResponse(httpResponse, "getFiscalCode");

        PiiResource piiResource = ObjectMapperUtils.mapString(httpResponse.body(), PiiResource.class);
        logger.debug("PDV Tokenizer getFiscalCode invocation completed");
        return piiResource.getPii();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateTokenForFiscalCode(String fiscalCode) throws PDVTokenizerException, JsonProcessingException {
        logger.debug("PDV Tokenizer generateTokenForFiscalCode called");
        PiiResource piiResource = PiiResource.builder().pii(fiscalCode).build();
        String tokenizerBody = ObjectMapperUtils.writeValueAsString(piiResource);

        HttpResponse<String> httpResponse = pdvTokenizerClient.createToken(tokenizerBody);

        if (httpResponse.statusCode() == HttpStatus.SC_BAD_REQUEST
                || httpResponse.statusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            ErrorResponse response = ObjectMapperUtils.mapString(httpResponse.body(), ErrorResponse.class);
            String errMsg = String.format("PDV Tokenizer generateTokenForFiscalCode invocation failed with status %s and message: %s. Error description: %s (%s)",
                    response.getStatus(), response.getTitle(), response.getDetail(), response.getType());
            throw new PDVTokenizerException(errMsg, response.getStatus());
        }
        if (httpResponse.statusCode() != HttpStatus.SC_OK) {
            ErrorMessage response = ObjectMapperUtils.mapString(httpResponse.body(), ErrorMessage.class);
            String errMsg = String.format("PDV Tokenizer generateTokenForFiscalCode invocation failed with status %s and message: %s.",
                    httpResponse.statusCode(), response.getMessage());
            throw new PDVTokenizerException(errMsg, httpResponse.statusCode());
        }
        TokenResource tokenResource = ObjectMapperUtils.mapString(httpResponse.body(), TokenResource.class);
        logger.debug("PDV Tokenizer generateTokenForFiscalCode invocation completed");
        return tokenResource.getToken();
    }


    private void handleErrorResponse(HttpResponse<String> httpResponse, String serviceName) throws JsonProcessingException, PDVTokenizerException {
        if (httpResponse.statusCode() == HttpStatus.SC_BAD_REQUEST
                || httpResponse.statusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR
                || httpResponse.statusCode() == HttpStatus.SC_NOT_FOUND) {
            ErrorResponse response = ObjectMapperUtils.mapString(httpResponse.body(), ErrorResponse.class);
            String errMsg = String.format("PDV Tokenizer %s invocation failed with status %s and message: %s. Error description: %s (%s)",
                    serviceName, response.getStatus(), response.getTitle(), response.getDetail(), response.getType());
            throw new PDVTokenizerException(errMsg, response.getStatus());
        }
        if (httpResponse.statusCode() != HttpStatus.SC_OK) {
            ErrorMessage response = ObjectMapperUtils.mapString(httpResponse.body(), ErrorMessage.class);
            String errMsg = String.format("PDV Tokenizer %s invocation failed with status %s and message: %s.",
                    serviceName, httpResponse.statusCode(), response.getMessage());
            throw new PDVTokenizerException(errMsg, httpResponse.statusCode());
        }
    }
}
