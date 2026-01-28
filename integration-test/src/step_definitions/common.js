function sleep(ms) {
	return new Promise(resolve => setTimeout(resolve, ms));
}

function createReceipt(id, fiscalCodeToken) {
	let receipt =
	{
		"eventId": id,
		"eventData": {
			"payerFiscalCode": fiscalCodeToken,
			"debtorFiscalCode": fiscalCodeToken,
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
		"id": id
	}
	return receipt
}

function createCartReceipt(id, fiscalCodeToken) {
	return {
		"cartId": id,
		"id": id,
		"version": "1",
		"payload": {
			"payerFiscalCode": fiscalCodeToken,
			"transactionCreationDate": "2025-11-27T15:22:45.227227227Z",
			"totalNotice": 2,
			"totalAmount": "26,48",
			"mdAttachPayer": {
				"name": "pagopa-ricevuta-251127-test-ricevute-carrello-295f85fa-23a5-4bd1-bf1c-f20c4963a925-0-p-c.pdf",
				"url": "https://pagopauweureceiptsfnsa.blob.core.windows.net/pagopa-u-weu-receipts-azure-blob-receipt-st-attach/pagopa-ricevuta-251127-test-ricevute-carrello-295f85fa-23a5-4bd1-bf1c-f20c4963a925-0-p-c.pdf"
			},
			"cart": [
				{
					"bizEventId": "doc-test-ricevute-5606a4ef-61e7-48e2-ac1b-13581fd47f48-0-0-0",
					"subject": "N004",
					"payeeName": "Ministero delle infrastrutture e dei trasporti",
					"debtorFiscalCode": fiscalCodeToken,
					"amount": "16.0"
				},
				{
					"bizEventId": "doc-test-ricevute-5606a4ef-61e7-48e2-ac1b-13581fd47f48-0-0-1",
					"subject": "Pagamento multa 1",
					"payeeName": "Ministero delle infrastrutture e dei trasporti",
					"debtorFiscalCode": fiscalCodeToken,
					"amount": "10.2",
					"mdAttach": {
						"name": "pagopa-ricevuta-251127-doc-test-ricevute-5606a4ef-61e7-48e2-ac1b-13581fd47f48-0-0-1-d-c.pdf",
						"url": "https://pagopauweureceiptsfnsa.blob.core.windows.net/pagopa-u-weu-receipts-azure-blob-receipt-st-attach/pagopa-ricevuta-251127-doc-test-ricevute-5606a4ef-61e7-48e2-ac1b-13581fd47f48-0-0-1-d-c.pdf"
					}
				}
			]
		},
		"status": "GENERATED",
		"numRetry": 0,
		"notificationNumRetry": 1,
		"inserted_at": 1764256965525,
		"generated_at": 1764256969888,
		"notified_at": 0
	}
}

function createReceiptForError(id, fiscalCodeToken) {
	let receiptForError = createReceipt(id, fiscalCodeToken);
	receiptForError.status = "IO_ERROR_TO_NOTIFY";
	return receiptForError;
}

function createCartReceiptForError(id, fiscalCodeToken) {
	let receiptForError = createCartReceipt(id, fiscalCodeToken);
	receiptForError.status = "IO_ERROR_TO_NOTIFY";
	return receiptForError;
}

module.exports = {
	sleep, createReceipt, createReceiptForError, createCartReceipt, createCartReceiptForError
}