package it.gov.pagopa.receipt.pdf.notifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.*;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.utils.ObjectMapperUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Azure Functions with Azure Storage Queue trigger.
 */
public class NotifierRetry {

    /**
     * This function will be invoked when an Azure Storage Queue trigger occurs
     * #
     * Retrieve receipt with the eventId equals to the decoded queue message
     * If found the receipt's status is updated as IO_NOTIFIER_RETRY and saved on Cosmos
     * It will trigger the ReceiptToIO function to retry the notification to user
     *
     * @param message Message from notification error queue
     * @param documentReceipts Output binding to save the updated receipt
     * @param context Function Context
     * @throws ReceiptNotFoundException in case the receipt is not found
     */
    @FunctionName("NotifierRetryProcessor")
    public void processNotifierRetry(
            @QueueTrigger(
                    name = "QueueReceiptIoNotifierError",
                    queueName = "%NOTIFIER_QUEUE_TOPIC%",
                    connection = "STORAGE_CONN_STRING")
            String queueMessage,
            @CosmosDBOutput(
                    name = "ReceiptOutputDatastore",
                    databaseName = "db",
                    collectionName = "receipts",
                    connectionStringSetting = "COSMOS_RECEIPTS_CONN_STRING")
            OutputBinding<List<Receipt>> documentReceipts,
            final ExecutionContext context
    ) throws JsonProcessingException {
        Logger logger = context.getLogger();

        String logMsg = String.format("NotifierRetry function called at %s", LocalDateTime.now());
        logger.info(logMsg);

        List<Receipt> receiptsToRetry = new ArrayList<>();

        if (queueMessage != null && !queueMessage.isEmpty()) {

            Receipt receipt = ObjectMapperUtils.mapString(queueMessage, Receipt.class);

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
