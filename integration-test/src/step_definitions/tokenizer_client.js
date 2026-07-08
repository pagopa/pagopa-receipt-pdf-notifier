const axios = require("axios");

const tokenizer_url = process.env.TOKENIZER_URL;

async function createToken(fiscalCode) {
    let token_api_key = process.env.TOKENIZER_API_KEY;
  	let headers = {
  	  "x-api-key": token_api_key
  	};

  	return await axios.put(tokenizer_url, { "pii": fiscalCode }, { headers })
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
