package it.gov.pagopa.receipt.pdf.notifier.entity.receipt;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReasonError {
    private int code;
    private String message;

}
