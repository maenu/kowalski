package ch.unibe.scg.kowalski.serialization;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.solr.client.solrj.SolrQuery;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.unibe.scg.kowalski.task.Page;

public class FactoryTest {

	private ObjectMapper objectMapper;

	public FactoryTest() {
		super();
		this.objectMapper = (new Factory()).newObjectMapper();
	}

	@Test
	public void testArtifactWithoutPropertiesAndFile() throws IOException {
		Artifact original = new DefaultArtifact("org.neo4j", "neo4j", "jar", "3.0.5");
		String message = this.objectMapper.writeValueAsString(original);
		Artifact deserialized = this.objectMapper.readValue(message, Artifact.class);
		Assert.assertEquals(original, deserialized);
	}

	@Test
	public void testArtifactWithPropertiesAndFile() throws IOException {
		Map<String, String> properties = new HashMap<>();
		properties.put("whatever", "value");
		Artifact original = new DefaultArtifact("org.neo4j", "neo4j", null, "jar", "3.0.5", properties,
				new File("artifact"));
		String message = this.objectMapper.writeValueAsString(original);
		Artifact deserialized = this.objectMapper.readValue(message, Artifact.class);
		Assert.assertEquals(original, deserialized);
	}

	@Test
	public void testSolrQuery() throws IOException {
		SolrQuery original = new SolrQuery("g:org.neo4j");
		String message = this.objectMapper.writeValueAsString(original);
		SolrQuery deserialized = this.objectMapper.readValue(message, SolrQuery.class);
		Assert.assertEquals(original.toString(), deserialized.toString());
	}

	@Test
	public void testPageWithKeyAndWithElements() throws IOException {
		Page<Artifact, Artifact> original = new Page<>(
				Optional.of(new DefaultArtifact("org.neo4j", "neo4j", "jar", "3.0.4")),
				Collections.singletonList(new DefaultArtifact("org.neo4j", "neo4j", "jar", "3.0.5")));
		String message = this.objectMapper.writeValueAsString(original);
		@SuppressWarnings("unchecked")
		Page<Artifact, Artifact> deserialized = this.objectMapper.readValue(message, Page.class);
		Assert.assertEquals(original, deserialized);
	}

	@Test
	public void testPageWithoutKeyAndWithElements() throws IOException {
		Page<Artifact, Artifact> original = new Page<>(Optional.empty(),
				Collections.singletonList(new DefaultArtifact("org.neo4j", "neo4j", "jar", "3.0.5")));
		String message = this.objectMapper.writeValueAsString(original);
		@SuppressWarnings("unchecked")
		Page<Artifact, Artifact> deserialized = this.objectMapper.readValue(message, Page.class);
		Assert.assertEquals(original, deserialized);
	}

	@Test
	public void testPageWithKeyAndWithoutElements() throws IOException {
		Page<Artifact, Artifact> original = new Page<>(
				Optional.of(new DefaultArtifact("org.neo4j", "neo4j", "jar", "3.0.4")), Collections.emptyList());
		String message = this.objectMapper.writeValueAsString(original);
		@SuppressWarnings("unchecked")
		Page<Artifact, Artifact> deserialized = this.objectMapper.readValue(message, Page.class);
		Assert.assertEquals(original, deserialized);
	}

	@Test
	public void testPageWithoutKeyAndWithoutElements() throws IOException {
		Page<Artifact, Artifact> original = new Page<>(Optional.empty(), Collections.emptyList());
		String message = this.objectMapper.writeValueAsString(original);
		@SuppressWarnings("unchecked")
		Page<Artifact, Artifact> deserialized = this.objectMapper.readValue(message, Page.class);
		Assert.assertEquals(original, deserialized);
	}

}
