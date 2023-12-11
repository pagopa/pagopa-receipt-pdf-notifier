package it.gov.pagopa.receipt.pdf.notifier.model.io.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class that hold the payload for submitting a message IO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessagePayload {

    @JsonProperty("time_to_live")
    private Integer timeToLive = 3600;

    @JsonProperty("content")
    private MessageContent content;

    @JsonProperty("fiscal_code")
    private String fiscalCode;

    @JsonProperty("feature_level_type")
    private String featureLevelType = "STANDARD";
}
