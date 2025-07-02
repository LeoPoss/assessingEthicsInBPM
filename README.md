# Assessing Ethics in BPM

This repository contains the prototype implementation for the APERT framework, a novel approach for integrating ethical considerations into business process execution.

## Overview

APERT is designed to assess ethical perspectives and values throughout the Business Process Management (BPM) lifecycle. It provides a mechanism for evaluating ethical considerations during process modeling, execution, and monitoring phases.

## Architecture

The prototype consists of three main components:

- **BPMS**: Camunda is used as the Business Process Management System.
- **Interface Application**: A cross-platform application built with Compose Multiplatform.
- **Data Collection and Visualization**: Elastic Stack (Elasticsearch and Kibana) for data storage and visualization.

## Setup

1. Start Elastic Stack (`/elasticstack` -> :5601)
    ```shell
    docker compose up -d
    ```
2. Start Camunda (`/bpms` -> :8080 admin:admin)
    ```shell
    ./gradlew bootRun
    ```
3. Start App (`/application`)  
    Here because of [Compose Multiplatform](https://www.jetbrains.com/de-de/compose-multiplatform/) there are many possibilities (resize window if needed). The easiest ones include:
    - Using Android Studio and running it as a Compose project either on an Android or iOS Emulator/Simulator
    - running it using Kotlin WASM (keep ports in mind, as Camunda and the application both start at 8080 default)
    ```shell
    ./gradlew wasmJsBrowserRun 
    ```
    - running it on desktop
    ```shell
    ./gradlew run 
    ```
4. Import the premade dashboard with exemplary visualizations
   ```shell
   curl -X POST "http://localhost:5601/api/saved_objects/_import?overwrite=true" -H "kbn-xsrf: true" --form file=@./elasticstack/kibana-dashboard.ndjson   
   ```
5. App:
    - Start process instance 
    - Swipe cards
    - Evaluate perspectives
6. Visit Kibana and select the imported Dasboard ([localhost:5601](http://localhost:5601))

### Additional information

Next to the values and perspectives directly gained from the users, we also use [runtime fields](https://www.elastic.co/guide/en/elasticsearch/painless/current/painless-runtime-fields.html) that Elastic Search provides to accumulate the average for each broader view during runtime. This is included in the dashboard, where the following views are defined:

#### Broad-based Moral Equity
```js
if (doc['moralityValues.Just'].size() != 0 &&
doc['moralityValues.Fair'].size() != 0 &&
doc['moralityValues.Morally Right'].size() != 0 &&
doc['moralityValues.Acceptable to my Family'].size() != 0) {
    emit((doc['moralityValues.Just'].value + doc['moralityValues.Fair'].value + doc['moralityValues.Morally Right'].value + doc['moralityValues.Acceptable to my Family'].value) / 4);
}
```

#### Relativist View
```js
if (doc['moralityValues.Culturally Acceptable'].size() != 0 &&
doc['moralityValues.Traditionally Acceptable'].size() != 0 ) {
    emit((doc['moralityValues.Culturally Acceptable'].value + doc['moralityValues.Traditionally Acceptable'].value) / 2);
}
```

#### Social Contract View
```js
if (doc['moralityValues.Does not Violate an Unspoken Promise'].size() != 0 &&
doc['moralityValues.Does not Violate an Unwritten Contract'].size() != 0 ) {
    emit((doc['moralityValues.Does not Violate an Unspoken Promise'].value + doc['moralityValues.Does not Violate an Unwritten Contract'].value) / 2);
} 
```

License
This project is licensed under the GNU GPLv3 License - see the LICENSE file for details.