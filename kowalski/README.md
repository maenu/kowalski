# KOWALSKI

Collects APIs and API clients including its dependencies.

## Build

Tests don't pass, so skip tests in build.

	mvn install -DskipTests

## Configure

The default configuration will collect clients of the APIs matched to a query in the `input` JMS queue (see `configuration/collector.properties`).
The classpaths of the collected clients are put into the JMS `output` queue, from where you can consume it for analysis.
You can change the plumbing and parallelization of the tasks.
For example, you can collect only APIs, but not clients.

## Run

Start the JMS broker (tested with Apache Artemis 1.5.5).
Build and unpack the `kowalski-distribution` archive.
Run `kowalski-service start 'g:"org.apache.lucene" AND a:"lucene-core"' input` to collect clients of Apache Lucene (given the default configuration).
`kowalski-service` has also the commands `stop` and `status`.
