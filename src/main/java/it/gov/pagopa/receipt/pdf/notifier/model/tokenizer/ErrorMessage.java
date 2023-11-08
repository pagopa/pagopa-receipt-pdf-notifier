package it.gov.pagopa.receipt.pdf.notifier.model.tokenizer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class for the error response of the PDV Tokenizer for status 403, 404, 429
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorMessage {

    private String message;
}
