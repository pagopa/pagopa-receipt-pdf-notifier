/*
 * IO API for Public Administration Services
 * # Warning **This is an experimental API that is (most probably) going to change as we evolve the IO platform.** # Introduction This is the documentation of the IO API for 3rd party services. This API enables Public Administration services to integrate with the IO platform. IO enables services to communicate with Italian citizens via the [IO app](https://io.italia.it/). # How to get an API key To get access to this API, you'll need to register on the [IO Developer Portal](https://developer.io.italia.it/). After the registration step, you have to click on the button that says `subscribe to the digital citizenship api` to receive the API key that you will use to authenticate the API calls. You will also receive an email with further instructions, including a fake Fiscal Code that you will be able to use to send test messages. Messages sent to the fake Fiscal Code will be notified to the email address used during the registration process on the developer portal. # Messages ## What is a message Messages are the primary form of communication enabled by the IO APIs. Messages are **personal** communications directed to a **specific citizen**. You will not be able to use this API to broadcast a message to a group of citizens, you will have to create and send a specific, personalized message to each citizen you want to communicate to. The recipient of the message (i.e. a citizen) is identified trough his [Fiscal Code](https://it.wikipedia.org/wiki/Codice_fiscale). ## Message format A message is conceptually very similar to an email and, in its simplest form, is composed of the following attributes:    * A required `subject`: a short description of the topic.   * A required `markdown` body: a Markdown representation of the body (see     below on what Markdown tags are allowed).   * An optional `payment_data`: in case the message is a payment request,     the _payment data_ will enable the recipient to pay the requested amount     via [PagoPA](https://www.agid.gov.it/it/piattaforme/pagopa).   * An optional `due_date`: a _due date_ that let the recipient     add a reminder when receiving the message. The format for all     dates is [ISO8601](https://it.wikipedia.org/wiki/ISO_8601) with time     information and UTC timezone (ie. \"2018-10-13T00:00:00.000Z\").   * An optional `feature_level_type`: the kind of the submitted message.      It can be:     - `STANDARD` for normal messages;     - `ADVANCED` to enable premium features.      Default is `STANDARD`.  ## Allowed Markdown formatting Not all Markdown formatting is currently available. Currently you can use the following formatting:    * Headings   * Text stylings (bold, italic, etc...)   * Lists (bullet and numbered)  ## Sending a message to a citizen Not every citizen will be interested in what you have to say and not every citizen you want to communicate to will be registered on IO. For this reason, before sending a message you need to check whether the recipient is registered on the platform and that he has not yet opted out from receiving messages from you. The process for sending a message is made of 3 steps:    1. Call [getProfile](#operation/getProfile): if the profile does not exist      (i.e. you get a 404 response) or if the recipient has opted-out from      your service (the response contains `sender_allowed: false`), you      cannot send the message and you must stop here.   1. Call [submitMessageforUser](#operation/submitMessageforUser) to submit      a new message.   1. (optional) Call [getMessage](#operation/getMessage) to check whether      the message has been notified to the recipient. 
 *
 * The version of the OpenAPI document: 3.30.3
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package it.gov.pagopa.receipt.pdf.notifier.client.generated.api;

import it.gov.pagopa.receipt.pdf.notifier.client.generated.ApiException;
import it.gov.pagopa.receipt.pdf.notifier.client.generated.ApiResponse;
import it.gov.pagopa.receipt.pdf.notifier.model.generated.CreatedMessage;
import it.gov.pagopa.receipt.pdf.notifier.model.generated.FiscalCodePayload;
import it.gov.pagopa.receipt.pdf.notifier.model.generated.LimitedProfile;
import it.gov.pagopa.receipt.pdf.notifier.model.generated.NewMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * API tests for DefaultApi
 */
@Disabled
class DefaultApiTest {

    private final DefaultApi api = new DefaultApi();

    /**
     * Get a User Profile using POST
     *
     * Returns the preferences for the user identified by the fiscal code provided in the request body. The field &#x60;sender_allowed&#x60; is set fo &#x60;false&#x60; in case the service which is calling the API has been disabled by the user.
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    void getProfileByPOSTTest() throws ApiException {
        FiscalCodePayload payload = null;
        ApiResponse<LimitedProfile> response = api.getProfileByPOSTWithHttpInfo(payload);
        // TODO: test validations
    }

    /**
     * Submit a Message passing the user fiscal_code in the request body
     *
     * Submits a message to a user with STANDARD or ADVANCED features based on &#x60;feature_level_type&#x60; value. On error, the reason is returned in the response payload. In order to call &#x60;submitMessageforUser&#x60;, before sending any message, the sender MUST call &#x60;getProfile&#x60; and check that the profile exists (for the specified fiscal code) and that the &#x60;sender_allowed&#x60; field of the user&#39;s profile it set to &#x60;true&#x60;.
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    void submitMessageforUserWithFiscalCodeInBodyTest() throws ApiException {
        NewMessage message = null;
        ApiResponse<CreatedMessage> response = api.submitMessageforUserWithFiscalCodeInBodyWithHttpInfo(message);
        // TODO: test validations
    }

}
