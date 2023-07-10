package it.gov.pagopa.receipt.pdf.notifier;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;


/**
 * Azure Functions with Azure Http trigger.
 */
public class Info {

    /**
     * This function will be invoked when a Http Trigger occurs
     *
     * @return response with HttpStatus.OK
     */
    @FunctionName("Info")
    public HttpResponseMessage run (
            @HttpTrigger(name = "InfoTrigger",
                    methods = {HttpMethod.GET},
                    route = "info",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        return request.createResponseBuilder(HttpStatus.OK).build();
    }


}