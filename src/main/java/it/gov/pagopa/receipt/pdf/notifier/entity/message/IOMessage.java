package it.gov.pagopa.receipt.pdf.notifier.entity.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IOMessage {

    String messageId;
    String eventId;
}
