package it.gov.pagopa.receipt.pdf.notifier.client;

import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;

import java.net.http.HttpResponse;

public interface PDVTokenizerClient {
    HttpResponse<String> searchTokenByPII(String piiBody) throws PDVTokenizerException;

    HttpResponse<String> findPIIByToken(String token) throws PDVTokenizerException;

    HttpResponse<String> createToken(String piiBody) throws PDVTokenizerException;
}
