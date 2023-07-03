# pagoPA Receipt-pdf-datastore

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pagopa_pagopa-receipt-pdf-datastore&metric=alert_status)](https://sonarcloud.io/dashboard?id=pagopa_pagopa-receipt-pdf-datastore)

Java Azure Function that exposes REST API to generate a PDFA/2a document based on the provided data and HTML template.

---

## Summary 📖

- [Start Project Locally 🚀](#start-project-locally-)
  * [Run locally with Docker](#run-locally-with-docker)
    + [Prerequisites](#prerequisites)
    + [Run docker container](#run-docker-container)
  * [Run locally with Maven](#run-locally-with-maven)
    + [Prerequisites](#prerequisites-1)
    + [Set environment variables](#set-environment-variables)
    + [Run the project](#run-the-project)
  * [Test](#test)
- [Develop Locally 💻](#develop-locally-)
  * [Prerequisites](#prerequisites-2)
  * [Testing 🧪](#testing-)
    + [Unit testing](#unit-testing)
    + [Integration testing](#integration-testing)
    + [Performance testing](#performance-testing)
- [Contributors 👥](#contributors-)
  * [Maintainers](#maintainers)

---

## Start Project Locally 🚀

### Run locally with Docker

#### Prerequisites

- docker

#### Set environment variables

`docker build -t pagopa-receip-pdf-datastore .`

`cp .env.example .env`

and replace in `.env` with correct values

#### Run docker container

then type :

`docker run -p 80:80 --env-file=./.env pagopa-receip-pdf-datastore`

### Run locally with Maven

#### Prerequisites

- maven

#### Set environment variables

On terminal type:

`cp local.settings.json.example local.settings.json`

then replace env variables with correct values
(if there is NO default value, the variable HAS to be defined)

| VARIABLE                          | USAGE                                                                            |      DEFAULT VALUE      |
|-----------------------------------|----------------------------------------------------------------------------------|:-----------------------:|
| `RECEIPT_QUEUE_CONN_STRING`       | Connection string to the Receipt Queue                                           |                         |
| `RECEIPT_QUEUE_TOPIC`             | Topic name of the Receipt Queue                                                  |                         |
| `RECEIPT_QUEUE_DELAY`             | Delay, in seconds, the visibility of the messages in the queue                   |           "1"           |
| `RECEIPT_QUEUE_MAX_RETRY`         | Number of retry to complete the generation process before being tagged as FAILED |           "5"           |
| `BLOB_STORAGE_ACCOUNT_ENDPOINT`   | Endpoint to the Receipt Blob Storage                                             |                         |
| `BLOB_STORAGE_CONN_STRING`        | Connection string of the Receipt Blob Storage                                    |                         |
| `BLOB_STORAGE_CONTAINER_NAME`     | Container name of the Receipt container in the Blob Storage                      |                         |
| `COSMOS_BIZ_EVENT_CONN_STRING`    | Connection string to the BizEvent CosmosDB                                       |                         |
| `COSMOS_RECEIPTS_CONN_STRING`     | Connection string to the Receipt CosmosDB                                        |                         |
| `COSMOS_RECEIPT_SERVICE_ENDPOINT` | Endpoint to the Receipt CosmosDB                                                 |                         |
| `COSMOS_RECEIPT_KEY`              | Key to the Receipt CosmosDB                                                      |                         |
| `COSMOS_RECEIPT_DB_NAME`          | Database name of the Receipt database in CosmosDB                                |                         |
| `COSMOS_RECEIPT_CONTAINER_NAME`   | Container name of the Receipt container in CosmosDB                              |                         |
| `PDF_ENGINE_ENDPOINT`             | Endpoint to the PDF engine                                                       |                         |
| `OCP_APIM_SUBSCRIPTION_KEY`       | Auth key for Azure to access the PDF Engine                                      |                         |
| `COMPLETE_TEMPLATE_FILE_NAME `    | Filename of the complete template                                                | "complete_template.zip" |
| `PARTIAL_TEMPLATE_FILE_NAME`      | Filename of the partial template                                                 | "partial_template.zip"  |

> to doc details about AZ fn config
> see [here](https://stackoverflow.com/questions/62669672/azure-functions-what-is-the-purpose-of-having-host-json-and-local-settings-jso)

#### Run the project

`mvn clean package`

`mvn azure-functions:run`

### Test

`curl http://localhost:8080/info`

---

## Develop Locally 💻

### Prerequisites

- git
- maven
- jdk-11

### Testing 🧪

#### Unit testing

To run the **Junit** tests:

`mvn clean verify`

#### Integration testing

#### Performance testing

---

## Contributors 👥

Made with ❤️ by PagoPa S.p.A.

### Maintainers

See `CODEOWNERS` file