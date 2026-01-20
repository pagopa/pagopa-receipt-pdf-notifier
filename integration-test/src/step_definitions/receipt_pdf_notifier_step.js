const assert = require('assert');
const { After, Given, When, Then, setDefaultTimeout, Before } = require('@cucumber/cucumber');
const { sleep, createReceiptForError, createCartReceiptForError } = require("./common");
const { getDocumentByIdFromReceiptsDatastore, deleteDocumentFromReceiptsDatastore, createDocumentInReceiptsDatastore, getDocumentByIdFromCartReceiptsDatastore, deleteDocumentFromCartReceiptsDatastore, createDocumentInCartReceiptsDatastore } = require("./receipts_datastore_client");
const { putMessageOnQueue, putMessageOnCartQueue } = require("./receipt_queue_client");
const { createToken } = require("./tokenizer_client");
// set timeout for Hooks function, it allows to wait for long task
setDefaultTimeout(360 * 1000);

Before(async function () {
    let response = await createToken("JHNDOE00A01F205N");
    assert.notStrictEqual(response.token, null);
    this.fiscalCodeToken = response.token;
});

// After each Scenario
After(async function () {
    // remove event
    if (this.receiptId != null) {
        await deleteDocumentFromReceiptsDatastore(this.receiptId, this.receiptId);
    }
    // remove cart event
    if (this.cartReceiptId != null) {
        await deleteDocumentFromCartReceiptsDatastore(this.cartReceiptId, this.cartReceiptId);
    }
    this.responseToCheck = null;
    this.receiptId = null;
    this.cartReceiptId = null;
});


Given('a random receipt with id {string} stored on receipt datastore with generated pdf and status GENERATED', async function (id) {
    this.receiptId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromReceiptsDatastore(this.receiptId, this.receiptId);

    let receiptsStoreResponse = await createDocumentInReceiptsDatastore(this.receiptId, this.fiscalCodeToken);
    assert.strictEqual(receiptsStoreResponse.statusCode, 201);
});


When('receipt has been properly notified after {int} ms', async function (time) {
    // boundary time spent by azure function to process event
    await sleep(time);
    this.responseToCheck = await getDocumentByIdFromReceiptsDatastore(this.receiptId);

    if (this.responseToCheck.resources.length === 0) {
        // in some rare cases the first read does not find the document, retry after sleep
        await sleep(time);
        this.responseToCheck = await getDocumentByIdFromReceiptsDatastore(this.receiptId);
    }
    assert.strictEqual(this.responseToCheck.resources.length > 0, true);
});

Then('the receipt has not the status {string}', function (targetStatus) {
    assert.notStrictEqual(this.responseToCheck.resources[0].status, targetStatus);
});

Then('the receipt has the status {string}', function (targetStatus) {
    assert.strictEqual(this.responseToCheck.resources[0].status, targetStatus);
});

Given('a random receipt with id {string} enqueued on notification error queue', async function (id) {
    this.receiptId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromReceiptsDatastore(this.receiptId, this.receiptId);

    let event = createReceiptForError(this.receiptId, this.fiscalCodeToken);
    await putMessageOnQueue(event);
});

////////////////////////////////////////////////////
////////////////        Cart        ////////////////
////////////////////////////////////////////////////


Given('a random cart receipt with id {string} stored on cart receipt datastore with generated pdf and status GENERATED', async function (id) {
    this.cartReceiptId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromCartReceiptsDatastore(this.cartReceiptId, this.cartReceiptId);

    let receiptsStoreResponse = await createDocumentInCartReceiptsDatastore(this.cartReceiptId, this.fiscalCodeToken);
    assert.strictEqual(receiptsStoreResponse.statusCode, 201);
});


When('cart receipt has been properly notified after {int} ms', async function (time) {
    // boundary time spent by azure function to process event
    await sleep(time);
    this.responseToCheck = await getDocumentByIdFromCartReceiptsDatastore(this.cartReceiptId);

    if (this.responseToCheck.resources.length === 0) {
        // in some rare cases the first read does not find the document, retry after sleep
        await sleep(time);
        this.responseToCheck = await getDocumentByIdFromCartReceiptsDatastore(this.cartReceiptId);
    }
    assert.strictEqual(this.responseToCheck.resources.length > 0, true);
});

Then('the cart receipt has not the status {string}', function (targetStatus) {
    assert.notStrictEqual(this.responseToCheck.resources[0].status, targetStatus);
});

Then('the cart receipt has the status {string}', function (targetStatus) {
    assert.strictEqual(this.responseToCheck.resources[0].status, targetStatus);
});

Given('a random cart receipt with id {string} enqueued on notification error queue for cart', async function (id) {
    this.cartReceiptId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromCartReceiptsDatastore(this.cartReceiptId, this.cartReceiptId);

    let event = createCartReceiptForError(this.cartReceiptId, this.fiscalCodeToken);
    await putMessageOnCartQueue(event);
});