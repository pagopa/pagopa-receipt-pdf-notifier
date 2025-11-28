const assert = require('assert');
const { After, Given, When, Then, setDefaultTimeout } = require('@cucumber/cucumber');
const { sleep, createReceiptForError, createCartReceiptForError } = require("./common");
const { getDocumentByIdFromReceiptsDatastore, deleteDocumentFromReceiptsDatastore, createDocumentInReceiptsDatastore, getDocumentByIdFromCartReceiptsDatastore, deleteDocumentFromCartReceiptsDatastore, createDocumentInCartReceiptsDatastore } = require("./receipts_datastore_client");
const { putMessageOnQueue, putMessageOnCartQueue } = require("./receipt_queue_client");
// set timeout for Hooks function, it allows to wait for long task
setDefaultTimeout(360 * 1000);

// After each Scenario
After(async function () {
    // remove event
    await deleteDocumentFromReceiptsDatastore(this.receiptId, this.receiptId);
    this.responseToCheck = null;
    this.receiptId = null;
    // remove cart event
    await deleteDocumentFromCartReceiptsDatastore(this.cartReceiptId, this.cartReceiptId);
    this.responseToCheck = null;
    this.cartReceiptId = null;
});


Given('a random receipt with id {string} stored on receipt datastore with generated pdf and status GENERATED', async function (id) {
    this.receiptId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromReceiptsDatastore(this.receiptId, this.receiptId);

    let receiptsStoreResponse = await createDocumentInReceiptsDatastore(this.receiptId);
    assert.strictEqual(receiptsStoreResponse.statusCode, 201);
});


When('receipt has been properly notified after {int} ms', async function (time) {
    // boundary time spent by azure function to process event
    await sleep(time);
    this.responseToCheck = await getDocumentByIdFromReceiptsDatastore(this.receiptId);
});

Then('the receipt has not the status {string}', function (targetStatus) {
    assert.notStrictEqual(this.responseToCheck.resources[0].status, targetStatus);
});

Given('a random receipt with id {string} enqueued on notification error queue', async function (id) {
    this.receiptId = id;
    let event = createReceiptForError(this.receiptId);
    await putMessageOnQueue(event);
});

////////////////////////////////////////////////////
////////////////        Cart        ////////////////
////////////////////////////////////////////////////


Given('a random cart receipt with id {string} stored on cart receipt datastore with generated pdf and status GENERATED', async function (id) {
    this.cartReceiptId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromCartReceiptsDatastore(this.cartReceiptId, this.cartReceiptId);

    let receiptsStoreResponse = await createDocumentInCartReceiptsDatastore(this.cartReceiptId);
    assert.strictEqual(receiptsStoreResponse.statusCode, 201);
});


When('cart receipt has been properly notified after {int} ms', async function (time) {
    // boundary time spent by azure function to process event
    await sleep(time);
    this.responseToCheck = await getDocumentByIdFromCartReceiptsDatastore(this.cartReceiptId);
});

Then('the cart receipt has not the status {string}', function (targetStatus) {
    assert.notStrictEqual(this.responseToCheck.resources[0].status, targetStatus);
});

Given('a random cart receipt with id {string} enqueued on notification error queue for cart', async function (id) {
    this.cartReceiptId = id;
    let event = createCartReceiptForError(this.cartReceiptId);
    await putMessageOnCartQueue(event);
});