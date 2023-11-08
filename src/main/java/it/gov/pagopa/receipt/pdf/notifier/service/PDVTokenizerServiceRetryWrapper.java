package it.gov.pagopa.receipt.pdf.notifier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;

/**
 * Service that wrap the {@link PDVTokenizerService} for adding retry logic for tokenizer responses with 429 status code
 */
public interface PDVTokenizerServiceRetryWrapper {

    /**
     * Call {@link PDVTokenizerService#getToken(String)} with retry on failure
     *
     * @param fiscalCode the fiscal code
     * @return the token associated to the fiscal code
     * @throws JsonProcessingException if an error occur when parsing input or output
     * @throws PDVTokenizerException if an error occur when invoking the PDV Tokenizer
     */
    String getTokenWithRetry(String fiscalCode) throws JsonProcessingException, PDVTokenizerException;

    /**
     * Call {@link PDVTokenizerService#getFiscalCode(String)} with retry on failure
     *
     * @param token the token
     * @return the fiscal code associated to the provided token
     * @throws JsonProcessingException if an error occur when parsing input or output
     * @throws PDVTokenizerException if an error occur when invoking the PDV Tokenizer
     */
    String getFiscalCodeWithRetry(String token) throws PDVTokenizerException, JsonProcessingException;

    /**
     * Call {@link PDVTokenizerService#generateTokenForFiscalCode(String)} with retry on failure
     *
     * @param fiscalCode the fiscal code
     * @return the generated token
     * @throws JsonProcessingException if an error occur when parsing input or output
     * @throws PDVTokenizerException if an error occur when invoking the PDV Tokenizer
     */
    String generateTokenForFiscalCodeWithRetry(String fiscalCode) throws PDVTokenizerException, JsonProcessingException;
}
