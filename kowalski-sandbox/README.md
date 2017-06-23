# KOWALSKI Sandbox

Sets up an environment to play around with KOWALSKI.

## Install

Requires Java 8, installation testsed on OS X.
Run `./install` to prepare a new Apache Artemis broker, Neo4j, and KOWALSKI with the call graph analysis.

## Run

Run `./start 'g:"org.apache.lucene" AND a:"lucene-core"' 'org.apache.lucene'` to collect clients of Apache Lucene.
Observe KOWALSKI with JMX by looking at the sizes of the JMS queues.
You can do that with JDK's JMC Mbean browser, connect to `localhost:1099`.
Or use the `index.html`, which connect through Jolokia at `localhost:8778`.
Run `./stop` to stop, when you have enough data.

## Inspect

Run `./neo4j-community-3.0.5/bin/neo4j start` to start Neo4j and go to [localhost:7474](localhost:7474) to query the call-graph db.
You can query the data with Neo4j's Cypher language, `call-graph.cql` will give you a small slice of call graph between Apache Lucene and a client.
