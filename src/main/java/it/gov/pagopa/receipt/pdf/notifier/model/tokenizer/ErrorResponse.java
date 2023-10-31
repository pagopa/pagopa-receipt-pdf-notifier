package it.gov.pagopa.receipt.pdf.notifier.model.tokenizer;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Model class for the error response of the PDV Tokenizer
 */
@Data
@Builder
public class ErrorResponse {

    private String detail;
    private String instance;
    private List<InvalidParam> invalidParams;
    private int status;
    private String title;
    private String type;

}
