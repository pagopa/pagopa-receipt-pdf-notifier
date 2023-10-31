package it.gov.pagopa.receipt.pdf.notifier.client;

import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
import it.gov.pagopa.receipt.pdf.notifier.model.tokenizer.PiiResource;

import java.net.http.HttpResponse;

/**
 * Client for invoking PDV Tokenizer service
 */
public interface PDVTokenizerClient {

    /**
     * Search the token associated to the specified PII
     *
     * @param piiBody the {@link PiiResource} serialized as String
     * @return the {@link HttpResponse} of the PDV Tokenizer service
     * @throws PDVTokenizerException if an error occur when invoking the PDV Tokenizer service
     */
    HttpResponse<String> searchTokenByPII(String piiBody) throws PDVTokenizerException;

    /**
     * Find the PII associated to the specified token
     *
     * @param token the token
     * @return the {@link HttpResponse} of the PDV Tokenizer service
     * @throws PDVTokenizerException if an error occur when invoking the PDV Tokenizer service
     */
    HttpResponse<String> findPIIByToken(String token) throws PDVTokenizerException;

    /**
     * Create a new token for the specified PII
     *
     * @param piiBody the {@link PiiResource} serialized as String
     * @return the {@link HttpResponse} of the PDV Tokenizer service
     * @throws PDVTokenizerException if an error occur when invoking the PDV Tokenizer service
     */
    HttpResponse<String> createToken(String piiBody) throws PDVTokenizerException;
}
