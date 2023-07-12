const { CosmosClient } = require("@azure/cosmos");
const {createReceipt} = require("./common");

const cosmos_db_conn_string  = process.env.RECEIPTS_COSMOS_CONN_STRING || "";
const databaseId             = process.env.RECEIPT_COSMOS_DB_NAME; 
const containerId            = process.env.RECEIPT_COSMOS_DB_CONTAINER_NAME;

const client = new CosmosClient(cosmos_db_conn_string);
const container = client.database(databaseId).container(containerId);

async function getDocumentByIdFromReceiptsDatastore(id) {
    return await container.items
                    .query({
                        query: "SELECT * from c WHERE c.eventId=@eventId",
                        parameters: [{ name: "@eventId", value: id }]
                      })
                    .fetchNext();
}

async function createDocumentInReceiptsDatastore(id) {  
    let event = createReceipt(id);
    try {
        return await container.items.create(event);
    } catch (err) {
        console.log(err);
    }
}

async function deleteDocumentFromReceiptsDatastore(id, partitionKey) {
    try {
        return await container.item(id, partitionKey).delete();
    } catch (error) {
        if (error.code !== 404) {
            console.log(error)
        }
    }
}

module.exports = {
    getDocumentByIdFromReceiptsDatastore, createDocumentInReceiptsDatastore, deleteDocumentFromReceiptsDatastore
}