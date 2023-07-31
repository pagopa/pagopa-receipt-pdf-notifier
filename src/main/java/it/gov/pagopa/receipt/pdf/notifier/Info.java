package it.gov.pagopa.receipt.pdf.notifier;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import it.gov.pagopa.receipt.pdf.notifier.model.AppInfo;

import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


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

        return request.createResponseBuilder(HttpStatus.OK)
                .body(getInfo(context.getLogger(), "/META-INF/maven/it.gov.pagopa.receipt.pdf.notifier/pagopa-receipt-pdf-notifier/pom.properties"))
                .build();
    }
    public synchronized AppInfo getInfo(Logger logger, String path) {
        String version = null;
        String name = null;
        try {
            Properties properties = new Properties();
            InputStream inputStream = getClass().getResourceAsStream(path);
            if (inputStream != null) {
                properties.load(inputStream);
                version = properties.getProperty("version", null);
                name = properties.getProperty("artifactId", null);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Impossible to retrieve information from pom.properties file.", e);
        }
        return AppInfo.builder().version(version).environment("azure-fn").name(name).build();
    }
}