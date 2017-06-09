package ch.unibe.scg.kowalski.worker.match;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import ch.unibe.scg.kowalski.task.Match;
import ch.unibe.scg.kowalski.task.Page;

public class Processor implements ItemProcessor<SolrQuery, Page<SolrQuery, SolrDocument>> {

	protected static final Logger LOGGER = LoggerFactory.getLogger(Processor.class);

	protected Match fetcher;

	public Processor(Match fetcher) {
		super();
		this.fetcher = fetcher;
	}

	@Override
	public Page<SolrQuery, SolrDocument> process(SolrQuery query) throws Exception {
		LOGGER.info("Processing {}", query);
		return this.fetcher.fetchPage(query);
	}

}
