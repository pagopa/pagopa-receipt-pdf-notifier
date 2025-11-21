package it.gov.pagopa.receipt.pdf.notifier;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartStatusType;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.CartIOMessage;
import it.gov.pagopa.receipt.pdf.notifier.model.NotifyCartResult;
import it.gov.pagopa.receipt.pdf.notifier.service.CartReceiptToIOService;
import it.gov.pagopa.receipt.pdf.notifier.service.impl.CartReceiptToIOServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Azure Functions with CosmosDB trigger.
 */
public class CartReceiptToIO {

    private final Logger logger = LoggerFactory.getLogger(CartReceiptToIO.class);

    private final CartReceiptToIOService cartReceiptToIOService;

    public CartReceiptToIO() {
        this.cartReceiptToIOService = new CartReceiptToIOServiceImpl();
    }

    CartReceiptToIO(CartReceiptToIOService cartReceiptToIOService) {
        this.cartReceiptToIOService = cartReceiptToIOService;
    }

    /**
     * This function will be invoked when a CosmosDB trigger occurs
     * #
     * User(s) retrieved from the cart receipt's data will be notified in the IO app
     * and the cart receipt will be updated with the right status
     * #
     * Step to notify user:
     * - Verify user is a IO user with IO API /profiles
     * - Send notification message to IO user with IO API /messages
     * If user(s) is not an IO user the receipt's status will be NOT_TO_NOTIFY
     * In case of errors during notification the receipt's status will be:
     * - IO_ERROR_TO_NOTIFY before max numbers of retry
     * - UNABLE_TO_SEND after max numbers of retry
     * #
     * In case of any type of error a queue message with the receipt's cart
     * id will be sent to the error queue ready to be processed by the NotifyRetry function
     * #
     * In case of success the receipt's status will be IO_NOTIFIED
     *
     * @param listReceipts     Receipts saved on CosmosDB and triggering the function
     * @param documentReceipts Output binding to save receipts to cosmos
     * @param documentMessages Output binding to save the IO notification id to cosmos
     * @param context          Function context
     */
    @FunctionName("CartReceiptToIoProcessor")
    public void processReceiptToIO(
            @CosmosDBTrigger(
                    name = "CartReceiptInputDatastore",
                    databaseName = "db",
                    containerName = "cart-for-receipts",
                    leaseContainerName = "cart-for-receipts-leases",
                    leaseContainerPrefix = "materialized",
                    createLeaseContainerIfNotExists = true,
                    maxItemsPerInvocation = 300,
                    connection = "COSMOS_RECEIPTS_CONN_STRING")
            List<CartForReceipt> listReceipts,
            @CosmosDBOutput(
                    name = "CartReceiptOutputDatastore",
                    databaseName = "db",
                    containerName = "cart-for-receipts",
                    connection = "COSMOS_RECEIPTS_CONN_STRING")
            OutputBinding<List<CartForReceipt>> documentReceipts,
            @CosmosDBOutput(
                    name = "CartIoMessageDatastore",
                    databaseName = "db",
                    containerName = "cart-receipts-io-messages",
                    connection = "COSMOS_RECEIPTS_CONN_STRING")
            OutputBinding<List<CartIOMessage>> documentMessages,
            final ExecutionContext context
    ) {

        logger.info("[{}] function called at {} with a batch of {} cart receipt",
                context.getFunctionName(), LocalDateTime.now(), listReceipts.size());

        List<CartForReceipt> cartReceiptsNotified = new ArrayList<>();
        List<CartIOMessage> messagesNotified = new ArrayList<>();

        listReceipts.parallelStream().forEach(cartReceipt -> {
            if (isCartReceiptNotValid(cartReceipt)) {
                logger.info("Cart receipt discarded");
                return;
            }

            NotifyCartResult notifyCartResult = this.cartReceiptToIOService.notifyCart(cartReceipt);

            List<CartIOMessage> cartIOMessages = this.cartReceiptToIOService
                    .verifyNotificationResultAndUpdateCartReceipt(notifyCartResult, cartReceipt);

            messagesNotified.addAll(cartIOMessages);
            cartReceiptsNotified.add(cartReceipt);
        });

        if (!cartReceiptsNotified.isEmpty()) {
            documentReceipts.setValue(cartReceiptsNotified);
        }

        if (!messagesNotified.isEmpty()) {
            documentMessages.setValue(messagesNotified);
        }
    }

    private boolean isCartReceiptNotValid(CartForReceipt cartForReceipt) {
        return cartForReceipt == null
                || cartForReceipt.getPayload() == null
                || cartForReceipt.getPayload().getCart() == null
                || !statusCanBeNotified(cartForReceipt);
    }

    public boolean statusCanBeNotified(CartForReceipt cartForReceipt) {
        return cartForReceipt.getStatus().equals(CartStatusType.GENERATED) ||
                cartForReceipt.getStatus().equals(CartStatusType.SIGNED) ||
                cartForReceipt.getStatus().equals(CartStatusType.IO_NOTIFIER_RETRY);
    }
}
