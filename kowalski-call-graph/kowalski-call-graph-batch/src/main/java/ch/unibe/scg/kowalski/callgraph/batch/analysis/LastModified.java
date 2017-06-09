package ch.unibe.scg.kowalski.callgraph.batch.analysis;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.aether.artifact.Artifact;

public class LastModified {

	public static class NotOkResponseException extends Exception {

		protected static final long serialVersionUID = 1L;

		public NotOkResponseException(String message) {
			super(message);
		}

	}

	public static URI newUri(Artifact artifact) throws URISyntaxException {
		return new URI(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s",
				artifact.getGroupId().replaceAll("\\.", "/"), artifact.getArtifactId(), artifact.getBaseVersion(),
				artifact.getFile().getName()));
	}

	protected CloseableHttpClient client;
	protected SimpleDateFormat format;

	public LastModified(File cache) {
		this.client = CachingHttpClients.custom().setCacheDir(cache).useSystemProperties().build();
		this.format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	}

	public Date fetch(URI uri) throws ClientProtocolException, IOException, ParseException, NotOkResponseException,
			java.text.ParseException {
		HttpHead request = new HttpHead(uri);
		try (CloseableHttpResponse response = this.client.execute(request)) {
			EntityUtils.consume(response.getEntity());
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw new NotOkResponseException(String.format("Service response not ok %s %s %s", uri.toString(),
						response.getStatusLine(), response.getAllHeaders()));
			}
			String lastModified = response.getFirstHeader("Last-Modified").getValue();
			return this.format.parse(lastModified);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		this.client.close();
	}

}
