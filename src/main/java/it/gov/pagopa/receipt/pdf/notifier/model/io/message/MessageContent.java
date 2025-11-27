package it.gov.pagopa.receipt.pdf.notifier.model.io.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class that hold the IO message content
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageContent {

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("markdown")
    private String markdown;

    @JsonProperty("third_party_data")
    private ThirdPartyData thirdPartyData;
}
