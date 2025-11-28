package it.gov.pagopa.receipt.pdf.notifier.model;

import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
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
public class NotifyUserResult {

    private UserNotifyStatus notifyStatus;
    private String messageId;
}
