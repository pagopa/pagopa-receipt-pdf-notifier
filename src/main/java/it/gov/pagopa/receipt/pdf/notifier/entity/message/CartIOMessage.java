package it.gov.pagopa.receipt.pdf.notifier.entity.message;

import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartIOMessage {
    String id;
    String messageId;
    String cartId;
    String eventId;
    UserType userType;
}
