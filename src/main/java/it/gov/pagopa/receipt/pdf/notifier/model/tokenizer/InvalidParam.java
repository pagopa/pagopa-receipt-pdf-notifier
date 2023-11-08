package it.gov.pagopa.receipt.pdf.notifier.model.tokenizer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class for the details of invalid param error
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvalidParam {

    private String name;
    private String reason;
}
