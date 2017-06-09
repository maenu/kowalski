package ch.unibe.scg.kowalski.callgraph.batch.analysis;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;

import com.google.common.hash.Hashing;

import ch.unibe.scg.kowalski.callgraph.analysis.model.Invocation;
import ch.unibe.scg.kowalski.callgraph.analysis.utility.Cache;

/**
 * Creates a new analysis graph. Neo4j doesn't provide serializable isolation,
 * but uses read committed. This implies that means that we have non-repeatable
 * reads, e.g. we might create the same entity twice, if we didn't find it first
 * and then two parallel runs create it. Therefore we merge equal artifacts,
 * classes, methods and imprecise and precise value types later.
 *
 * Merges require unique constraints to lock, but constraints are only available
 * for single column, hence the hash.
 *
 * Using cypher does not promise read committed, hence we need SET.
 *
 * @see <a href=
 *      "http://neo4j.com/docs/java-reference/current/#transactions-isolation">http://neo4j.com/docs/java-reference/current/#transactions-isolation</a>
 * @see <a href=
 *      "https://github.com/neo4j/neo4j/issues/37">https://github.com/neo4j/neo4j/issues/37</a>
 * @see <a href=
 *      "https://neo4j.com/blog/advanced-neo4j-fiftythree-reading-writing-scaling/">https://neo4j.com/blog/advanced-neo4j-fiftythree-reading-writing-scaling/</a>
 * @see <a href=
 *      "https://neo4j.com/blog/common-confusions-cypher/">https://neo4j.com/blog/common-confusions-cypher/</a>
 */
public class GraphDatabaseOperations {

	private GraphDatabaseService graphDatabaseService;
	private Cache<org.eclipse.aether.artifact.Artifact, Node> artifactCache;
	private Cache<ch.unibe.scg.kowalski.callgraph.analysis.model.Class, Node> classCache;
	private Cache<ch.unibe.scg.kowalski.callgraph.analysis.model.Method, Node> methodCache;

	public GraphDatabaseOperations(GraphDatabaseService graphDatabaseService) {
		this.graphDatabaseService = graphDatabaseService;
		this.artifactCache = new Cache<>();
		this.classCache = new Cache<>();
		this.methodCache = new Cache<>();
	}

	public void mergeArtifact(org.eclipse.aether.artifact.Artifact artifactModel) {
		this.artifactCache.getOrPut(artifactModel, () -> {
			String cypherQuery = "MERGE (artifact:ARTIFACT {groupId: {groupId}, artifactId: {artifactId}, version: {version}, extension: {extension}, hash: {hash}%s}) RETURN artifact";
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("groupId", artifactModel.getGroupId());
			parameters.put("artifactId", artifactModel.getArtifactId());
			parameters.put("version", artifactModel.getVersion());
			parameters.put("extension", artifactModel.getExtension());
			if (!artifactModel.getClassifier().isEmpty()) {
				cypherQuery = String.format(cypherQuery, ", classifier: {classifier}%s");
				parameters.put("classifier", artifactModel.getClassifier());
			}
			parameters.put("hash", this.hash(parameters.values().toArray(new String[0])));
			cypherQuery = String.format(cypherQuery, "");
			try (Result result = this.graphDatabaseService.execute(cypherQuery, parameters)) {
				return result.<Node>columnAs("artifact").next();
			}
		});
	}

	/**
	 * @param artifactModel
	 *            Must be cached already.
	 */
	public void setArtifactTimestamp(org.eclipse.aether.artifact.Artifact artifactModel) {
		assert this.artifactCache.containsKey(artifactModel);
		Node artifact = this.artifactCache.get(artifactModel);
		if (artifactModel.getProperties().containsKey("timestamp")) {
			artifact.setProperty("timestamp", Long.parseLong(artifactModel.getProperties().get("timestamp")));
		}
	}

	/**
	 * @param artifactModel
	 *            Must be cached already.
	 */
	public boolean setArtifactAnalyzed(org.eclipse.aether.artifact.Artifact artifactModel) {
		assert this.artifactCache.containsKey(artifactModel);
		Node artifact = this.artifactCache.get(artifactModel);
		String cypherQuery = "MATCH (artifact:ARTIFACT) WHERE ID(artifact) = {artifactId} SET artifact._lock = true WITH artifact, EXISTS(artifact.analyzed) AS alreadyAnalyzed SET artifact.analyzed = true REMOVE artifact._lock RETURN alreadyAnalyzed";
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("artifactId", artifact.getId());
		try (Result result = this.graphDatabaseService.execute(cypherQuery, parameters)) {
			return result.<Boolean>columnAs("alreadyAnalyzed").next();
		}
	}

	/**
	 * @param artifactModel
	 *            Must be cached already.
	 * @param artifactDependencyModel
	 *            Must be cached already.
	 */
	public void mergeDependsOn(org.eclipse.aether.artifact.Artifact artifactModel,
			org.eclipse.aether.artifact.Artifact artifactDependencyModel) {
		assert this.artifactCache.containsKey(artifactModel);
		assert this.artifactCache.containsKey(artifactDependencyModel);
		Node artifact = this.artifactCache.get(artifactModel);
		Node artifactDependency = this.artifactCache.get(artifactDependencyModel);
		String cypherQuery = "MATCH (artifact:ARTIFACT), (artifactDependency:ARTIFACT) WHERE ID(artifact) = {artifactId} AND ID(artifactDependency) = {artifactDependencyId} MERGE (artifact)-[:DEPENDS_ON]->(artifactDependency)";
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("artifactId", artifact.getId());
		parameters.put("artifactDependencyId", artifactDependency.getId());
		try (Result result = this.graphDatabaseService.execute(cypherQuery, parameters)) {

		}
	}

