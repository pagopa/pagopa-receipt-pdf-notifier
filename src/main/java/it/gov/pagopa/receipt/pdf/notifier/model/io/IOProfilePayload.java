package it.gov.pagopa.receipt.pdf.notifier.model.io;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class that hold the payload for getProfiles IO API
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IOProfilePayload {

    @JsonProperty("fiscal_code")
    private String fiscalCode;
}
