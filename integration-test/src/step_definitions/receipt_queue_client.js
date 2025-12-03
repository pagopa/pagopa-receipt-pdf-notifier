const { QueueServiceClient } = require("@azure/storage-queue");

const connStr = process.env.RECEIPTS_STORAGE_CONN_STRING || "";
const queueName = process.env.NOTIFIER_QUEUE_TOPIC;
const cartQueueName = process.env.NOTIFIER_CART_QUEUE_TOPIC;

async function putMessageOnQueue(message) {
    const queueServiceClient = QueueServiceClient.fromConnectionString(connStr);
    const queueClient = queueServiceClient.getQueueClient(queueName);
    // Send a message into the queue using the sendMessage method.
    message = btoa(JSON.stringify(message));
    return await queueClient.sendMessage(message);
}

async function putMessageOnCartQueue(message) {
    const queueServiceClient = QueueServiceClient.fromConnectionString(connStr);
    const cartQueueClient = queueServiceClient.getQueueClient(cartQueueName);
    // Send a message into the queue using the sendMessage method.
    message = btoa(JSON.stringify(message));
    return await cartQueueClient.sendMessage(message);
}

module.exports = {
    putMessageOnQueue, putMessageOnCartQueue
}