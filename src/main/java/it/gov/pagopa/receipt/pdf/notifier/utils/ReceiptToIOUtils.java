package it.gov.pagopa.receipt.pdf.notifier.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.ReasonError;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReasonErrorCode;
import it.gov.pagopa.receipt.pdf.notifier.exception.IOAPIException;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
import org.apache.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ReceiptToIOUtils {

    public static final String ANONIMO = "ANONIMO";

    private static final Pattern FISCAL_CODE_PATTERN =
            Pattern.compile("^[A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$");
    private static final List<String> CF_FILTER_NOTIFIER = Arrays.asList(System.getenv().getOrDefault("CF_FILTER_NOTIFIER", "").split(","));

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
        if (e instanceof IOAPIException ioApiException) {
            return ioApiException.getStatusCode();
        }
        if (e instanceof JsonProcessingException) {
            return ReasonErrorCode.ERROR_PDV_MAPPING.getCode();
        }
        return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }

    public static boolean isFiscalCodeValid(String fiscalCode) {
        return fiscalCode != null
                && !fiscalCode.isEmpty()
                && FISCAL_CODE_PATTERN.matcher(fiscalCode).matches()
                && (CF_FILTER_NOTIFIER.contains("*") || CF_FILTER_NOTIFIER.contains(fiscalCode));
    }

    private ReceiptToIOUtils() {
    }
}
