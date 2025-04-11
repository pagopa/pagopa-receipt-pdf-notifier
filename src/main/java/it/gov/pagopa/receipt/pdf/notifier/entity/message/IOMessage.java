package it.gov.pagopa.receipt.pdf.notifier.entity.message;

import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IOMessage {
    String id;
    String messageId;
    String eventId;
    UserType userType;
}
