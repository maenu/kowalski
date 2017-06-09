package ch.unibe.scg.kowalski.callgraph.batch.analysis;

public enum RelationshipType implements org.neo4j.graphdb.RelationshipType {
	CONTAINS, IMPLEMENTS, DECLARES, INVOKES
}