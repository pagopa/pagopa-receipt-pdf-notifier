package it.gov.pagopa.receipt.pdf.notifier.entity.receipt;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class EventData {
    private String payerFiscalCode;
    private String debtorFiscalCode;
    private String transactionCreationDate;
    private String amount;
    private List<CartItem> cart;
}
