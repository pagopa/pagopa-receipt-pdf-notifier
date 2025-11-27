package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import it.gov.pagopa.receipt.pdf.notifier.client.IOClient;
import it.gov.pagopa.receipt.pdf.notifier.exception.IOAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReasonErrorCode.ERROR_IO_API_IO;
import static it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReasonErrorCode.ERROR_IO_API_UNEXPECTED;

/**
 * {@inheritDoc}
 */
public class IOClientImpl implements IOClient {

    private final Logger logger = LoggerFactory.getLogger(IOClientImpl.class);

    private static final String IO_API_BASE_PATH = System.getenv().getOrDefault("IO_API_BASE_PATH", "https://api.uat.platform.pagopa.it/mock-io/api/v1");
    private static final String IO_API_PROFILES_PATH = System.getenv().getOrDefault("IO_API_PROFILES_PATH", "/profiles");
    private static final String IO_API_MESSAGES_PATH = System.getenv().getOrDefault("IO_API_MESSAGES_PATH", "/messages");
    private static final String OCP_APIM_SUBSCRIPTION_KEY = System.getenv().getOrDefault("OCP_APIM_SUBSCRIPTION_KEY", "");
    private static final String OCP_APIM_HEADER_KEY = System.getenv().getOrDefault("OCP_APIM_HEADER_KEY", "Ocp-Apim-Subscription-Key");

    private static final String CONTENT_TYPE_JSON = "application/json";

    private final HttpClient client;

    private static IOClient instance = null;

    public static IOClient getInstance() {
        if (instance == null) {
            instance = new IOClientImpl();
        }
        return instance;
    }

    private IOClientImpl() {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    IOClientImpl(HttpClient client) {
        this.client = client;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpResponse<String> getProfile(String fiscalCodePayload) throws IOAPIException {
        String uri = String.format("%s%s", IO_API_BASE_PATH, IO_API_PROFILES_PATH);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .version(HttpClient.Version.HTTP_2)
                .header("Content-Type", CONTENT_TYPE_JSON)
                .header(OCP_APIM_HEADER_KEY, OCP_APIM_SUBSCRIPTION_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(fiscalCodePayload))
                .build();

        return makeCall(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpResponse<String> submitMessage(String messagePayload) throws IOAPIException {
        String uri = String.format("%s%s", IO_API_BASE_PATH, IO_API_MESSAGES_PATH);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .version(HttpClient.Version.HTTP_2)
                .header("Content-Type", CONTENT_TYPE_JSON)
                .header(OCP_APIM_HEADER_KEY, OCP_APIM_SUBSCRIPTION_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(messagePayload))
                .build();

        return makeCall(request);
    }

    private HttpResponse<String> makeCall(HttpRequest request) throws IOAPIException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IOAPIException("I/O error when invoking IO API", ERROR_IO_API_IO.getCode(), e);
        } catch (InterruptedException e) {
            logger.warn("This thread was interrupted, restoring the state");
            Thread.currentThread().interrupt();
            throw new IOAPIException("Unexpected error when invoking IO API, the thread was interrupted", ERROR_IO_API_UNEXPECTED.getCode(), e);
        }
    }
}
