package ch.unibe.scg.kowalski.callgraph.batch;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

import javax.jms.ConnectionFactory;

import org.apache.activemq.artemis.junit.EmbeddedJMSResource;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ch.unibe.scg.kowalski.callgraph.batch.Application;
import ch.unibe.scg.kowalski.task.Dependency;
import ch.unibe.scg.kowalski.task.Maven;
import ch.unibe.scg.kowalski.task.Page;
import ch.unibe.scg.kowalski.worker.dependency.Processor;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class ApplicationTest {

	@org.springframework.context.annotation.Configuration
	@Import({ Application.class })
	public static class Configuration {

		@Bean
		public JmsTemplate jmsTemplate(MessageConverter messageConverter, ConnectionFactory connectionFactory) {
			JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
			jmsTemplate.setMessageConverter(messageConverter);
			jmsTemplate.setReceiveTimeout(100);
			return jmsTemplate;
		}

	}

	@Rule
	public EmbeddedJMSResource resource;
	@Autowired
	private Application application;
	@Autowired
	private JmsTemplate jmsTemplate;
	@Value("${" + ch.unibe.scg.kowalski.callgraph.batch.Configuration.PACKAGE + ".output}")
	private String output;

	public ApplicationTest() throws SettingsBuildingException, IOException {
		this.resource = new EmbeddedJMSResource();
		try {
			this.deleteDirectory(Paths.get("target/test"));
		} catch (NoSuchFileException exception) {
			// noop
		}
	}

	@Test
	public void testJob() throws Exception {
		// this.jmsTemplate.convertAndSend(this.output, this
		// .fetch(new DefaultArtifact("org.apache.flink",
		// "flink-metrics-dropwizard", "jar", "1.1.3-hadoop1")));
		// this.jmsTemplate.convertAndSend(this.output,
		// this.fetch(new DefaultArtifact("commons-io", "commons-io", "jar",
		// "2.4")));
		// this.jmsTemplate.convertAndSend(this.output,
		// this.fetch(new
		// DefaultArtifact("org.apache.lucene:lucene-analyzers-common:jar:5.5.0")));
		// this.jmsTemplate.convertAndSend(this.output,
		// this.fetch(new DefaultArtifact("org.elasticsearch", "elasticsearch",
		// "jar", "0.6.0")));
		// this.jmsTemplate.convertAndSend(this.output,
		// this.fetch(new
		// DefaultArtifact("org.apache.lucene:lucene-core:jar:4.1.0")));
		// this.jmsTemplate.convertAndSend(this.output,
		// this.fetch(new
		// DefaultArtifact("org.apache.lucene:lucene-core:jar:5.1.0")));
		// this.jmsTemplate.convertAndSend(this.output,
		// this.fetch(new
		// DefaultArtifact("org.apache.lucene:lucene-core:jar:6.1.0")));
		// this.jmsTemplate.convertAndSend(this.output,
		// this.fetch(new
		// DefaultArtifact("org.apache.solr:solr-test-framework:jar:5.5.4")));
		// this.jmsTemplate.convertAndSend(this.output, this.fetch(new
		// DefaultArtifact("uk.ac.gate:gate-core:jar:8.0")));
		this.jmsTemplate.convertAndSend(this.output, this.fetch(new DefaultArtifact("com.h2database:h2:jar:1.2.124")));
		this.application.start();
		// should terminate
		// TODO test if worked
	}

	private Page<Artifact, Artifact> fetch(Artifact artifact)
			throws SettingsBuildingException, DependencyResolutionException, DependencyCollectionException {
		ScopeDependencySelector dependencySelector = new ScopeDependencySelector(
				Collections.singleton(JavaScopes.COMPILE), Collections.emptySet());
		Dependency task = new Dependency(new Maven());
		Processor processor = new Processor(task, true, true, JavaScopes.COMPILE, dependencySelector, false);
		return processor.process(artifact);
	}

	private void deleteDirectory(Path directory) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});
	}

}
