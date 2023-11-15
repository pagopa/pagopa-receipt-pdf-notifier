package it.gov.pagopa.receipt.pdf.notifier;

import com.azure.cosmos.implementation.apachecommons.lang.tuple.Pair;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.*;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.ReasonError;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.service.ReceiptToIOService;
import it.gov.pagopa.receipt.pdf.notifier.service.impl.ReceiptToIOServiceImpl;
import it.gov.pagopa.receipt.pdf.notifier.utils.ReceiptToIOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Azure Functions with CosmosDB trigger.
 */
public class ReceiptToIO {

    private final Logger logger = LoggerFactory.getLogger(ReceiptToIO.class);

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
     * @param listReceipts Receipts saved on CosmosDB and triggering the function
     * @param documentReceipts Output binding to save receipts to cosmos
     * @param documentMessages Output binding to save the IO notification id to cosmos
     * @param context Function context
     */
    @FunctionName("ReceiptToIoProcessor")
    public void processReceiptToIO(
            @CosmosDBTrigger(
                    name = "ReceiptInputDatastore",
                    databaseName = "db",
                    collectionName = "receipts",
                    leaseCollectionName = "receipts-leases",
                    leaseCollectionPrefix = "materialized",
                    createLeaseCollectionIfNotExists = true,
                    maxItemsPerInvocation = 300,
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
    ) throws JsonProcessingException {

        logger.info("[{}] function called at {}", context.getFunctionName(), LocalDateTime.now());
        AtomicInteger discarder = new AtomicInteger();

        List<Receipt> receiptsNotified = new ArrayList<>();
        List<IOMessage> messagesNotified = new ArrayList<>();
        AtomicInteger queueSent = new AtomicInteger();

        listReceipts.parallelStream().forEach(receipt ->  {
            if (receipt == null
                    || receipt.getEventData() == null
                    || receipt.getEventData().getDebtorFiscalCode() == null
                    || !statusCanBeNotified(receipt)
            ) {
                discarder.getAndIncrement();
                return;
            }

            String debtorFiscalCode = receipt.getEventData().getDebtorFiscalCode();
            String payerFiscalCode = receipt.getEventData().getPayerFiscalCode();

            EnumMap<UserType, UserNotifyStatus> usersToBeVerified = new EnumMap<>(UserType.class);

            //Notify to debtor
            Pair<Receipt, UserNotifyStatus> userNotifyStatusPair = this.receiptToIOService.notifyMessage(debtorFiscalCode, UserType.DEBTOR, receipt);
            receipt = userNotifyStatusPair.getLeft();
            UserNotifyStatus debtorNotifyStatus = userNotifyStatusPair.getRight();
            usersToBeVerified.put(UserType.DEBTOR, debtorNotifyStatus);

            if(payerFiscalCode != null && (debtorFiscalCode == null || !debtorFiscalCode.equals(payerFiscalCode))){
                //Notify to payer
                userNotifyStatusPair = this.receiptToIOService.notifyMessage(payerFiscalCode, UserType.PAYER, receipt);
                receipt = userNotifyStatusPair.getLeft();
                UserNotifyStatus payerNotifyStatus = userNotifyStatusPair.getRight();
                usersToBeVerified.put(UserType.PAYER, payerNotifyStatus);
            }


            boolean boolQueueSent = false;
            try {
                Pair<Receipt, Boolean> pair =
                        this.receiptToIOService.verifyMessagesNotification(usersToBeVerified,
                                messagesNotified, receipt);
                receipt = pair.getLeft();
                boolQueueSent = pair.getRight();
            } catch (JsonProcessingException e) {
                receipt.setStatus(ReceiptStatusType.IO_ERROR_TO_NOTIFY);
                int code = ReceiptToIOUtils.getCodeOrDefault(e);
                ReceiptToIOUtils.buildReasonError(e.getMessage(), code);
            }

            if(boolQueueSent){
                queueSent.getAndIncrement();
            }

            logger.info(receipt.getStatus().toString());
            receiptsNotified.add(receipt);
        });

        if (!receiptsNotified.isEmpty()) {
            documentReceipts.setValue(receiptsNotified);
        }

        if (!messagesNotified.isEmpty()) {
            documentMessages.setValue(messagesNotified);
        }
    }

    public boolean statusCanBeNotified(Receipt receipt) {
        return receipt.getStatus().equals(ReceiptStatusType.GENERATED) ||
                receipt.getStatus().equals(ReceiptStatusType.SIGNED) ||
                receipt.getStatus().equals(ReceiptStatusType.IO_NOTIFIER_RETRY);
    }}
