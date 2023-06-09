package it.gov.pagopa.receipt.pdf.notifier.entity.receipt;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReceiptMetadata {

    private String name;
    private String url;
}
