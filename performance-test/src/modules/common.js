
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
		"id": id
	}
    return receipt
}
