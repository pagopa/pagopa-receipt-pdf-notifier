function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function createReceipt(id) {
    let receipt = 
	{
		"eventId": id,
		"eventData": {
			"payerFiscalCode": "AAAAAA00A00A000A",
			"debtorFiscalCode": "AAAAAA00A00A000A"
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

module.exports = {
    sleep, createReceipt
}