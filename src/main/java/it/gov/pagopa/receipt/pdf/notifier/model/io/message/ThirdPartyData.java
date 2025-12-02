package it.gov.pagopa.receipt.pdf.notifier.model.io.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class that hold the IO message third party data
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThirdPartyData {

    @JsonProperty("id")
    private String id;

    @JsonProperty("has_attachments")
    private Boolean hasAttachments;

    @JsonProperty("has_remote_content")
    private Boolean hasRemoteContent;

    @JsonProperty("configuration_id")
    private String configurationId;
}
