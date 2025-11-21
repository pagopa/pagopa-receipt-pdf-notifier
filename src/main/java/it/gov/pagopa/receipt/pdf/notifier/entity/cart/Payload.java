package it.gov.pagopa.receipt.pdf.notifier.entity.cart;

import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.ReasonError;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.ReceiptMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payload {
    private String payerFiscalCode;
    private String transactionCreationDate;
    private int totalNotice;
    private String totalAmount;
    private ReceiptMetadata mdAttachPayer;
    private String idMessagePayer;
    private List<CartPayment> cart;
    private ReasonError reasonErrPayer;

}
