package ch.unibe.scg.kowalski.task;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.eclipse.aether.artifact.Artifact;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.unibe.scg.kowalski.task.Match;
import ch.unibe.scg.kowalski.task.Page;

public class MatchTest {

	private String query;
	private Match fetcher;

	@Before
	public void before() {
		this.query = "g:org.neo4j";
		this.fetcher = new Match();
	}

	@Test
	public void testFetchPage() throws SolrServerException, IOException {
		List<Artifact> artifacts = this.fetcher.fetchPage(new SolrQuery(this.query)).getElements().stream()
				.map(Match::newArtifactWithVersion).collect(Collectors.toList());
		Assert.assertTrue(artifacts.size() > 1);
	}

	@Test
	public void testPagination() throws SolrServerException, IOException {
		Page<SolrQuery, SolrDocument> page1 = this.fetcher.fetchPage(new SolrQuery(this.query));
		Page<SolrQuery, SolrDocument> page2 = this.fetcher.fetchPage(page1.getKey().get());
		// make sure pages are different
		Assert.assertEquals(page1.getElements().get(0), page1.getElements().get(0));
		Assert.assertNotEquals(page1.getElements().get(0), page2.getElements().get(0));
	}

}
