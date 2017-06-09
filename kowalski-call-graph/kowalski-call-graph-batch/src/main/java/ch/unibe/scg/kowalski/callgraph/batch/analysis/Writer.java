package ch.unibe.scg.kowalski.callgraph.batch.analysis;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.helpers.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

public class Writer implements ItemWriter<Result> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Writer.class);

	private GraphDatabaseService graphDatabaseService;

	public Writer(GraphDatabaseService graphDatabaseService) {
		this.graphDatabaseService = graphDatabaseService;
	}

	@Override
	public void write(List<? extends Result> results) throws Exception {
		results.stream().forEach(this::writeWithTransaction);
	}

	private void writeWithTransaction(Result result) {
		TransactionTemplate template = new TransactionTemplate().retries(3).backoff(3, TimeUnit.SECONDS);
		template.with(this.graphDatabaseService).execute(transaction -> {
			this.write(result);
		});
	}

	/**
	 * Relies on object identity on equal input objects (artifact, class,
	 * method, ...).
	 */
	private void write(Result result) {
		GraphDatabaseOperations graphDatabaseOperations = new GraphDatabaseOperations(this.graphDatabaseService);
		// artifacts
		// - analyzed
		graphDatabaseOperations.mergeArtifact(result.getArtifact());
		if (graphDatabaseOperations.setArtifactAnalyzed(result.getArtifact())) {
			LOGGER.info("Already persisted, skip {}", result.getArtifact().toString());
			return;
		}
		graphDatabaseOperations.setArtifactTimestamp(result.getArtifact());
		// - depends on
		result.getClassArtifacts().values().stream().collect(Collectors.toSet()).stream()
				.forEach(artifactDependencyModel -> {
					graphDatabaseOperations.mergeArtifact(artifactDependencyModel);
					graphDatabaseOperations.mergeDependsOn(result.getArtifact(), artifactDependencyModel);
					graphDatabaseOperations.setArtifactTimestamp(artifactDependencyModel);
				});
		// classes
		// - contains
		result.getClassArtifacts().entrySet().stream().forEach(entry -> {
			graphDatabaseOperations.mergeContains(entry.getValue(), entry.getKey());
		});
		// methods
		// - declares
		result.getMethods().stream().forEach(methodModel -> {
			graphDatabaseOperations.mergeDeclares(methodModel.getClazz(), methodModel);
		});
		// methods
		// - implements
		result.getData().keySet().stream().forEach(methodModel -> {
			graphDatabaseOperations.mergeImplements(methodModel.getClazz(), methodModel);
		});
		// invocations
		result.getData().entrySet().stream().forEach(entry -> {
			graphDatabaseOperations.mergeInvocations(entry.getKey(), entry.getValue());
		});
	}

}
