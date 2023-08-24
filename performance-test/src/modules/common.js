
export function randomString(length, charset) {
    let res = '';
    while (length--) res += charset[(Math.random() * charset.length) | 0];
    return res;
}

export function createReceipt(id) {
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
		"id": id
	}
    return receipt
}
