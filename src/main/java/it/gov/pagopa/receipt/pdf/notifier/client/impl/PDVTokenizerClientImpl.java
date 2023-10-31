package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import it.gov.pagopa.receipt.pdf.notifier.client.PDVTokenizerClient;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * {@inheritDoc}
 */
public class PDVTokenizerClientImpl implements PDVTokenizerClient {

    private final Logger logger = LoggerFactory.getLogger(PDVTokenizerClientImpl.class);

    private static final String BASE_PATH = System.getenv().getOrDefault("PDV_TOKENIZER_BASE_PATH", "https://api.uat.tokenizer.pdv.pagopa.it/tokenizer/v1");
    private static final String SUBSCRIPTION_KEY = System.getenv().getOrDefault("PDV_TOKENIZER_SUBSCRIPTION_KEY", "");
    private static final String SUBSCRIPTION_KEY_HEADER =  System.getenv().getOrDefault("OCP_APIM_HEADER_KEY", "Ocp-Apim-Subscription-Key");
    private static final String SEARCH_TOKEN_ENDPOINT = System.getenv().getOrDefault("PDV_TOKENIZER_SEARCH_TOKEN_ENDPOINT", "/tokens/search");
    private static final String FIND_PII_ENDPOINT = System.getenv().getOrDefault("PDV_TOKENIZER_FIND_PII_ENDPOINT", "/tokens/%s/pii");
    private static final String CREATE_TOKEN_ENDPOINT = System.getenv().getOrDefault("PDV_TOKENIZER_CREATE_TOKEN_ENDPOINT", "/tokens");
    private static final int PDV_IO_ERROR = 800;
    private static final int PDV_UNEXPECTED_ERROR = 801;

    private final HttpClient client;

    private static PDVTokenizerClientImpl instance;

    public static PDVTokenizerClientImpl getInstance() {
        if (instance == null) {
            instance = new PDVTokenizerClientImpl();
        }
        return instance;
    }

    private PDVTokenizerClientImpl() {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    PDVTokenizerClientImpl(HttpClient client) {
        this.client = client;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpResponse<String> searchTokenByPII(String piiBody) throws PDVTokenizerException {
        String uri = String.format("%s%s", BASE_PATH, SEARCH_TOKEN_ENDPOINT);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .version(HttpClient.Version.HTTP_2)
                .header(SUBSCRIPTION_KEY_HEADER, SUBSCRIPTION_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(piiBody))
                .build();

        return makeCall(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpResponse<String> findPIIByToken(String token) throws PDVTokenizerException {
        String endpoint = String.format(FIND_PII_ENDPOINT, token);
        String uri = String.format("%s%s", BASE_PATH, endpoint);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .version(HttpClient.Version.HTTP_2)
                .header(SUBSCRIPTION_KEY_HEADER, SUBSCRIPTION_KEY)
                .build();

        return makeCall(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpResponse<String> createToken(String piiBody) throws PDVTokenizerException {
        String uri = String.format("%s%s", BASE_PATH, CREATE_TOKEN_ENDPOINT);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .version(HttpClient.Version.HTTP_2)
                .header(SUBSCRIPTION_KEY_HEADER, SUBSCRIPTION_KEY)
                .PUT(HttpRequest.BodyPublishers.ofString(piiBody))
                .build();

        return makeCall(request);
    }

    private HttpResponse<String> makeCall(HttpRequest request) throws PDVTokenizerException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new PDVTokenizerException("I/O error when invoking PDV Tokenizer", PDV_IO_ERROR, e);
        } catch (InterruptedException e) {
            logger.warn("This thread was interrupted, restoring the state");
            Thread.currentThread().interrupt();
            throw new PDVTokenizerException("Unexpected error when invoking PDV Tokenizer, the thread was interrupted", PDV_UNEXPECTED_ERROR, e);
        }
    }
}
