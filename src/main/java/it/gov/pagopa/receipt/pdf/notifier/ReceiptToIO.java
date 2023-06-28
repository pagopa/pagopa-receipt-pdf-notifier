package it.gov.pagopa.receipt.pdf.notifier;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueOutput;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.service.impl.ReceiptToIOServiceImpl;

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
                    name = "ReceiptInputDatastore",
                    databaseName = "db",
                    collectionName = "receipts",
                    maxItemsPerInvocation = 100,
                    connectionStringSetting = "COSMOS_RECEIPTS_CONN_STRING")
            List<Receipt> listReceipts,
            @QueueOutput(
                    name = "QueueReceiptIoNotifierError",
                    queueName = "%NOTIFIER_QUEUE_TOPIC%",
                    connection = "STORAGE_CONN_STRING")
            OutputBinding<String> requeueMessages,
            @CosmosDBOutput(
                    name = "ReceiptOutputDatastore",
                    databaseName = "db",
                    collectionName = "receipts",
                    connectionStringSetting = "COSMOS_RECEIPTS_CONN_STRING")
            OutputBinding<List<Receipt>> documentReceipts,
            @CosmosDBOutput(
                    name = "IoMessageDatastore",
                    databaseName = "db",
                    collectionName = "receipts-io-messages",
                    connectionStringSetting = "COSMOS_RECEIPTS_CONN_STRING")
            OutputBinding<List<IOMessage>> documentMessages,
            final ExecutionContext context
    ) {
        Logger logger = context.getLogger();

        String logMsg = String.format("ReceiptToIO function called at %s", LocalDateTime.now());
        logger.info(logMsg);
        int discarder = 0;

        List<Receipt> receiptsNotified = new ArrayList<>();
        List<IOMessage> messagesNotified = new ArrayList<>();
        int queueSent = 0;

        for (Receipt receipt : listReceipts) {
            if (receipt != null &&
                    receipt.getEventData() != null &&
                    (receipt.getStatus().equals(ReceiptStatusType.GENERATED) ||
                            receipt.getStatus().equals(ReceiptStatusType.SIGNED) ||
                            receipt.getStatus().equals(ReceiptStatusType.IO_NOTIFIER_RETRY))
            ) {


                String debtorFiscalCode = receipt.getEventData().getDebtorFiscalCode();
                String payerFiscalCode = receipt.getEventData().getPayerFiscalCode();

                Map<String, UserNotifyStatus> usersToBeVerified = new HashMap<>();

                ReceiptToIOServiceImpl service = new ReceiptToIOServiceImpl();

                //TODO verify if both fiscal code can be null
                if (debtorFiscalCode != null &&
                        (receipt.getIoMessageData() == null ||
                                receipt.getIoMessageData().getIdMessageDebtor() == null)
                ) {
                    service.notifyMessage(usersToBeVerified, debtorFiscalCode, UserType.DEBTOR, receipt, logger);
                }
                if (payerFiscalCode != null &&
                        (receipt.getIoMessageData() == null ||
                                receipt.getIoMessageData().getIdMessagePayer() == null
                        )
                ) {
                    service.notifyMessage(usersToBeVerified, payerFiscalCode, UserType.PAYER, receipt, logger);
                }

                queueSent += service.verifyMessagesNotification(
                        usersToBeVerified,
                        messagesNotified,
                        receipt,
                        requeueMessages,
                        logger
                );

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
}
