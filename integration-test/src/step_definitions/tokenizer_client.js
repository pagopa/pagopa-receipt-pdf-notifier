const axios = require("axios");

const tokenizer_url = process.env.TOKENIZER_URL;

async function createToken(fiscalCode) {
    let token_api_key = process.env.TOKENIZER_API_KEY;
  	let headers = {
  	  "x-api-key": token_api_key
  	};

  	return await axios.put(tokenizer_url, { "pii": fiscalCode }, { headers })
  		.then(res => {
  			return res.data;
  		})
  		.catch(error => {
  			return error.response;
  		});

}

module.exports = {
	createToken
}
