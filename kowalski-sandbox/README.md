# KOWALSKI Sandbox

Sets up an environment to play around with KOWALSKI.

## Preparation

Build `kowalski` and `kowalski-call-graph`.
Move unpacked distributions to `kowalski-sandbox`.
Grab [Apache Artemis 1.5.5](https://activemq.apache.org/artemis/download.html) and unpack it.
Run `./install-jms-broker` to prepare a new broker for KOWALSKI.

## Run

Run `./start 'g:"org.apache.lucene" AND a:"lucene-core"' input` to collect clients of Apache Lucene.
Observe KOWALSKI with JMX by looking at the sizes of the JMS queues.
Run `./stop` to stop, when you have enough data.

## Inspect

Run [Neo4j 3.1](https://neo4j.com/download/other-releases/) on the database path `kowalski-call-graph/data/db` and go to [localhost:7474](localhost:7474) to query the call-graph db.
