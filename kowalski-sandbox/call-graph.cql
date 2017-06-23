MATCH (a:ARTIFACT)-[:DEPENDS_ON]->(l:ARTIFACT)
WHERE a.analyzed
	AND l.groupId = 'org.apache.lucene'
	AND l.artifactId = 'lucene-core'
	AND NOT a.groupId = 'org.apache.lucene'
WITH a, l
LIMIT 1
MATCH p=(a)-[:CONTAINS]->(c:CLASS)-[:IMPLEMENTS]->(:METHOD)-[:INVOKES]->(:METHOD)<-[:DECLARES]-(:CLASS)<-[:CONTAINS]-(l)
WHERE c.name STARTS WITH a.groupId
RETURN p
LIMIT 20;
