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
     * Get a User Profile.
     * <p>
     * Returns the preferences for the user identified by the fiscal code provided in the request body.
     * The field <code>sender_allowed</code> is set to <code>false</code> in case the service which is
     * calling the API has been disabled by the user.
     *
     * @param fiscalCodePayload the {@link IOProfilePayload} serialized as String
     * @return the {@link HttpResponse} of the IO API
     * @throws IOAPIException If fail to call the API
     */
    HttpResponse<String> getProfile(String fiscalCodePayload) throws IOAPIException;

    /**
     * Submit a Message passing the user fiscal code in the request body
     * <p>
     * Submits a message to a user with STANDARD or ADVANCED features based on <code>feature_level_type</code> value.
     * On error, the reason is returned in the response payload. In order to call <code>submitMessageforUser</code>,
     * before sending any message, the sender MUST call <code>getProfile</code> and check that the profile exists
     * (for the specified fiscal code) and that the <code>sender_allowed</code> field of the user&#39;s profile it is
     * set to <code>true</code>.
     *
     * @param messagePayload the {@link MessagePayload} serialized as String
     * @return the {@link HttpResponse} of the IO API
     * @throws IOAPIException If fail to call the API
     */
    HttpResponse<String> submitMessage(String messagePayload) throws IOAPIException;
}
