package it.gov.pagopa.receipt.pdf.notifier.entity.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageData {

    private String id;
    private String subject;
    private String markdown;
}
