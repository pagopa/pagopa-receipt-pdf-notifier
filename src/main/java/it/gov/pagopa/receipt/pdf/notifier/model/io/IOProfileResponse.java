package it.gov.pagopa.receipt.pdf.notifier.model.io;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Model class that hold the response for getProfiles IO API
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IOProfileResponse {

    @JsonProperty("sender_allowed")
    private boolean senderAllowed;

    @JsonProperty("preferred_languages")
    private List<String> preferredLanguages;
}
