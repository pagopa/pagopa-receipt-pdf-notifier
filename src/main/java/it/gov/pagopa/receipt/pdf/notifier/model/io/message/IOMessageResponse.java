package it.gov.pagopa.receipt.pdf.notifier.model.io.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class that hold the response of submit message IO API
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IOMessageResponse {

    @JsonProperty("id")
    private String id;
}
