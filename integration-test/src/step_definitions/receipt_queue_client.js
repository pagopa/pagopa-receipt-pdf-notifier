const { QueueServiceClient } = require("@azure/storage-queue");

const connStr = process.env.RECEIPTS_STORAGE_CONN_STRING || "";
const queueName = process.env.NOTIFIER_QUEUE_TOPIC;
const cartQueueName = process.env.NOTIFIER_CART_QUEUE_TOPIC;

const queueServiceClient = QueueServiceClient.fromConnectionString(connStr);
const receiptQueueClient = queueServiceClient.getQueueClient(queueName);
const cartQueueClient = queueServiceClient.getQueueClient(cartQueueName);

async function putMessageOnQueue(message) {
    // Send a message into the queue using the sendMessage method.
    message = btoa(JSON.stringify(message));
    return await receiptQueueClient.sendMessage(message);
}

async function putMessageOnCartQueue(message) {
    // Send a message into the queue using the sendMessage method.
    message = btoa(JSON.stringify(message));
    return await cartQueueClient.sendMessage(message);
}

module.exports = {
    putMessageOnQueue, putMessageOnCartQueue
}