	/**
	 * @param artifactModel
	 *            Must be cached already.
	 */
	public void mergeContains(org.eclipse.aether.artifact.Artifact artifactModel,
			ch.unibe.scg.kowalski.callgraph.analysis.model.Class classModel) {
		assert this.artifactCache.containsKey(artifactModel);
		Node artifact = this.artifactCache.get(artifactModel);
		Node clazz = this.mergeClass(artifact, classModel);
		String cypherQuery = "MATCH (artifact:ARTIFACT), (class:CLASS) WHERE ID(artifact) = {artifactId} AND ID(class) = {classId} MERGE (artifact)-[:CONTAINS]->(class)";
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("artifactId", artifact.getId());
		parameters.put("classId", clazz.getId());
		try (Result result = this.graphDatabaseService.execute(cypherQuery, parameters)) {

		}
	}

	/**
	 * @param classModel
	 *            Must be cached already.
	 * @param methodModel
	 *            Must be cached already.
	 */
	public void mergeImplements(ch.unibe.scg.kowalski.callgraph.analysis.model.Class classModel,
			ch.unibe.scg.kowalski.callgraph.analysis.model.Method methodModel) {
		assert this.classCache.containsKey(classModel);
		Node clazz = this.classCache.get(classModel);
		assert this.methodCache.containsKey(methodModel);
		Node method = this.methodCache.get(methodModel);
		String cypherQuery = "MATCH (class:CLASS), (method:METHOD) WHERE ID(class) = {classId} AND ID(method) = {methodId} MERGE (class)-[:IMPLEMENTS]->(method) RETURN method";
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("classId", clazz.getId());
		parameters.put("methodId", method.getId());
		try (Result result = this.graphDatabaseService.execute(cypherQuery, parameters)) {

		}
	}

	/**
	 * @param classModel
	 *            Must be cached already.
	 * @param methodModel
	 */
	public void mergeDeclares(ch.unibe.scg.kowalski.callgraph.analysis.model.Class classModel,
			ch.unibe.scg.kowalski.callgraph.analysis.model.Method methodModel) {
		assert this.classCache.containsKey(classModel);
		Node clazz = this.classCache.get(classModel);
		Node method = this.mergeMethod(methodModel);
		String cypherQuery = "MATCH (class:CLASS), (method:METHOD) WHERE ID(class) = {classId} AND ID(method) = {methodId} MERGE (class)-[:DECLARES]->(method) RETURN method";
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("classId", clazz.getId());
		parameters.put("methodId", method.getId());
		try (Result result = this.graphDatabaseService.execute(cypherQuery, parameters)) {

		}
	}

	/**
	 *
	 * @param methodModel
	 *            Must be cached already.
	 * @param invocationsModels
	 *            Dereferenced methods and fields must be cached already.
	 */
	public void mergeInvocations(ch.unibe.scg.kowalski.callgraph.analysis.model.Method methodModel,
			Set<Invocation> invocationsModels) {
		assert this.methodCache.containsKey(methodModel);
		Node method = this.methodCache.get(methodModel);
		invocationsModels.stream().forEach(invocationModel -> {
			assert this.methodCache.containsKey(invocationModel.getMethod());
			Node invokedMethod = this.methodCache.get(invocationModel.getMethod());
			Relationship invokes = method.createRelationshipTo(invokedMethod, RelationshipType.INVOKES);
			invokes.setProperty("line", invocationModel.getLine());
			invokes.setProperty("statement", invocationModel.getStatement());
		});
	}

	private Node mergeClass(Node artifact, ch.unibe.scg.kowalski.callgraph.analysis.model.Class classModel) {
		return this.classCache.getOrPut(classModel, () -> {
			String cypherQuery = "MERGE (class:CLASS {name: {name}, hash: {hash}}) RETURN class";
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("name", classModel.getName());
			parameters.put("hash", this.hash("" + artifact.getId(), classModel.getName()));
			try (Result result = this.graphDatabaseService.execute(cypherQuery, parameters)) {
				return result.<Node>columnAs("class").next();
			}
		});
	}

	private Node mergeMethod(ch.unibe.scg.kowalski.callgraph.analysis.model.Method methodModel) {
		return this.methodCache.getOrPut(methodModel, () -> {
			assert this.classCache.containsKey(methodModel.getClazz());
			Node clazz = this.classCache.get(methodModel.getClazz());
			String cypherQuery = "MERGE (method:METHOD {signature: {signature}, hash: {hash}}) RETURN method";
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("signature", methodModel.getSignature());
			parameters.put("hash", this.hash("" + clazz.getId(), methodModel.getSignature()));
			try (Result result = this.graphDatabaseService.execute(cypherQuery, parameters)) {
				return result.<Node>columnAs("method").next();
			}
		});
	}

	private String hash(String... parts) {
		return Hashing.sha256().hashString(String.join("|", parts), StandardCharsets.UTF_8).toString();
	}

}
