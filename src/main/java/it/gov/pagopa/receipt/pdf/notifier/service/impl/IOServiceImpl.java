package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.client.IOClient;
import it.gov.pagopa.receipt.pdf.notifier.client.impl.IOClientImpl;
import it.gov.pagopa.receipt.pdf.notifier.exception.ErrorToNotifyException;
import it.gov.pagopa.receipt.pdf.notifier.exception.IOAPIException;
import it.gov.pagopa.receipt.pdf.notifier.model.io.IOProfilePayload;
import it.gov.pagopa.receipt.pdf.notifier.model.io.IOProfileResponse;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.IOMessageResponse;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.MessagePayload;
import it.gov.pagopa.receipt.pdf.notifier.service.IOService;
import it.gov.pagopa.receipt.pdf.notifier.utils.ObjectMapperUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;

/**
 * {@inheritDoc}
 */
public class IOServiceImpl implements IOService {

    private final Logger logger = LoggerFactory.getLogger(IOServiceImpl.class);

    private final IOClient ioClient;

    public IOServiceImpl() {
        this.ioClient = IOClientImpl.getInstance();
    }

    public IOServiceImpl(IOClient ioClient) {
        this.ioClient = ioClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNotifyToIOUserAllowed(String fiscalCode) throws IOAPIException, ErrorToNotifyException {
        IOProfilePayload iOProfilePayload = IOProfilePayload.builder().fiscalCode(fiscalCode).build();
        String payload = serializePayload(iOProfilePayload);

        logger.debug("IO API getProfile called");
        HttpResponse<String> getProfileResponse = this.ioClient.getProfile(payload);
        logger.debug("IO API getProfile invocation completed");

        if (getProfileResponse == null) {
            throw new ErrorToNotifyException("IO /profiles failed to respond");
        }

        if (getProfileResponse.statusCode() == HttpStatus.SC_NOT_FOUND) {
            return false;
        }

        if (getProfileResponse.statusCode() != HttpStatus.SC_OK || getProfileResponse.body() == null) {
            String errorMsg = String.format("IO /profiles responded with code %s", getProfileResponse.statusCode());
            throw new ErrorToNotifyException(errorMsg);
        }

        IOProfileResponse ioProfileResponse = deserializeResponse(getProfileResponse.body(), IOProfileResponse.class);
        return ioProfileResponse.isSenderAllowed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String sendNotificationToIOUser(MessagePayload message) throws IOAPIException, ErrorToNotifyException {
        String payload = serializePayload(message);

        logger.debug("IO API submitMessage called");
        HttpResponse<String> notificationResponse = this.ioClient.submitMessage(payload);
        logger.debug("IO API submitMessage invocation completed");

        if (notificationResponse == null) {
            throw new ErrorToNotifyException("IO /messages failed to respond");
        }
        if (notificationResponse.statusCode() != HttpStatus.SC_CREATED || notificationResponse.body() == null) {
            String errorMsg = String.format("IO /messages responded with code %s", notificationResponse.statusCode());
            throw new ErrorToNotifyException(errorMsg);
        }

        IOMessageResponse ioMessageResponse = deserializeResponse(notificationResponse.body(), IOMessageResponse.class);
        return ioMessageResponse.getId();
    }

    private String serializePayload(Object payload) throws ErrorToNotifyException {
        try {
            return ObjectMapperUtils.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ErrorToNotifyException("Failed to serialize payload for IO API invocation", e);
        }
    }

    private <T> T deserializeResponse(String response, Class<T> clazz) throws ErrorToNotifyException {
        try {
            return ObjectMapperUtils.mapString(response, clazz);
        } catch (JsonProcessingException e) {
            throw new ErrorToNotifyException("Failed to deserialize response of IO API invocation", e);
        }
    }
}
