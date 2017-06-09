package ch.unibe.scg.kowalski.callgraph.batch;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.jms.ConnectionFactory;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jms.core.JmsTemplate;

import ch.unibe.scg.kowalski.callgraph.analysis.utility.Pair;
import ch.unibe.scg.kowalski.callgraph.batch.analysis.AnalysisRunner;
import ch.unibe.scg.kowalski.callgraph.batch.analysis.Label;
import ch.unibe.scg.kowalski.callgraph.batch.analysis.Processor;
import ch.unibe.scg.kowalski.callgraph.batch.analysis.Reader;
import ch.unibe.scg.kowalski.callgraph.batch.analysis.Result;
import ch.unibe.scg.kowalski.callgraph.batch.analysis.Writer;
import ch.unibe.scg.kowalski.task.Page;

@org.springframework.context.annotation.Configuration
@Import(ch.unibe.scg.kowalski.worker.Configuration.class)
public class Configuration {

	public static final String PACKAGE = "ch.unibe.scg.kowalski.callgraph.batch";

	@Value("${" + PACKAGE + ".brokerUrl:#{\"tcp://localhost:61616\"}}")
	private String brokerUrl;
	@Value("${" + PACKAGE + ".output}")
	private String output;
	@Value("${" + PACKAGE + ".cardinality}")
	private int cardinality;
	@Value("${" + PACKAGE + ".timeout}")
	private long timeout;
	@Value("${" + PACKAGE + ".lastModified.cache}")
	private String lastModifiedCache;
	@Value("${" + PACKAGE + ".neo4j.path}")
	private String neo4jPath;
	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Bean
	public ConnectionFactory connectionFactory() {
		return new ActiveMQJMSConnectionFactory(this.brokerUrl);
	}

	@Bean
	public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
	}

	@Bean
	public Supplier<Job> jobFactory(JmsTemplate jmsTemplate, Artifact jreArtifact,
			GraphDatabaseService graphDatabaseService) throws SettingsBuildingException {
		return () -> {
			ItemReader<Page<Artifact, Artifact>> reader = new Reader(jmsTemplate, new ActiveMQQueue(this.output));
			ItemProcessor<Page<Artifact, Artifact>, Result> processor = new Processor(new AnalysisRunner(this.timeout),
					jreArtifact, new File(this.lastModifiedCache));
			ItemWriter<Result> writer = new Writer(graphDatabaseService);
			Step step = this.stepBuilderFactory.get("analysis").<Page<Artifact, Artifact>, Result>chunk(1)
					.faultTolerant().skipPolicy(new LoggingAlwaysSkipItemSkipPolicy()).noRollback(Throwable.class)
					.reader(reader).processor(processor).writer(writer).build();
			return this.jobBuilderFactory.get("analysis").start(step).build();
		};
	}

	@Bean
	public GraphDatabaseService graphDatabaseService() {
		// FIXME indexes
		GraphDatabaseService graphDatabaseService = (new GraphDatabaseFactory())
				.newEmbeddedDatabaseBuilder(Paths.get(this.neo4jPath).toFile())
				.loadPropertiesFromURL(this.getClass().getClassLoader().getResource("neo4j.conf")).newGraphDatabase();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDatabaseService.shutdown();
			}
		});
		// constraints
		try (Transaction transaction = graphDatabaseService.beginTx()) {
			// merge needs constraints
			// http://neo4j.com/docs/java-reference/current/#tutorials-java-embedded-unique-get-or-create
			Schema schema = graphDatabaseService.schema();
			Map<Label, String> constraints = new HashMap<>();
			constraints.put(Label.ARTIFACT, "hash");
			constraints.put(Label.CLASS, "hash");
			constraints.put(Label.METHOD, "hash");
			constraints.entrySet().stream().filter(entry -> {
				for (ConstraintDefinition constraint : schema.getConstraints(entry.getKey())) {
					for (String key : constraint.getPropertyKeys()) {
						if (key.equals(entry.getValue())) {
							return false;
						}
					}
				}
				return true;
			}).forEach(entry -> graphDatabaseService.schema().constraintFor(entry.getKey())
					.assertPropertyIsUnique(entry.getValue()).create());
			transaction.success();
		}
		// indexes
		try (Transaction transaction = graphDatabaseService.beginTx()) {
			Schema schema = graphDatabaseService.schema();
			Set<Pair<Label, String>> indexes = new HashSet<>();
			indexes.add(new Pair<>(Label.ARTIFACT, "groupId"));
			indexes.add(new Pair<>(Label.ARTIFACT, "artifactId"));
			indexes.add(new Pair<>(Label.ARTIFACT, "version"));
			indexes.add(new Pair<>(Label.ARTIFACT, "classifier"));
			indexes.add(new Pair<>(Label.ARTIFACT, "extension"));
			indexes.add(new Pair<>(Label.ARTIFACT, "timestamp"));
			indexes.add(new Pair<>(Label.CLASS, "name"));
			indexes.add(new Pair<>(Label.METHOD, "signature"));
			indexes.stream().filter(pair -> {
				for (IndexDefinition index : schema.getIndexes(pair.getU())) {
					for (String key : index.getPropertyKeys()) {
						if (key.equals(pair.getV())) {
							return false;
						}
					}
				}
				return true;
			}).forEach(pair -> graphDatabaseService.schema().indexFor(pair.getU()).on(pair.getV()).create());
			transaction.success();
		}
		return graphDatabaseService;
	}

	@Bean
	public Artifact jreArtifact() {
		return new DefaultArtifact(System.getProperty("java.vendor"), "jre", null, "jar",
				System.getProperty("java.version"), null,
				Paths.get(System.getProperty("java.home")).resolve("lib").resolve("rt.jar").toFile());
	}

}
