ARG JAVA_VERSION=11
# This image additionally contains function core tools – useful when using custom extensions
FROM mcr.microsoft.com/azure-functions/java:3.0-java$JAVA_VERSION-build AS installer-env

COPY . /src/java-function-app
RUN echo $(ls -1 /src/java-function-app)
RUN chmod 777 /src/java-function-app/agent/config.yaml
RUN cd /src/java-function-app && \
    wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.19.0/jmx_prometheus_javaagent-0.19.0.jar && \
    curl -o 'opentelemetry-javaagent.jar' -L 'https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.25.1/opentelemetry-javaagent.jar' && \
    mkdir -p /home/site/wwwroot && \
    mvn clean package -Dmaven.test.skip=true && \
    cd ./target/azure-functions/ && \
    cd $(ls -d */|head -n 1) && \
    cp -a . /home/site/wwwroot && \
    cp /src/java-function-app/agent/config.yaml /home/site/wwwroot/config.yaml
RUN chmod 777 /src/java-function-app/jmx_prometheus_javaagent-0.19.0.jar && \
    cp /src/java-function-app/jmx_prometheus_javaagent-0.19.0.jar /home/site/wwwroot/jmx_prometheus_javaagent-0.19.0.jar

RUN chmod 777 /src/java-function-app/opentelemetry-javaagent.jar && \
    cp /src/java-function-app/opentelemetry-javaagent.jar /home/site/wwwroot/opentelemetry-javaagent.jar

# This image is ssh enabled
#FROM mcr.microsoft.com/azure-functions/java:3.0-java$JAVA_VERSION-appservice
# This image isn't ssh enabled
FROM mcr.microsoft.com/azure-functions/java:3.0-java$JAVA_VERSION

ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true

EXPOSE 80
EXPOSE 12345
COPY --from=installer-env ["/home/site/wwwroot", "/home/site/wwwroot"]