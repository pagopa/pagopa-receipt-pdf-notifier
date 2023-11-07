package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerUnexpectedException;
import it.gov.pagopa.receipt.pdf.notifier.service.PDVTokenizerService;
import it.gov.pagopa.receipt.pdf.notifier.service.PDVTokenizerServiceRetryWrapper;

/**
 * {@inheritDoc}
 */
public class PDVTokenizerServiceRetryWrapperImpl implements PDVTokenizerServiceRetryWrapper {

    private static final Long INITIAL_INTERVAL = Long.parseLong(System.getenv().getOrDefault("PDV_TOKENIZER_INITIAL_INTERVAL", "1000"));
    private static final Double MULTIPLIER = Double.parseDouble(System.getenv().getOrDefault("PDV_TOKENIZER_MULTIPLIER", "2.0"));
    private static final Double RANDOMIZATION_FACTOR = Double.parseDouble(System.getenv().getOrDefault("PDV_TOKENIZER_RANDOMIZATION_FACTOR", "0.6"));
    private static final Integer MAX_RETRIES = Integer.parseInt(System.getenv().getOrDefault("PDV_TOKENIZER_MAX_RETRIES", "4"));

    private final PDVTokenizerService pdvTokenizerService;
    private final Retry retry;

    PDVTokenizerServiceRetryWrapperImpl(PDVTokenizerService pdvTokenizerService, Retry retry) {
        this.pdvTokenizerService = pdvTokenizerService;
        this.retry = retry;
    }

    public PDVTokenizerServiceRetryWrapperImpl() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(MAX_RETRIES)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(INITIAL_INTERVAL, MULTIPLIER, RANDOMIZATION_FACTOR))
                .retryOnException(e -> (e instanceof PDVTokenizerException tokenizerException) && tokenizerException.getStatusCode() == 429)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);

        this.pdvTokenizerService = new PDVTokenizerServiceImpl();
        this.retry = registry.retry("tokenizerRetry");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenWithRetry(String fiscalCode) throws JsonProcessingException, PDVTokenizerException {
        CheckedFunction<String, String> function = Retry.decorateCheckedFunction(retry, pdvTokenizerService::getToken);
        return runFunction(fiscalCode, function);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFiscalCodeWithRetry(String token) throws PDVTokenizerException, JsonProcessingException {
        CheckedFunction<String, String> function = Retry.decorateCheckedFunction(retry, pdvTokenizerService::getFiscalCode);
        return runFunction(token, function);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateTokenForFiscalCodeWithRetry(String fiscalCode) throws PDVTokenizerException, JsonProcessingException {
        CheckedFunction<String, String> function = Retry.decorateCheckedFunction(retry, pdvTokenizerService::generateTokenForFiscalCode);
        return runFunction(fiscalCode, function);
    }

    private String runFunction(String fiscalCode, CheckedFunction<String, String> function) throws PDVTokenizerException, JsonProcessingException {
        try {
            return function.apply(fiscalCode);
        } catch (Throwable e) {
            if (e instanceof PDVTokenizerException tokenizerException) {
                throw tokenizerException;
            }
            if (e instanceof JsonProcessingException jsonProcessingException) {
                throw jsonProcessingException;
            }
            throw new PDVTokenizerUnexpectedException(e);
        }
    }
}
