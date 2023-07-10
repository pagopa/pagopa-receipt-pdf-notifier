package it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration;

public enum ReceiptStatusType {
    NOT_QUEUE_SENT, INSERTED, RETRY, GENERATED, SIGNED, FAILED, IO_NOTIFIED, IO_ERROR_TO_NOTIFY, IO_NOTIFIER_RETRY, UNABLE_TO_SEND, NOT_TO_NOTIFY
}
