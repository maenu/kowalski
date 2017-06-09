package ch.unibe.scg.kowalski.worker.dependent;

import java.net.URI;

import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import ch.unibe.scg.kowalski.task.Dependent;
import ch.unibe.scg.kowalski.task.Page;

public class Processor implements ItemProcessor<URI, Page<URI, Artifact>> {

	protected static final Logger LOGGER = LoggerFactory.getLogger(Processor.class);

	protected Dependent fetcher;

	public Processor(Dependent fetcher) {
		super();
		this.fetcher = fetcher;
	}

	@Override
	public Page<URI, Artifact> process(URI uri) throws Exception {
		LOGGER.info("Processing {}", uri);
		return this.fetcher.fetchPage(uri);
	}

}
