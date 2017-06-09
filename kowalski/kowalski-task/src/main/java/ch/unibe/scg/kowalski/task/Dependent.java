package ch.unibe.scg.kowalski.task;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Dependent {

	public static class NotOkResponseException extends Exception {

		protected static final long serialVersionUID = 1L;

		public NotOkResponseException(String message) {
			super(message);
		}

	}

	public static URI newUri(Artifact artifact) throws URISyntaxException {
		return new URI(String.format("http://mvnrepository.com/artifact/%s/%s/usages", artifact.getGroupId(),
				artifact.getArtifactId()));
	}

	protected CloseableHttpClient client;

	public Dependent() {
		this.client = HttpClients.createSystem();
	}

	public Page<URI, Artifact> fetchPage(URI uri)
			throws ClientProtocolException, IOException, URISyntaxException, ParseException, NotOkResponseException {
		HttpGet request = new HttpGet(uri);
		try (CloseableHttpResponse response = this.client.execute(request)) {
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				return new Page<>(Optional.empty(), Collections.emptyList());
			}
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw new NotOkResponseException(
						String.format("Service response not ok %s %s %s", response.getStatusLine(),
								response.getAllHeaders(), EntityUtils.toString(response.getEntity())));
			}
			Document document = Jsoup.parse(EntityUtils.toString(response.getEntity()), uri.toString());
			Optional<URI> next = Optional.empty();
			Elements nexts = document.select(".search-nav li:last-child a[href]");
			if (!nexts.isEmpty()) {
				next = Optional.of(new URI(nexts.first().attr("abs:href")));
			}
			List<Artifact> artifacts = document.select(".im .im-subtitle").stream()
					.map(element -> new DefaultArtifact(element.select("a:nth-child(1)").first().text(),
							element.select("a:nth-child(2)").first().text(), null, null))
					.collect(Collectors.toList());
			return new Page<>(next, artifacts);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		this.client.close();
	}

}
