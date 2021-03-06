package ch.unibe.scg.kowalski.callgraph.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipPolicy;

//TODO resolve duplication with dataset builder
public class LoggingAlwaysSkipItemSkipPolicy implements SkipPolicy {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAlwaysSkipItemSkipPolicy.class);

	@Override
	public boolean shouldSkip(Throwable t, int skipCount) {
		LOGGER.warn("Skipping item", t);
		return true;
	}

}
