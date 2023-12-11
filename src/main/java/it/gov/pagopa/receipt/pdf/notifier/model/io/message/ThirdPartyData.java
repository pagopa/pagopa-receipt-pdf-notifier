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

    @JsonProperty("original_sender")
    private String originalSender;

    @JsonProperty("original_receipt_date")
    private String originalReceiptDate;

    @JsonProperty("has_attachments")
    private Boolean hasAttachments = false;

    @JsonProperty("summary")
    private String summary;
}
