package it.gov.pagopa.receipt.pdf.notifier.model.io.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class that hold the IO message payment data
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentData {

    @JsonProperty("amount")
    private Integer amount;

    @JsonProperty("notice_number")
    private String noticeNumber;

    @JsonProperty("invalid_after_due_date")
    private Boolean invalidAfterDueDate = false;

    @JsonProperty("payee")
    private Payee payee;
}
