package it.gov.pagopa.receipt.pdf.notifier.entity.receipt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReasonError {
    private int code;
    private String message;

}
