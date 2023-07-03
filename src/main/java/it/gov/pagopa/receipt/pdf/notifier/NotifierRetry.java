package it.gov.pagopa.receipt.pdf.notifier;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.*;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.ReceiptCosmosClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.exception.ReceiptNotFoundException;
import org.glassfish.pfl.basic.fsm.Guard;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

/**
 * Azure Functions with CosmosDB trigger.
 */
public class NotifierRetry {

    @FunctionName("NotifierRetryProcessor")
    @ExponentialBackoffRetry(maxRetryCount = 5, minimumInterval = "500", maximumInterval = "5000")
    public void processNotifierRetry(
            @QueueTrigger(
                    name = "QueueReceiptIoNotifierError",
                    queueName = "%NOTIFIER_QUEUE_TOPIC%",
                    connection = "STORAGE_CONN_STRING")
            String message,
            @CosmosDBOutput(
                    name = "ReceiptOutputDatastore",
                    databaseName = "db",
                    collectionName = "receipts",
                    connectionStringSetting = "COSMOS_RECEIPTS_CONN_STRING")
            OutputBinding<List<Receipt>> documentReceipts,
            final ExecutionContext context
    ) throws ReceiptNotFoundException {
        Logger logger = context.getLogger();

        String logMsg = String.format("NotifierRetry function called at %s", LocalDateTime.now());
        logger.info(logMsg);

        List<Receipt> receiptsToRetry = new ArrayList<>();

        if (message != null && !message.isEmpty()) {

            String bizEventId = new String(Base64.getDecoder().decode(message));

            ReceiptCosmosClientImpl client = ReceiptCosmosClientImpl.getInstance();

            Receipt receipt = client.getReceiptDocument(bizEventId);

            if (receipt != null && receipt.getStatus().equals(ReceiptStatusType.IO_ERROR_TO_NOTIFY)) {
                receipt.setStatus(ReceiptStatusType.IO_NOTIFIER_RETRY);

                receiptsToRetry.add(receipt);
            }

        }

        //Call to receipts' datastore info
        logMsg = String.format(
                "receipts retry notify stat %s function - number of receipts updated with state IO_NOTIFY_RETRY on the receipts' datastore %d",
                context.getInvocationId(), receiptsToRetry.size());
        logger.info(logMsg);

        if (!receiptsToRetry.isEmpty()) {
            documentReceipts.setValue(receiptsToRetry);
        }


    }
}
