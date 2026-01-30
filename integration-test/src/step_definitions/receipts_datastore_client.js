const { CosmosClient } = require("@azure/cosmos");
const { createReceipt, createReceiptForError, createCartReceipt, createCartReceiptForError } = require("./common");

const cosmos_db_conn_string = process.env.RECEIPTS_COSMOS_CONN_STRING || "";
const databaseId = process.env.RECEIPT_COSMOS_DB_NAME;
const containerId = process.env.RECEIPT_COSMOS_DB_CONTAINER_NAME;
const cartContainerId = process.env.CART_RECEIPT_COSMOS_DB_CONTAINER_NAME;

const client = new CosmosClient(cosmos_db_conn_string);
const container = client.database(databaseId).container(containerId);
const cartContainer = client.database(databaseId).container(cartContainerId);

async function getDocumentByIdFromReceiptsDatastore(id) {
    return await container.items
        .query({
            query: "SELECT * from c WHERE c.eventId=@eventId",
            parameters: [{ name: "@eventId", value: id }]
        })
        .fetchNext();
}

async function createDocumentInReceiptsDatastore(id, fiscalCodeToken) {
    let event = createReceipt(id, fiscalCodeToken);
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

////////////////////////////////////////////////////
////////////////        Cart        ////////////////
////////////////////////////////////////////////////

async function getDocumentByIdFromCartReceiptsDatastore(id) {
    return await cartContainer.items
        .query({
            query: "SELECT * from c WHERE c.cartId=@cartId",
            parameters: [{ name: "@cartId", value: id }]
        })
        .fetchNext();
}

async function createDocumentInCartReceiptsDatastore(id, fiscalCodeToken) {
    let event = createCartReceipt(id, fiscalCodeToken);
    try {
        return await cartContainer.items.create(event);
    } catch (err) {
        console.log(err);
    }
}

async function deleteDocumentFromCartReceiptsDatastore(id, partitionKey) {
    try {
        return await cartContainer.item(id, partitionKey).delete();
    } catch (error) {
        if (error.code !== 404) {
            console.log(error)
        }
    }
}

module.exports = {
    getDocumentByIdFromReceiptsDatastore,
    createDocumentInReceiptsDatastore,
    deleteDocumentFromReceiptsDatastore,
    getDocumentByIdFromCartReceiptsDatastore,
    createDocumentInCartReceiptsDatastore,
    deleteDocumentFromCartReceiptsDatastore
}