package it.gov.pagopa.receipt.pdf.notifier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;

public interface PDVTokenizerService {

    String getToken(String fiscalCode) throws JsonProcessingException, PDVTokenizerException;

    String getFiscalCode(String token) throws PDVTokenizerException, JsonProcessingException;

    String generateTokenForFiscalCode(String fiscalCode) throws PDVTokenizerException, JsonProcessingException;
}
