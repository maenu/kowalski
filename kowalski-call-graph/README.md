# KOWALSKI Call-Graph

Extracts call-graphs from classpaths collected by KOWALSKI.
Relies on SOOT.

## Build

Tests don't pass, so skip tests in build.

    mvn install -DskipTests

## Configure

The default configuration will read classpaths from the `output` JMS queue (see `configuration/application.properties`).
The call-graphs are written into a Neo4j database.

## Run

Start the JMS broker (tested with Apache Artemis 1.5.5).
Build and unpack the `kowalski-call-graph-distribution` archive.
Run `kowalski-call-graph-service start` to analyze classpaths read from the `output` JMS queue (given the default configuration).
`kowalski-call-graph-service` has also the commands `stop` and `status`.
