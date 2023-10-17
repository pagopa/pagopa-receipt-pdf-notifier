package it.gov.pagopa.receipt.pdf.notifier.entity.message;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IOMessage {

    String messageId;
    String eventId;
}
