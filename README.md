# pagoPA Receipt-pdf-notifier

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pagopa_pagopa-receipt-pdf-notifier&metric=alert_status)](https://sonarcloud.io/dashboard?id=pagopa_pagopa-receipt-pdf-notifier)

Java Azure Function that notifies to an IO user a message with the previously generated receipt's info.

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

`docker build -t pagopa-receip-pdf-notifier .`

`cp .env.example .env`

and replace in `.env` with correct values

#### Run docker container

then type :

`docker run -p 80:80 --env-file=./.env pagopa-receip-pdf-notifier`

### Run locally with Maven

#### Prerequisites

- maven

#### Set environment variables

On terminal type:

`cp local.settings.json.example local.settings.json`

then replace env variables with correct values
(if there is NO default value, the variable HAS to be defined)

| VARIABLE                              | USAGE                                                                             |                    DEFAULT VALUE                     |
|---------------------------------------|-----------------------------------------------------------------------------------|:----------------------------------------------------:|
| `STORAGE_CONN_STRING`                 | Connection string to the Receipt Queue                                            |                                                      |
| `NOTIFIER_QUEUE_TOPIC`                | Topic name of the Receipt Queue                                                   |                                                      |
| `NOTIFIER_QUEUE_DELAY`                | Delay, in seconds, the visibility of the messages in the queue                    |                          1                           |
| `NOTIFY_RECEIPT_MAX_RETRY`            | Number of retry to complete the generation process before being tagged as FAILED  |                          5                           |
| `COSMOS_RECEIPTS_CONN_STRING`         | Connection string to the Receipt CosmosDB                                         |                                                      |
| `COSMOS_RECEIPT_SERVICE_ENDPOINT`     | Endpoint to the Receipt CosmosDB                                                  |                                                      |
| `COSMOS_RECEIPT_KEY`                  | Key to the Receipt CosmosDB                                                       |                                                      |
| `COSMOS_RECEIPT_DB_NAME`              | Database name of the Receipt database in CosmosDB                                 |                                                      |
| `COSMOS_RECEIPT_CONTAINER_NAME`       | Container name of the Receipt container in CosmosDB                               |                                                      |
| `IO_API_BASE_PATH`                    | Base path to IO APIs                                                              |  https://api.dev.platform.pagopa.it/mock-io/api/v1   |
| `IO_API_PROFILES_PATH`                | Path to IO check user API                                                         |                      /profiles                       |
| `IO_API_MESSAGES_PATH`                | Path to IO send messages API                                                      |                      /messages                       |
| `OCP_APIM_HEADER_KEY`                 | OCP APIM header key                                                               |              Ocp-Apim-Subscription-Key               |
| `PDV_TOKENIZER_BASE_PATH`             | Base path to PDV Tokenizer                                                        | https://api.uat.tokenizer.pdv.pagopa.it/tokenizer/v1 |
| `TOKENIZER_APIM_HEADER_KEY`           | Tokenizer APIM header key                                                         |                      x-api-key                       |
| `PDV_TOKENIZER_SEARCH_TOKEN_ENDPOINT` | PDV Tokenizer endpoint to search token                                            |                    /tokens/search                    |
| `PDV_TOKENIZER_FIND_PII_ENDPOINT`     | PDV Tokenizer endpoint to find fiscal code                                        |                 /tokens/{token}/pii                  |
| `PDV_TOKENIZER_CREATE_TOKEN_ENDPOINT` | PDV Tokenizer endpoint to generate token                                          |                       /tokens                        |
| `PDV_TOKENIZER_INITIAL_INTERVAL`      | PDV Tokenizer initial interval for retry a request that fail with 429 status code |                         1000                         |
| `PDV_TOKENIZER_MULTIPLIER`            | PDV Tokenizer interval multiplier for subsequent request retry                    |                         2.0                          |
| `PDV_TOKENIZER_RANDOMIZATION_FACTOR`  | PDV Tokenizer randomization factor for interval retry calculation                 |                         0.6                          |
| `PDV_TOKENIZER_MAX_RETRIES`           | PDV Tokenizer max request retry                                                   |                          4                           |

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

From `./integration-test/src`

1. `yarn install`
2. `yarn test`

#### Performance testing

install [k6](https://k6.io/) and then from `./performance-test/src`

1. `k6 run --env VARS=local.environment.json --env TEST_TYPE=./test-types/load.json --env RECEIPT_COSMOS_DB_SUBSCRIPTION_KEY=<your-secret> receipt_processor.js`


---

## Contributors 👥

Made with ❤️ by PagoPa S.p.A.

### Maintainers

See `CODEOWNERS` file