package it.gov.pagopa.receipt.pdf.notifier;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueOutput;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.exception.ErrorToNotifyException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Azure Functions with CosmosDB trigger.
 */
public class ReceiptToIO {

    @FunctionName("ReceiptToIoProcessor")
    //TODO @ExponentialBackoffRetry(maxRetryCount = 5, minimumInterval = "500", maximumInterval = "5000")
    public void processReceiptToIO(
            @CosmosDBTrigger(
                    name = "ReceiptDatastore",
                    databaseName = "db",
                    collectionName = "receipts",
                    //TODO verify need of lease collection
                    //  leaseCollectionName = "biz-events-leases",
                    //  leaseCollectionPrefix = "materialized",
                    //  createLeaseCollectionIfNotExists = true,
                    maxItemsPerInvocation = 100,
                    connectionStringSetting = "COSMOS_RECEIPTS_CONN_STRING")
            List<Receipt> listReceipts,
            @QueueOutput(
                    name = "QueueReceiptIoNotifierError",
                    //TODO change with correct queue name
                    queueName = "pagopa-d-weu-receipts-queue-receipt-waiting-4-gen",
                    connection = "NOTIFIER_QUEUE_CONN_STRING")
            OutputBinding<String> requeueMessage,
            @CosmosDBOutput(
                    name = "ReceiptDatastore",
                    databaseName = "db",
                    collectionName = "receipts",
                    connectionStringSetting = "COSMOS_RECEIPTS_CONN_STRING")
            OutputBinding<List<Receipt>> documentReceipts,
            @CosmosDBOutput(
                    name = "IoMessageDatastore",
                    databaseName = "db",
                    //TODO change with correct collection name
                    collectionName = "messages",
                    connectionStringSetting = "COSMOS_IO_MESSAGE_CONN_STRING")
            OutputBinding<List<Map<String, String>>> documentMessages,
            final ExecutionContext context
    ) {
        Logger logger = context.getLogger();

        String logMsg = String.format("ReceiptToIO function called at %s", LocalDateTime.now());
        logger.info(logMsg);
        int discarder = 0;

        List<Receipt> receiptsNotified = new ArrayList<>();
        List<Map<String, String>> messagesNotified = new ArrayList<>();
        int queueSent = 0;

        for (Receipt receipt : listReceipts) {
            if (receipt != null && (receipt.getStatus().equals(ReceiptStatusType.GENERATED) || receipt.getStatus().equals(ReceiptStatusType.IO_NOTIFIER_RETRY))) {

                try {
                    if (receipt.getEventData() != null) {
                        String debtorFiscalCode = receipt.getEventData().getDebtorFiscalCode();
                        String payerFiscalCode = receipt.getEventData().getPayerFiscalCode();

                        boolean payerDebtorEqual = payerFiscalCode.equals(debtorFiscalCode);

                        if (payerDebtorEqual) {
                            //TODO verify user is an IO user
                            //IO /profiles

                            //TODO send notification to user
                            //IO /messages
                        } else {
                            //TODO verify user(s) is an IO user
                            //IO /profiles

                            //TODO send notification to users
                            //IO /messages
                        }

                        verifyMessagesNotification(messagesNotified, receipt, payerDebtorEqual);

                    } else {
                        throw new ErrorToNotifyException("Receipt does not contain event data");
                    }
                } catch (ErrorToNotifyException e) {
                    receipt.setStatus(ReceiptStatusType.IO_NOTIFIER_RETRY);
                    //TODO save on queue on error
                    queueSent++;
                }

                receiptsNotified.add(receipt);
            } else {
                discarder++;
            }
        }

        //Discarder info
        logMsg = String.format("itemsDone stat %s function - %d number of events in discarder  ", context.getInvocationId(), discarder);
        logger.info(logMsg);

        //Call to error queue info
        logMsg = String.format("error messages stat %s function - number of error messages sent to queue %d", context.getInvocationId(), queueSent);
        logger.info(logMsg);

        //Call to receipts' datastore info
        logMsg = String.format("receipts notified stat %s function - number of receipts updated on the receipts' datastore %d", context.getInvocationId(), receiptsNotified.size());
        logger.info(logMsg);

        //Call to messages' datastore info
        logMsg = String.format("messages notified stat %s function - number of messages inserted on the messages' datastore %d", context.getInvocationId(), messagesNotified.size());
        logger.info(logMsg);

        if (!receiptsNotified.isEmpty()) {
            documentReceipts.setValue(receiptsNotified);
        }

        if (!messagesNotified.isEmpty()) {
            documentMessages.setValue(messagesNotified);
        }

    }

    private static void verifyMessagesNotification(List<Map<String, String>> messagesNotified, Receipt receipt, boolean payerDebtorEqual) throws ErrorToNotifyException {
        if (receipt.getIoMessageData() != null) {
            Map<String, String> debtorMessageData = new HashMap<>();
            debtorMessageData.put(receipt.getIoMessageData().getIdMessageDebtor(), receipt.getIdEvent());
            messagesNotified.add(debtorMessageData);

            String payerMessageId = receipt.getIoMessageData().getIdMessagePayer();

            if (payerMessageId != null && !payerMessageId.isEmpty()) {
                Map<String, String> payerMessageData = new HashMap<>();
                payerMessageData.put(payerMessageId, receipt.getIdEvent());
                messagesNotified.add(payerMessageData);

            } else {
                if (!payerDebtorEqual) {
                    throw new ErrorToNotifyException("Error sending notification to IO user (payer)");
                }
            }

            receipt.setStatus(ReceiptStatusType.IO_NOTIFIED);
        } else {
            throw new ErrorToNotifyException("Error sending notification to IO user");
        }
    }
}
