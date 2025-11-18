package it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration;

public enum ReasonErrorCode {
    ERROR_PDV_IO(800),
    ERROR_PDV_UNEXPECTED(801),
    ERROR_PDV_MAPPING(802),
    ERROR_IO_API_IO(803),
    ERROR_IO_API_UNEXPECTED(804);

    private final int code;

    ReasonErrorCode(int code){
        this.code = code;
    }

    public int getCode(){
        return this.code;
    }
}
