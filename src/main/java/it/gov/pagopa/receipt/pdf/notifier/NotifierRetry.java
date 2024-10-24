package it.gov.pagopa.receipt.pdf.notifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.*;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.utils.ObjectMapperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Azure Functions with Azure Storage Queue trigger.
 */
public class NotifierRetry {

    private final Logger logger = LoggerFactory.getLogger(NotifierRetry.class);

    /**
     * This function will be invoked when an Azure Storage Queue trigger occurs
     * #
     * The queueMessage is mapped to the Receipt class
     * and updated with the status IO_NOTIFIER_RETRY
     * It will trigger the ReceiptToIO function to retry the notification to user
     *
     * @param queueMessage Message from notification error queue with receipt's data
     * @param documentReceipts Output binding to save the updated receipt
     * @param context Function Context
     * @throws JsonProcessingException in case the message can't be mapped to the Receipt class
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
                    containerName = "receipts",
                    connection = "COSMOS_RECEIPTS_CONN_STRING")
            OutputBinding<List<Receipt>> documentReceipts,
            final ExecutionContext context
    ) throws JsonProcessingException {

        logger.info("[{}] function called at {}", context.getFunctionName(), LocalDateTime.now());
        List<Receipt> receiptsToRetry = new ArrayList<>();

        if (queueMessage != null && !queueMessage.isEmpty()) {
            Receipt receipt = ObjectMapperUtils.mapString(queueMessage, Receipt.class);

            if (receipt != null && receipt.getStatus().equals(ReceiptStatusType.IO_ERROR_TO_NOTIFY)) {
                receipt.setStatus(ReceiptStatusType.IO_NOTIFIER_RETRY);
                receiptsToRetry.add(receipt);
            }
        }
        //Call to receipts' datastore info
        logger.debug("receipts retry notify stat {} function - number of receipts updated with state IO_NOTIFY_RETRY on the receipts' datastore {}",
                context.getInvocationId(), receiptsToRetry.size());

        if (!receiptsToRetry.isEmpty()) {
            documentReceipts.setValue(receiptsToRetry);
        }
    }
}
