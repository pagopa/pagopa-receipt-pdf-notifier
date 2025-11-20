package it.gov.pagopa.receipt.pdf.notifier.entity.cart;

import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.IOMessageData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.ReasonError;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.ReceiptMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartPayment {
    private String bizEventId;
    private String subject;
    private String payeeName;
    private String debtorFiscalCode;
    private String amount;
    private ReceiptMetadata mdAttach;
    private IOMessageData idMessageDebtor;
    private ReasonError reasonErrDebtor;

}
