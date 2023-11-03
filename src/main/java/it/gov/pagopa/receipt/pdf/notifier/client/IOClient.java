package it.gov.pagopa.receipt.pdf.notifier.client;

import it.gov.pagopa.receipt.pdf.notifier.exception.IOAPIException;
import it.gov.pagopa.receipt.pdf.notifier.model.io.IOProfilePayload;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.MessagePayload;

import java.net.http.HttpResponse;

/**
 * Client for invoking IO APIs
 */
public interface IOClient {

    /**
     * Get a User Profile using POST
     * Returns the preferences for the user identified by the fiscal code provided in the request body.
     * The field &#x60;sender_allowed&#x60; is set fo &#x60;false&#x60; in case the service which is calling the API has been disabled by the user.
     *
     * @param fiscalCodePayload the {@link IOProfilePayload} serialized as String
     * @return the {@link HttpResponse} of the IO API
     * @throws IOAPIException If fail to call the API
     */
    HttpResponse<String> getProfile(String fiscalCodePayload) throws IOAPIException;

    /**
     * Submit a Message passing the user fiscal_code in the request body
     * Submits a message to a user with STANDARD or ADVANCED features based on &#x60;feature_level_type&#x60; value.
     * On error, the reason is returned in the response payload. In order to call &#x60;submitMessageforUser&#x60;,
     * before sending any message, the sender MUST call &#x60;getProfile&#x60; and check that the profile exists (for the specified fiscal code) and that the &#x60;sender_allowed&#x60; field of the user&#39;s profile it set to &#x60;true&#x60;.
     *
     * @param messagePayload the {@link MessagePayload} serialized as String
     * @return the {@link HttpResponse} of the IO API
     * @throws IOAPIException If fail to call the API
     */
    HttpResponse<String> submitMessage(String messagePayload) throws IOAPIException;
}
