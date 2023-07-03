package it.gov.pagopa.receipt.pdf.notifier;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.*;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.service.impl.ReceiptToIOServiceImpl;
import it.gov.pagopa.receipt.pdf.notifier.utils.ReceiptToIOUtils;

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

    /**
     * This function will be invoked when a CosmosDB trigger occurs
     * #
     * User(s) retrieved from the receipt's data will be notified in the IO app
     * and the receipt will be updated with the right status
     * #
     * Step to notify user:
     * - Verify user is a IO user with IO API /profiles
     * - Send notification message to IO user with IO API /messages
     * If user(s) is not an IO user the receipt's status will be NOT_TO_NOTIFY
     * In case of errors during notification the receipt's status will be:
     * - IO_ERROR_TO_NOTIFY before max numbers of retry
     * - UNABLE_TO_SEND after max numbers of retry
     * #
     * In case of any type of error a queue message with the receipt's biz-event
     * id will be sent to the error queue ready to be processed by the NotifyRetry function
     * #
     * In case of success the receipt's status will be IO_NOTIFIED
     *
     * @param listReceipts Receipts saved on CosmosDB and triggering the function
     * @param documentReceipts Output binding to save receipts to cosmos
     * @param documentMessages Output binding to save the IO notification id to cosmos
     * @param context Function context
     */
    @FunctionName("ReceiptToIoProcessor")
    @ExponentialBackoffRetry(maxRetryCount = 5, minimumInterval = "500", maximumInterval = "5000")
    public void processReceiptToIO(
            @CosmosDBTrigger(
                    name = "ReceiptInputDatastore",
                    databaseName = "db",
                    collectionName = "receipts",
                    leaseCollectionName = "receipts-leases",
                    leaseCollectionPrefix = "materialized",
                    createLeaseCollectionIfNotExists = true,
                    maxItemsPerInvocation = 100,
                    connectionStringSetting = "COSMOS_RECEIPTS_CONN_STRING")
            List<Receipt> listReceipts,
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
                    ReceiptToIOUtils.verifyReceiptStatus(receipt)
            ) {


                String debtorFiscalCode = receipt.getEventData().getDebtorFiscalCode();
                String payerFiscalCode = receipt.getEventData().getPayerFiscalCode();

                Map<String, UserNotifyStatus> usersToBeVerified = new HashMap<>();

                ReceiptToIOServiceImpl service = new ReceiptToIOServiceImpl();

                //TODO verify if both fiscal code can be null
                //Notify to debtor
                service.notifyMessage(usersToBeVerified, debtorFiscalCode, UserType.DEBTOR, receipt, logger);

                if(payerFiscalCode != null && (debtorFiscalCode == null || !debtorFiscalCode.equals(payerFiscalCode))){
                    //Notify to payer
                    service.notifyMessage(usersToBeVerified, payerFiscalCode, UserType.PAYER, receipt, logger);
                }

                //Verify notification(s) status
                queueSent += service.verifyMessagesNotification(
                        usersToBeVerified,
                        messagesNotified,
                        receipt,
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
