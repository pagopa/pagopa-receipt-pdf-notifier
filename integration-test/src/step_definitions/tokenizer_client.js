const axios = require("axios");

const tokenizerClient = axios.create({
	baseURL: process.env.TOKENIZER_URL,
	headers: {
		'x-api-key': process.env.TOKENIZER_API_KEY || ""
	}
});

async function createToken(fiscalCode) {
  	return await tokenizerClient.put("", { "pii": fiscalCode })
  		.then(res => {
			console.log("tokenizer response", res.status, res.data);
  			return res.data;
  		})
  		.catch(error => {
			console.log("tokenizer error", error.response.status, error.response.data);
  			return error.response;
  		});

}

module.exports = {
	createToken
}
