package it.gov.pagopa.receipt.pdf.notifier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserNotifyStatus;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;

import java.util.EnumMap;
import java.util.List;

public interface ReceiptToIOService {
    UserNotifyStatus notifyMessage(String fiscalCode, UserType userType, Receipt receipt);

    boolean verifyMessagesNotification(
            EnumMap<UserType, UserNotifyStatus> usersToBeVerified,
            List<IOMessage> messagesNotified,
            Receipt receipt
    ) throws JsonProcessingException;
}
