package ch.unibe.scg.kowalski.task;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

public class Match {

	public static Artifact newArtifactWithVersion(SolrDocument document) {
		String groupId = (String) document.getFieldValue("g");
		String artifactId = (String) document.getFieldValue("a");
		String classifier = null;
		String extension = (String) document.getFieldValue("p");
		String version = (String) document.getFieldValue("v");
		Map<String, String> properties = new HashMap<>();
		properties.put("timestamp", ((Long) document.getFieldValue("timestamp")).toString());
		File file = null;
		return new DefaultArtifact(groupId, artifactId, classifier, extension, version, properties, file);
	}

	public static Artifact newArtifactWithLatestVersion(SolrDocument document) {
		String groupId = (String) document.getFieldValue("g");
		String artifactId = (String) document.getFieldValue("a");
		String classifier = null;
		String extension = (String) document.getFieldValue("p");
		String version = (String) document.getFieldValue("latestVersion");
		Map<String, String> properties = new HashMap<>();
		properties.put("timestamp", ((Long) document.getFieldValue("timestamp")).toString());
		File file = null;
		return new DefaultArtifact(groupId, artifactId, classifier, extension, version, properties, file);
	}

	public static SolrQuery newSolrQueryForLatestVersion(Artifact artifact) {
		// FIXME add custom query params
		SolrQuery solrQuery = new SolrQuery(
				String.format("g:\"%s\" AND a:\"%s\" AND p:\"jar\"", artifact.getGroupId(), artifact.getArtifactId()));
		return solrQuery;
	}

	public static SolrQuery newSolrQueryForAllVersions(Artifact artifact) {
		SolrQuery solrQuery = newSolrQueryForLatestVersion(artifact);
		solrQuery.set("core", "gav");
		return solrQuery;
	}

	protected static class XMLResponseParser extends org.apache.solr.client.solrj.impl.XMLResponseParser {

		@Override
		public String getContentType() {
			return "text/xml; charset=UTF-8";
		}

	}

	protected HttpSolrClient client;

	public Match() {
		this.client = new HttpSolrClient.Builder("http://search.maven.org/solrsearch")
				.withResponseParser(new XMLResponseParser()).build();
	}

	public SolrDocumentList fetchDocumentList(SolrQuery query) throws SolrServerException, IOException {
		return this.client.query(query).getResults();
	}

	public Page<SolrQuery, SolrDocument> fetchPage(SolrQuery query) throws SolrServerException, IOException {
		SolrDocumentList documentList = this.fetchDocumentList(query);
		Optional<SolrQuery> next = Optional.empty();
		if (documentList.getStart() + documentList.size() < documentList.getNumFound()) {
			SolrQuery copy = query.getCopy();
			copy.setStart((int) (documentList.getStart() + documentList.size()));
			next = Optional.of(copy);
		}
		return new Page<>(next, documentList);
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		this.client.close();
	}

}
