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


package it.gov.pagopa.receipt.pdf.notifier.generated.model;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.gov.pagopa.receipt.pdf.notifier.generated.client.JSON;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

/**
 * MessageContent
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2023-06-23T14:54:01.440130+02:00[Europe/Rome]")
public class MessageContent {
  public static final String SERIALIZED_NAME_SUBJECT = "subject";
  @SerializedName(SERIALIZED_NAME_SUBJECT)
  private String subject;

  public static final String SERIALIZED_NAME_MARKDOWN = "markdown";
  @SerializedName(SERIALIZED_NAME_MARKDOWN)
  private String markdown;

  public static final String SERIALIZED_NAME_PAYMENT_DATA = "payment_data";
  @SerializedName(SERIALIZED_NAME_PAYMENT_DATA)
  private PaymentData paymentData;

  public static final String SERIALIZED_NAME_THIRD_PARTY_DATA = "third_party_data";
  @SerializedName(SERIALIZED_NAME_THIRD_PARTY_DATA)
  private ThirdPartyData thirdPartyData;

  public static final String SERIALIZED_NAME_DUE_DATE = "due_date";
  @SerializedName(SERIALIZED_NAME_DUE_DATE)
  private String dueDate;

  public MessageContent() {
  }

  public MessageContent subject(String subject) {
    
    this.subject = subject;
    return this;
  }

   /**
   * The (optional) subject of the message - note that only some notification channels support the display of a subject. When a subject is not provided, one gets generated from the client attributes.
   * @return subject
  **/
  @javax.annotation.Nonnull
  public String getSubject() {
    return subject;
  }


  public void setSubject(String subject) {
    this.subject = subject;
  }


  public MessageContent markdown(String markdown) {
    
    this.markdown = markdown;
    return this;
  }

   /**
   * The full version of the message, in plain text or Markdown format. The content of this field will be delivered to channels that don&#39;t have any limit in terms of content size (e.g. email, etc...).
   * @return markdown
  **/
  @javax.annotation.Nonnull
  public String getMarkdown() {
    return markdown;
  }


  public void setMarkdown(String markdown) {
    this.markdown = markdown;
  }


  public MessageContent paymentData(PaymentData paymentData) {
    
    this.paymentData = paymentData;
    return this;
  }

   /**
   * Get paymentData
   * @return paymentData
  **/
  @javax.annotation.Nullable
  public PaymentData getPaymentData() {
    return paymentData;
  }


  public void setPaymentData(PaymentData paymentData) {
    this.paymentData = paymentData;
  }

  public MessageContent thirdPartyData(ThirdPartyData thirdPartyData) {
    
    this.thirdPartyData = thirdPartyData;
    return this;
  }

   /**
   * Get thirdPartyData
   * @return thirdPartyData
  **/
  @javax.annotation.Nullable
  public ThirdPartyData getThirdPartyData() {
    return thirdPartyData;
  }


  public void setThirdPartyData(ThirdPartyData thirdPartyData) {
    this.thirdPartyData = thirdPartyData;
  }


  public MessageContent dueDate(String dueDate) {
    
    this.dueDate = dueDate;
    return this;
  }

   /**
   * A date-time field in ISO-8601 format and UTC timezone.
   * @return dueDate
  **/
  @javax.annotation.Nullable
  public String getDueDate() {
    return dueDate;
  }


  public void setDueDate(String dueDate) {
    this.dueDate = dueDate;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MessageContent messageContent = (MessageContent) o;
    return Objects.equals(this.subject, messageContent.subject) &&
        Objects.equals(this.markdown, messageContent.markdown) &&
        Objects.equals(this.paymentData, messageContent.paymentData) &&
        Objects.equals(this.thirdPartyData, messageContent.thirdPartyData) &&
        Objects.equals(this.dueDate, messageContent.dueDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, markdown, paymentData, thirdPartyData, dueDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MessageContent {\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    markdown: ").append(toIndentedString(markdown)).append("\n");
    sb.append("    paymentData: ").append(toIndentedString(paymentData)).append("\n");
    sb.append("    thirdPartyData: ").append(toIndentedString(thirdPartyData)).append("\n");
    sb.append("    dueDate: ").append(toIndentedString(dueDate)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }


  public static HashSet<String> openapiFields;
  public static HashSet<String> openapiRequiredFields;

  static {
    // a set of all properties/fields (JSON key names)
    openapiFields = new HashSet<String>();
    openapiFields.add("subject");
    openapiFields.add("markdown");
    openapiFields.add("payment_data");
    openapiFields.add("prescription_data");
    openapiFields.add("legal_data");
    openapiFields.add("eu_covid_cert");
    openapiFields.add("third_party_data");
    openapiFields.add("due_date");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
    openapiRequiredFields.add("subject");
    openapiRequiredFields.add("markdown");
  }

 /**
  * Validates the JSON Object and throws an exception if issues found
  *
  * @param jsonObj JSON Object
  * @throws IOException if the JSON Object is invalid with respect to MessageContent
  */
  public static void validateJsonObject(JsonObject jsonObj) throws IOException {
      if (jsonObj == null) {
        if (!MessageContent.openapiRequiredFields.isEmpty()) { // has required fields but JSON object is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in MessageContent is not found in the empty JSON string", MessageContent.openapiRequiredFields.toString()));
        }
      }

      Set<Entry<String, JsonElement>> entries = jsonObj.entrySet();
      // check to see if the JSON string contains additional fields
      for (Entry<String, JsonElement> entry : entries) {
        if (!MessageContent.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `MessageContent` properties. JSON: %s", entry.getKey(), jsonObj.toString()));
        }
      }

      // check to make sure all required properties/fields are present in the JSON string
      for (String requiredField : MessageContent.openapiRequiredFields) {
        if (jsonObj.get(requiredField) == null) {
          throw new IllegalArgumentException(String.format("The required field `%s` is not found in the JSON string: %s", requiredField, jsonObj.toString()));
        }
      }
      if (!jsonObj.get("subject").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `subject` to be a primitive type in the JSON string but got `%s`", jsonObj.get("subject").toString()));
      }
      if (!jsonObj.get("markdown").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `markdown` to be a primitive type in the JSON string but got `%s`", jsonObj.get("markdown").toString()));
      }
      // validate the optional field `payment_data`
      if (jsonObj.get("payment_data") != null && !jsonObj.get("payment_data").isJsonNull()) {
        PaymentData.validateJsonObject(jsonObj.getAsJsonObject("payment_data"));
      }
      // validate the optional field `third_party_data`
      if (jsonObj.get("third_party_data") != null && !jsonObj.get("third_party_data").isJsonNull()) {
        ThirdPartyData.validateJsonObject(jsonObj.getAsJsonObject("third_party_data"));
      }
      if ((jsonObj.get("due_date") != null && !jsonObj.get("due_date").isJsonNull()) && !jsonObj.get("due_date").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `due_date` to be a primitive type in the JSON string but got `%s`", jsonObj.get("due_date").toString()));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!MessageContent.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'MessageContent' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<MessageContent> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(MessageContent.class));

       return (TypeAdapter<T>) new TypeAdapter<MessageContent>() {
           @Override
           public void write(JsonWriter out, MessageContent value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public MessageContent read(JsonReader in) throws IOException {
             JsonObject jsonObj = elementAdapter.read(in).getAsJsonObject();
             validateJsonObject(jsonObj);
             return thisAdapter.fromJsonTree(jsonObj);
           }

       }.nullSafe();
    }
  }

 /**
  * Create an instance of MessageContent given an JSON string
  *
  * @param jsonString JSON string
  * @return An instance of MessageContent
  * @throws IOException if the JSON string is invalid with respect to MessageContent
  */
  public static MessageContent fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, MessageContent.class);
  }

 /**
  * Convert an instance of MessageContent to an JSON string
  *
  * @return JSON string
  */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

