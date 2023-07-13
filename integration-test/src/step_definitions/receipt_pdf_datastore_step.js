const assert = require('assert');
const {After, Given, When, Then, setDefaultTimeout} = require('@cucumber/cucumber');
const {sleep, createReceiptForError} = require("./common");
const {getDocumentByIdFromReceiptsDatastore, deleteDocumentFromReceiptsDatastore, createDocumentInReceiptsDatastore, createErrorDocumentInReceiptsDatastore} = require("./receipts_datastore_client");
const {putMessageOnQueue} = require("./receipt_queue_client");
// set timeout for Hooks function, it allows to wait for long task
setDefaultTimeout(360 * 1000);

// After each Scenario
After( async function () {
    // remove event
    await deleteDocumentFromReceiptsDatastore(this.receiptId, this.eventId);
    this.responseToCheck = null;
    this.receiptId = null;
});


Given('a random receipt with id {string} stored on receipt datastore with generated pdf', async function (id) {
    this.eventId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromReceiptsDatastore(this.eventId, this.eventId);

    let receiptsStoreResponse =  await createDocumentInReceiptsDatastore(this.eventId);
    assert.strictEqual(receiptsStoreResponse.statusCode, 201);
    this.receiptId = this.eventId;
  });


When('receipt has been properly stored into receipt datastore after {int} ms with eventId {string}', async function (time, eventId) {
    // boundary time spent by azure function to process event
    await sleep(time);
    this.responseToCheck = await getDocumentByIdFromReceiptsDatastore(this.eventId);
});

Then('the receipt has not the status {string}', function (targetStatus) {
    assert.notStrictEqual(this.responseToCheck.resources[0].status, targetStatus);
});

Given('a random receipt with id {string} stored on receipt datastore with generated pdf', async function (id) {
    this.eventId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromReceiptsDatastore(this.eventId, this.eventId);

    let receiptsStoreResponse =  await createErrorDocumentInReceiptsDatastore(this.eventId);
    assert.strictEqual(receiptsStoreResponse.statusCode, 201);
    this.receiptId = this.eventId;
});

Given('a random receipt with id {string} enqueued on notification error queue', async function (id) {
    assert.strictEqual(this.eventId, id);
    let event = createReceiptForError(this.eventId);
    await putMessageOnQueue(event);
});

Then('the receipt has not the status {string}', function (targetStatus) {
    assert.notStrictEqual(this.responseToCheck.resources[0].status, targetStatus);
})



