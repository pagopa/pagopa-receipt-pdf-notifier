const assert = require('assert');
const { After, Given, When, Then, setDefaultTimeout, Before } = require('@cucumber/cucumber');
const { sleep, createReceiptForError, createCartReceiptForError } = require("./common");
const { getDocumentByIdFromReceiptsDatastore, deleteDocumentFromReceiptsDatastore, createDocumentInReceiptsDatastore, getDocumentByIdFromCartReceiptsDatastore, deleteDocumentFromCartReceiptsDatastore, createDocumentInCartReceiptsDatastore } = require("./receipts_datastore_client");
const { putMessageOnQueue, putMessageOnCartQueue } = require("./receipt_queue_client");
const { createToken } = require("./tokenizer_client");
// set timeout for Hooks function, it allows to wait for long task
setDefaultTimeout(360 * 1000);

const TERMINAL_STATUSES = new Set(["NOT_TO_NOTIFY", "IO_NOTIFIED", "UNABLE_TO_SEND"]);
// Extra polling budget after the "warm-up" sleep passed by the feature file.
const EXTRA_POLL_MS = 3 * 60 * 1000; // 3 minutes
const POLL_INTERVAL_MS = 5 * 1000;

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
    // boundary time spent by azure function to process event, then poll until terminal status
    this.responseToCheck = await waitForTerminalStatus(
        () => getDocumentByIdFromReceiptsDatastore(this.receiptId),
        time
    );
    assert.strictEqual(this.responseToCheck.resources.length > 0, true);
});

Then('the receipt has not the status {string}', function (targetStatus) {
    assert.notStrictEqual(this.responseToCheck.resources[0].status, targetStatus);
});

Then('the receipt has the status {string}', function (targetStatus) {
    console.log("Receipt", this.responseToCheck.resources[0].id, this.responseToCheck.resources[0].status);
    assert.strictEqual(this.responseToCheck.resources[0].status, targetStatus);
});

Given('a random receipt with id {string} enqueued on notification error queue', async function (id) {
    this.receiptId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromReceiptsDatastore(this.receiptId, this.receiptId);

    let event = createReceiptForError(this.receiptId, this.fiscalCodeToken);
    console.log("Enqueuing event on notification error queue:", event);
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
    // boundary time spent by azure function to process event, then poll until terminal status
    this.responseToCheck = await waitForTerminalStatus(
        () => getDocumentByIdFromCartReceiptsDatastore(this.cartReceiptId),
        time
    );
    assert.strictEqual(this.responseToCheck.resources.length > 0, true);
});

Then('the cart receipt has not the status {string}', function (targetStatus) {
    assert.notStrictEqual(this.responseToCheck.resources[0].status, targetStatus);
});

Then('the cart receipt has the status {string}', function (targetStatus) {
    console.log("Cart receipt", this.responseToCheck.resources[0].id, this.responseToCheck.resources[0].status);
    assert.strictEqual(this.responseToCheck.resources[0].status, targetStatus);
});

Given('a random cart receipt with id {string} enqueued on notification error queue for cart', async function (id) {
    this.cartReceiptId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromCartReceiptsDatastore(this.cartReceiptId, this.cartReceiptId);

    let event = createCartReceiptForError(this.cartReceiptId, this.fiscalCodeToken);
    console.log("Enqueuing event on notification error queue for cart:", event);
    await putMessageOnCartQueue(event);
});

async function waitForTerminalStatus(fetchFn, initialSleepMs) {
    await sleep(initialSleepMs);
    let response = await fetchFn();
    const deadline = Date.now() + EXTRA_POLL_MS;
    while (
        Date.now() < deadline &&
        (response.resources.length === 0 ||
            !TERMINAL_STATUSES.has(response.resources[0].status))
        ) {
        await sleep(POLL_INTERVAL_MS);
        response = await fetchFn();
    }
    return response;
}