package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.client.PDVTokenizerClient;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.PDVTokenizerClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
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

        if (httpResponse.statusCode() != HttpStatus.SC_OK) {
            ErrorResponse errorResponse = ObjectMapperUtils.mapString(httpResponse.body(), ErrorResponse.class);
            String errMsg = String.format("PDV Tokenizer getToken invocation failed: %s. Error description: %s (%s)",
                    errorResponse.getTitle(), errorResponse.getDetail(), errorResponse.getType());
            throw new PDVTokenizerException(errMsg, errorResponse.getStatus());
        }
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

        if (httpResponse.statusCode() != HttpStatus.SC_OK) {
            ErrorResponse errorResponse = ObjectMapperUtils.mapString(httpResponse.body(), ErrorResponse.class);
            String errMsg = String.format("PDV Tokenizer getFiscalCode invocation failed: %s. Error description: %s (%s)",
                    errorResponse.getTitle(), errorResponse.getDetail(), errorResponse.getType());
            throw new PDVTokenizerException(errMsg, errorResponse.getStatus());
        }
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

        if (httpResponse.statusCode() != HttpStatus.SC_OK) {
            ErrorResponse errorResponse = ObjectMapperUtils.mapString(httpResponse.body(), ErrorResponse.class);
            String errMsg = String.format("PDV Tokenizer generateTokenForFiscalCode invocation failed: %s. Error description: %s (%s)",
                    errorResponse.getTitle(), errorResponse.getDetail(), errorResponse.getType());
            throw new PDVTokenizerException(errMsg, errorResponse.getStatus());
        }
        TokenResource tokenResource = ObjectMapperUtils.mapString(httpResponse.body(), TokenResource.class);
        logger.debug("PDV Tokenizer generateTokenForFiscalCode invocation completed");
        return tokenResource.getToken();
    }
}
