package it.gov.pagopa.receipt.pdf.notifier.model.tokenizer;

import lombok.Builder;
import lombok.Data;

/**
 * Model class that hold Personal Identifiable Information
 */
@Data
@Builder
public class PiiResource {

    private String pii;
}
