package it.gov.pagopa.receipt.pdf.notifier.model.tokenizer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class that hold Personal Identifiable Information
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PiiResource {

    private String pii;
}
