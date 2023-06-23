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
 * PaymentData
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2023-06-23T14:54:01.440130+02:00[Europe/Rome]")
public class PaymentData {
  public static final String SERIALIZED_NAME_AMOUNT = "amount";
  @SerializedName(SERIALIZED_NAME_AMOUNT)
  private Integer amount;

  public static final String SERIALIZED_NAME_NOTICE_NUMBER = "notice_number";
  @SerializedName(SERIALIZED_NAME_NOTICE_NUMBER)
  private String noticeNumber;

  public static final String SERIALIZED_NAME_INVALID_AFTER_DUE_DATE = "invalid_after_due_date";
  @SerializedName(SERIALIZED_NAME_INVALID_AFTER_DUE_DATE)
  private Boolean invalidAfterDueDate = false;

  public static final String SERIALIZED_NAME_PAYEE = "payee";
  @SerializedName(SERIALIZED_NAME_PAYEE)
  private Payee payee;

  public PaymentData() {
  }

  public PaymentData amount(Integer amount) {
    
    this.amount = amount;
    return this;
  }

   /**
   * Amount of payment in euro cent. PagoPA accepts up to 9999999999 euro cents.
   * minimum: 1
   * maximum: 9999999999
   * @return amount
  **/
  @javax.annotation.Nonnull
  public Integer getAmount() {
    return amount;
  }


  public void setAmount(Integer amount) {
    this.amount = amount;
  }


  public PaymentData noticeNumber(String noticeNumber) {
    
    this.noticeNumber = noticeNumber;
    return this;
  }

   /**
   * The field [\&quot;Numero Avviso\&quot;](https://pagopa-specifichepagamenti.readthedocs.io/it/latest/_docs/Capitolo7.html#il-numero-avviso-e-larchivio-dei-pagamenti-in-attesa) of pagoPa, needed to identify the payment. Format is &#x60;&lt;aux digit (1n)&gt;[&lt;application code&gt; (2n)]&lt;codice IUV (15|17n)&gt;&#x60;. See [pagoPa specs](https://www.agid.gov.it/sites/default/files/repository_files/specifiche_attuative_pagamenti_1_3_1_0.pdf) for more info on this field and the IUV.
   * @return noticeNumber
  **/
  @javax.annotation.Nonnull
  public String getNoticeNumber() {
    return noticeNumber;
  }


  public void setNoticeNumber(String noticeNumber) {
    this.noticeNumber = noticeNumber;
  }


  public PaymentData invalidAfterDueDate(Boolean invalidAfterDueDate) {
    
    this.invalidAfterDueDate = invalidAfterDueDate;
    return this;
  }

   /**
   * Get invalidAfterDueDate
   * @return invalidAfterDueDate
  **/
  @javax.annotation.Nullable
  public Boolean getInvalidAfterDueDate() {
    return invalidAfterDueDate;
  }


  public void setInvalidAfterDueDate(Boolean invalidAfterDueDate) {
    this.invalidAfterDueDate = invalidAfterDueDate;
  }


  public PaymentData payee(Payee payee) {
    
    this.payee = payee;
    return this;
  }

   /**
   * Get payee
   * @return payee
  **/
  @javax.annotation.Nullable
  public Payee getPayee() {
    return payee;
  }


  public void setPayee(Payee payee) {
    this.payee = payee;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PaymentData paymentData = (PaymentData) o;
    return Objects.equals(this.amount, paymentData.amount) &&
        Objects.equals(this.noticeNumber, paymentData.noticeNumber) &&
        Objects.equals(this.invalidAfterDueDate, paymentData.invalidAfterDueDate) &&
        Objects.equals(this.payee, paymentData.payee);
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount, noticeNumber, invalidAfterDueDate, payee);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PaymentData {\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    noticeNumber: ").append(toIndentedString(noticeNumber)).append("\n");
    sb.append("    invalidAfterDueDate: ").append(toIndentedString(invalidAfterDueDate)).append("\n");
    sb.append("    payee: ").append(toIndentedString(payee)).append("\n");
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
    openapiFields.add("amount");
    openapiFields.add("notice_number");
    openapiFields.add("invalid_after_due_date");
    openapiFields.add("payee");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
    openapiRequiredFields.add("amount");
    openapiRequiredFields.add("notice_number");
  }

 /**
  * Validates the JSON Object and throws an exception if issues found
  *
  * @param jsonObj JSON Object
  * @throws IOException if the JSON Object is invalid with respect to PaymentData
  */
  public static void validateJsonObject(JsonObject jsonObj) throws IOException {
      if (jsonObj == null) {
        if (!PaymentData.openapiRequiredFields.isEmpty()) { // has required fields but JSON object is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in PaymentData is not found in the empty JSON string", PaymentData.openapiRequiredFields.toString()));
        }
      }

      Set<Entry<String, JsonElement>> entries = jsonObj.entrySet();
      // check to see if the JSON string contains additional fields
      for (Entry<String, JsonElement> entry : entries) {
        if (!PaymentData.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `PaymentData` properties. JSON: %s", entry.getKey(), jsonObj.toString()));
        }
      }

      // check to make sure all required properties/fields are present in the JSON string
      for (String requiredField : PaymentData.openapiRequiredFields) {
        if (jsonObj.get(requiredField) == null) {
          throw new IllegalArgumentException(String.format("The required field `%s` is not found in the JSON string: %s", requiredField, jsonObj.toString()));
        }
      }
      if (!jsonObj.get("notice_number").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `notice_number` to be a primitive type in the JSON string but got `%s`", jsonObj.get("notice_number").toString()));
      }
      // validate the optional field `payee`
      if (jsonObj.get("payee") != null && !jsonObj.get("payee").isJsonNull()) {
        Payee.validateJsonObject(jsonObj.getAsJsonObject("payee"));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!PaymentData.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'PaymentData' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<PaymentData> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(PaymentData.class));

       return (TypeAdapter<T>) new TypeAdapter<PaymentData>() {
           @Override
           public void write(JsonWriter out, PaymentData value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public PaymentData read(JsonReader in) throws IOException {
             JsonObject jsonObj = elementAdapter.read(in).getAsJsonObject();
             validateJsonObject(jsonObj);
             return thisAdapter.fromJsonTree(jsonObj);
           }

       }.nullSafe();
    }
  }

 /**
  * Create an instance of PaymentData given an JSON string
  *
  * @param jsonString JSON string
  * @return An instance of PaymentData
  * @throws IOException if the JSON string is invalid with respect to PaymentData
  */
  public static PaymentData fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, PaymentData.class);
  }

 /**
  * Convert an instance of PaymentData to an JSON string
  *
  * @return JSON string
  */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

