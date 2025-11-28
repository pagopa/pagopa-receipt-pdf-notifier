package it.gov.pagopa.receipt.pdf.notifier.model;

import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotifyCartResult {

    private NotifyUserResult payerNotifyResult;
    private Map<String, NotifyUserResult> debtorNotifyResultMap;

    public void addDebtorNotifyStatusToMap(String debtorFiscalCode, NotifyUserResult userNotifyResult) {
        if (this.debtorNotifyResultMap == null) {
            this.debtorNotifyResultMap = new HashMap<>();
        }
        this.debtorNotifyResultMap.put(debtorFiscalCode, userNotifyResult);
    }
}
