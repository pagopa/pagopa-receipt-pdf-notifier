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


package it.gov.pagopa.receipt.pdf.notifier.model.generated;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.gov.pagopa.receipt.pdf.notifier.client.generated.JSON;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

/**
 * Payload containing all information needed to retrieve and visualize third party message details
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2023-06-23T14:54:01.440130+02:00[Europe/Rome]")
public class ThirdPartyData {
  public static final String SERIALIZED_NAME_ID = "id";
  @SerializedName(SERIALIZED_NAME_ID)
  private String id;

  public static final String SERIALIZED_NAME_ORIGINAL_SENDER = "original_sender";
  @SerializedName(SERIALIZED_NAME_ORIGINAL_SENDER)
  private String originalSender;

  public static final String SERIALIZED_NAME_ORIGINAL_RECEIPT_DATE = "original_receipt_date";
  @SerializedName(SERIALIZED_NAME_ORIGINAL_RECEIPT_DATE)
  private String originalReceiptDate;

  public static final String SERIALIZED_NAME_HAS_ATTACHMENTS = "has_attachments";
  @SerializedName(SERIALIZED_NAME_HAS_ATTACHMENTS)
  private Boolean hasAttachments = false;

  public static final String SERIALIZED_NAME_SUMMARY = "summary";
  @SerializedName(SERIALIZED_NAME_SUMMARY)
  private String summary;

  public ThirdPartyData() {
  }

  public ThirdPartyData id(String id) {
    
    this.id = id;
    return this;
  }

   /**
   * Unique id for retrieving third party enriched information about the message
   * @return id
  **/
  @javax.annotation.Nonnull
  public String getId() {
    return id;
  }


  public void setId(String id) {
    this.id = id;
  }


  public ThirdPartyData originalSender(String originalSender) {
    
    this.originalSender = originalSender;
    return this;
  }

   /**
   * Either a ServiceId or a simple string representing the sender name
   * @return originalSender
  **/
  @javax.annotation.Nullable
  public String getOriginalSender() {
    return originalSender;
  }


  public void setOriginalSender(String originalSender) {
    this.originalSender = originalSender;
  }


  public ThirdPartyData originalReceiptDate(String originalReceiptDate) {
    
    this.originalReceiptDate = originalReceiptDate;
    return this;
  }

   /**
   * A date-time field in ISO-8601 format and UTC timezone.
   * @return originalReceiptDate
  **/
  @javax.annotation.Nullable
  public String getOriginalReceiptDate() {
    return originalReceiptDate;
  }


  public void setOriginalReceiptDate(String originalReceiptDate) {
    this.originalReceiptDate = originalReceiptDate;
  }


  public ThirdPartyData hasAttachments(Boolean hasAttachments) {
    
    this.hasAttachments = hasAttachments;
    return this;
  }

   /**
   * Get hasAttachments
   * @return hasAttachments
  **/
  @javax.annotation.Nullable
  public Boolean getHasAttachments() {
    return hasAttachments;
  }


  public void setHasAttachments(Boolean hasAttachments) {
    this.hasAttachments = hasAttachments;
  }


  public ThirdPartyData summary(String summary) {
    
    this.summary = summary;
    return this;
  }

   /**
   * Get summary
   * @return summary
  **/
  @javax.annotation.Nullable
  public String getSummary() {
    return summary;
  }


  public void setSummary(String summary) {
    this.summary = summary;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ThirdPartyData thirdPartyData = (ThirdPartyData) o;
    return Objects.equals(this.id, thirdPartyData.id) &&
        Objects.equals(this.originalSender, thirdPartyData.originalSender) &&
        Objects.equals(this.originalReceiptDate, thirdPartyData.originalReceiptDate) &&
        Objects.equals(this.hasAttachments, thirdPartyData.hasAttachments) &&
        Objects.equals(this.summary, thirdPartyData.summary);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, originalSender, originalReceiptDate, hasAttachments, summary);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ThirdPartyData {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    originalSender: ").append(toIndentedString(originalSender)).append("\n");
    sb.append("    originalReceiptDate: ").append(toIndentedString(originalReceiptDate)).append("\n");
    sb.append("    hasAttachments: ").append(toIndentedString(hasAttachments)).append("\n");
    sb.append("    summary: ").append(toIndentedString(summary)).append("\n");
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
    openapiFields.add("id");
    openapiFields.add("original_sender");
    openapiFields.add("original_receipt_date");
    openapiFields.add("has_attachments");
    openapiFields.add("summary");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
    openapiRequiredFields.add("id");
  }

 /**
  * Validates the JSON Object and throws an exception if issues found
  *
  * @param jsonObj JSON Object
  * @throws IOException if the JSON Object is invalid with respect to ThirdPartyData
  */
  public static void validateJsonObject(JsonObject jsonObj) throws IOException {
      if (jsonObj == null) {
        if (!ThirdPartyData.openapiRequiredFields.isEmpty()) { // has required fields but JSON object is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in ThirdPartyData is not found in the empty JSON string", ThirdPartyData.openapiRequiredFields.toString()));
        }
      }

      Set<Entry<String, JsonElement>> entries = jsonObj.entrySet();
      // check to see if the JSON string contains additional fields
      for (Entry<String, JsonElement> entry : entries) {
        if (!ThirdPartyData.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `ThirdPartyData` properties. JSON: %s", entry.getKey(), jsonObj.toString()));
        }
      }

      // check to make sure all required properties/fields are present in the JSON string
      for (String requiredField : ThirdPartyData.openapiRequiredFields) {
        if (jsonObj.get(requiredField) == null) {
          throw new IllegalArgumentException(String.format("The required field `%s` is not found in the JSON string: %s", requiredField, jsonObj.toString()));
        }
      }
      if (!jsonObj.get("id").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `id` to be a primitive type in the JSON string but got `%s`", jsonObj.get("id").toString()));
      }
      if ((jsonObj.get("original_sender") != null && !jsonObj.get("original_sender").isJsonNull()) && !jsonObj.get("original_sender").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `original_sender` to be a primitive type in the JSON string but got `%s`", jsonObj.get("original_sender").toString()));
      }
      if ((jsonObj.get("original_receipt_date") != null && !jsonObj.get("original_receipt_date").isJsonNull()) && !jsonObj.get("original_receipt_date").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `original_receipt_date` to be a primitive type in the JSON string but got `%s`", jsonObj.get("original_receipt_date").toString()));
      }
      if ((jsonObj.get("summary") != null && !jsonObj.get("summary").isJsonNull()) && !jsonObj.get("summary").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `summary` to be a primitive type in the JSON string but got `%s`", jsonObj.get("summary").toString()));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!ThirdPartyData.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'ThirdPartyData' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<ThirdPartyData> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(ThirdPartyData.class));

       return (TypeAdapter<T>) new TypeAdapter<ThirdPartyData>() {
           @Override
           public void write(JsonWriter out, ThirdPartyData value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public ThirdPartyData read(JsonReader in) throws IOException {
             JsonObject jsonObj = elementAdapter.read(in).getAsJsonObject();
             validateJsonObject(jsonObj);
             return thisAdapter.fromJsonTree(jsonObj);
           }

       }.nullSafe();
    }
  }

 /**
  * Create an instance of ThirdPartyData given an JSON string
  *
  * @param jsonString JSON string
  * @return An instance of ThirdPartyData
  * @throws IOException if the JSON string is invalid with respect to ThirdPartyData
  */
  public static ThirdPartyData fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, ThirdPartyData.class);
  }

 /**
  * Convert an instance of ThirdPartyData to an JSON string
  *
  * @return JSON string
  */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}
