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
    private String id;
    private String messageId;
    private String cartId;
    private String eventId;
    private UserType userType;
    private String subject;
    private String markdown;
}
