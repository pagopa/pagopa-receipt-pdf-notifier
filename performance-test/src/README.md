# K6 tests for _ReceiptsNotifier_ project

[k6](https://k6.io/) is a load testing tool. 👀 See [here](https://k6.io/docs/get-started/installation/) to install it.

- [01. Receipt notifier function](#01-receipt-notifier-function)

This is a set of [k6](https://k6.io) tests related to the _Receipt PDF Notifier_ initiative.

To invoke k6 test passing parameter use -e (or --env) flag:

```
-e MY_VARIABLE=MY_VALUE
```

## 01. Receipt notifier function

Test the receipt notifier function:

```
k6 run --env VARS=local.environment.json --env TEST_TYPE=./test-types/load.json --env RECEIPT_COSMOS_DB_SUBSCRIPTION_KEY=<your-secret> receipt_processor.js
```

where the mean of the environment variables is:

```json
  "environment": [
    {
      "env": "local",
      "receiptCosmosDBURI": "",
      "receiptDatabaseID":"",
      "receiptContainerID":"",
      "processTime":""
    }
  ]
```

`receiptCosmosDBURI`: CosmosDB url to access Receipts CosmosDB REST API

`receiptDatabaseID`: database name to access Receipts Cosmos DB REST API

`receiptContainerID`: collection name to access Receipts Cosmos DB REST API

`processTime`: boundary time taken by azure function to fetch the payment event and save it in the datastore