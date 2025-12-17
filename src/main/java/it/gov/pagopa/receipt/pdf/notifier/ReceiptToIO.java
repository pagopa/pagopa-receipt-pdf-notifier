package it.gov.pagopa.receipt.pdf.notifier;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.service.ReceiptToIOService;
import it.gov.pagopa.receipt.pdf.notifier.service.impl.ReceiptToIOServiceImpl;
import it.gov.pagopa.receipt.pdf.notifier.utils.MDCConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static it.gov.pagopa.receipt.pdf.notifier.utils.ReceiptToIOUtils.ANONIMO;

/**
 * Azure Functions with CosmosDB trigger.
 */
public class ReceiptToIO {

    private final Logger logger = LoggerFactory.getLogger(ReceiptToIO.class);

    private final Boolean payerNotifyDisabled = Boolean.parseBoolean(System.getenv().getOrDefault("PAYER_NOTIFY_DISABLED", "true"));

    private final ReceiptToIOService receiptToIOService;

    public ReceiptToIO() {
        this.receiptToIOService = new ReceiptToIOServiceImpl();
    }

    ReceiptToIO(ReceiptToIOService receiptToIOService) {
        this.receiptToIOService = receiptToIOService;
    }

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
     * @param listReceipts     Receipts saved on CosmosDB and triggering the function
     * @param documentReceipts Output binding to save receipts to cosmos
     * @param documentMessages Output binding to save the IO notification id to cosmos
     * @param context          Function context
     */
    @FunctionName("ReceiptToIoProcessor")
    public void processReceiptToIO(
            @CosmosDBTrigger(
                    name = "ReceiptInputDatastore",
                    databaseName = "db",
                    containerName = "receipts",
                    leaseContainerName = "receipts-leases",
                    leaseContainerPrefix = "materialized",
                    createLeaseContainerIfNotExists = true,
                    maxItemsPerInvocation = 300,
                    connection = "COSMOS_RECEIPTS_CONN_STRING")
            List<Receipt> listReceipts,
            @CosmosDBOutput(
                    name = "ReceiptOutputDatastore",
                    databaseName = "db",
                    containerName = "receipts",
                    connection = "COSMOS_RECEIPTS_CONN_STRING")
            OutputBinding<List<Receipt>> documentReceipts,
            @CosmosDBOutput(
                    name = "IoMessageDatastore",
                    databaseName = "db",
                    containerName = "receipts-io-messages-evt",
                    connection = "COSMOS_RECEIPTS_CONN_STRING")
            OutputBinding<List<IOMessage>> documentMessages,
            final ExecutionContext context
    ) {

        logger.info("[{}] function called at {}", context.getFunctionName(), LocalDateTime.now());

        List<Receipt> receiptsNotified = new ArrayList<>();
        List<IOMessage> messagesNotified = new ArrayList<>();

        listReceipts.parallelStream().forEach(receipt -> {
            try {
                String eventId = receipt != null ? receipt.getEventId() : null;
                MDC.put(MDCConstants.BIZ_EVENT_ID, eventId);
                if (isReceiptNotValid(receipt)) {
                    logger.info("Receipt discarded");
                    return;
                }

                EnumMap<UserType, UserNotifyStatus> notifyResult = notifyUsers(receipt);
                List<IOMessage> ioMessages = this.receiptToIOService.verifyMessagesNotification(notifyResult, receipt);

                messagesNotified.addAll(ioMessages);
                receiptsNotified.add(receipt);
            } finally {
                MDC.remove(MDCConstants.BIZ_EVENT_ID);
            }
        });

        if (!receiptsNotified.isEmpty()) {
            documentReceipts.setValue(receiptsNotified);
        }

        if (!messagesNotified.isEmpty()) {
            documentMessages.setValue(messagesNotified);
        }
    }

    private EnumMap<UserType, UserNotifyStatus> notifyUsers(Receipt receipt) {
        String debtorFiscalCode = receipt.getEventData().getDebtorFiscalCode();
        String payerFiscalCode = receipt.getEventData().getPayerFiscalCode();

        EnumMap<UserType, UserNotifyStatus> usersToBeVerified = new EnumMap<>(UserType.class);

        //Notify to debtor
        if (!ANONIMO.equals(debtorFiscalCode) && !(Boolean.TRUE.equals(payerNotifyDisabled) && debtorFiscalCode.equals(payerFiscalCode))) {
            UserNotifyStatus debtorNotifyStatus = this.receiptToIOService.notifyMessage(debtorFiscalCode, UserType.DEBTOR, receipt);
            usersToBeVerified.put(UserType.DEBTOR, debtorNotifyStatus);
        }

        if (!Boolean.TRUE.equals(payerNotifyDisabled)
                && (payerFiscalCode != null && (debtorFiscalCode == null || !debtorFiscalCode.equals(payerFiscalCode)))
        ) {
            //Notify to payer
            UserNotifyStatus payerNotifyStatus = this.receiptToIOService.notifyMessage(payerFiscalCode, UserType.PAYER, receipt);
            usersToBeVerified.put(UserType.PAYER, payerNotifyStatus);
        }
        return usersToBeVerified;
    }

    private boolean isReceiptNotValid(Receipt receipt) {
        return receipt == null
                || receipt.getEventData() == null
                || receipt.getEventData().getDebtorFiscalCode() == null
                || !statusCanBeNotified(receipt);
    }

    public boolean statusCanBeNotified(Receipt receipt) {
        return receipt.getStatus().equals(ReceiptStatusType.GENERATED) ||
                receipt.getStatus().equals(ReceiptStatusType.SIGNED) ||
                receipt.getStatus().equals(ReceiptStatusType.IO_NOTIFIER_RETRY);
    }
}
