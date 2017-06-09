package ch.unibe.scg.kowalski.task;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.unibe.scg.kowalski.task.Dependent;
import ch.unibe.scg.kowalski.task.Page;
import ch.unibe.scg.kowalski.task.Dependent.NotOkResponseException;

public class DependentTest {

	private Artifact artifact;
	private Dependent fetcher;

	@Before
	public void before() {
		this.artifact = new DefaultArtifact("org.neo4j", "neo4j", "jar", "3.0.5");
		this.fetcher = new Dependent();
	}

	@Test
	public void testFetchPage()
			throws ClientProtocolException, ParseException, IOException, URISyntaxException, NotOkResponseException {
		List<Artifact> artifacts = this.fetcher.fetchPage(Dependent.newUri(this.artifact)).getElements();
		Assert.assertTrue(artifacts.size() > 1);
	}

	@Test
	public void testPagination()
			throws ClientProtocolException, ParseException, IOException, URISyntaxException, NotOkResponseException {
		Page<URI, Artifact> page1 = this.fetcher.fetchPage(Dependent.newUri(this.artifact));
		Page<URI, Artifact> page2 = this.fetcher.fetchPage(page1.getKey().get());
		// make sure pages are different
		Assert.assertEquals(page1.getElements().get(0), page1.getElements().get(0));
		Assert.assertNotEquals(page1.getElements().get(0), page2.getElements().get(0));
	}

}
