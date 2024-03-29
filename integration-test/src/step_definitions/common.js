function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function createReceipt(id) {
    let receipt = 
	{
		"eventId": id,
		"eventData": {
			"payerFiscalCode": "cd07268c-73e8-4df4-8305-a35085e32eff",
			"debtorFiscalCode": "cd07268c-73e8-4df4-8305-a35085e32eff",
			"amount": "200",
			"cart": [
				{
					"payeeName": "Comune di Milano",
					"subject": "ACI"
				}
			]
		},
		"status": "GENERATED",
		"numRetry": 0,
		"mdAttach": {
		    "name": "attachment_1.pdf",
		    "url": "aurl"
		},
		"id": id,
		"_rid": "Z9AJAMdamqNjAAAAAAAAAA==",
		"_self": "dbs/Z9AJAA==/colls/Z9AJAMdamqM=/docs/Z9AJAMdamqNjAAAAAAAAAA==/",
		"_etag": "\"08007a84-0000-0d00-0000-648b1e510000\"",
		"_attachments": "attachments/",
		"_ts": 1686838865
	}
    return receipt
}

function createReceiptForError(id) {
    let receiptForError = createReceipt(id);
    receiptForError.status = "IO_ERROR_TO_NOTIFY";
    return receiptForError;
}

module.exports = {
    sleep, createReceipt, createReceiptForError
}