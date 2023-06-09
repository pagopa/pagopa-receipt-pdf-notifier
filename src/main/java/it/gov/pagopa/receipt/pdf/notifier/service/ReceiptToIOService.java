package it.gov.pagopa.receipt.pdf.notifier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    boolean verifyMessagesNotification(
            Map<String, UserNotifyStatus> usersToBeVerified,
            List<IOMessage> messagesNotified,
            Receipt receipt,
            Logger logger
    ) throws JsonProcessingException;
}
