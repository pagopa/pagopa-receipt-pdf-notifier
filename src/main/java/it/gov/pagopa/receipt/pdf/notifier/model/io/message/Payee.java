package it.gov.pagopa.receipt.pdf.notifier.model.io.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class that hold the IO message payment data payee
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payee {

    @JsonProperty("fiscal_code")
    private String fiscalCode;
}
