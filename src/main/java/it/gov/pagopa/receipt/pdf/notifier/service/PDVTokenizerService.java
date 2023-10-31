package it.gov.pagopa.receipt.pdf.notifier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.client.PDVTokenizerClient;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;

/**
 * Service that handle the input and output for the {@link PDVTokenizerClient}
 */
public interface PDVTokenizerService {

    /**
     * Search the token associated to the specified fiscal code by calling {@link PDVTokenizerClient#searchTokenByPII(String)}
     *
     * @param fiscalCode the fiscal code
     * @return the token associated to the fiscal code
     * @throws JsonProcessingException if an error occur when parsing input or output
     * @throws PDVTokenizerException if an error occur when invoking the PDV Tokenizer
     */
    String getToken(String fiscalCode) throws JsonProcessingException, PDVTokenizerException;

    /**
     * Search the fiscal code associated to the specified token by calling {@link PDVTokenizerClient#findPIIByToken(String)}
     *
     * @param token the token
     * @return the fiscal code associated to the provided token
     * @throws JsonProcessingException if an error occur when parsing input or output
     * @throws PDVTokenizerException if an error occur when invoking the PDV Tokenizer
     */
    String getFiscalCode(String token) throws PDVTokenizerException, JsonProcessingException;

    /**
     * Generate a token for the specified fiscal code by calling {@link PDVTokenizerClient#createToken(String)}
     *
     * @param fiscalCode the fiscal code
     * @return the generated token
     * @throws JsonProcessingException if an error occur when parsing input or output
     * @throws PDVTokenizerException if an error occur when invoking the PDV Tokenizer
     */
    String generateTokenForFiscalCode(String fiscalCode) throws PDVTokenizerException, JsonProcessingException;
}
