package it.gov.pagopa.receipt.pdf.notifier.service;

import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public interface ReceiptToIOService {
    void notifyMessage(Map<String, UserNotifyStatus> usersToBeVerified,
                       String fiscalCode,
                       UserType userType,
                       Receipt receipt,
                       Logger logger);

    int verifyMessagesNotification(
            Map<String, UserNotifyStatus> usersToBeVerified,
            List<IOMessage> messagesNotified,
            Receipt receipt,
            Logger logger
    );
}
