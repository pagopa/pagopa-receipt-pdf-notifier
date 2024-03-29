package it.gov.pagopa.receipt.pdf.notifier.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.ReasonError;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReasonErrorCode;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
import org.apache.http.HttpStatus;

public class ReceiptToIOUtils {

    public static ReasonError buildReasonError(String errorMessage, int code) {
        return ReasonError.builder()
                .code(code)
                .message(errorMessage)
                .build();
    }

    public static int getCodeOrDefault(Exception e) {
        if (e instanceof PDVTokenizerException pdvTokenizerException) {
            return pdvTokenizerException.getStatusCode();
        }
        if (e instanceof JsonProcessingException) {
            return ReasonErrorCode.ERROR_PDV_MAPPING.getCode();
        }
        return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }

}
