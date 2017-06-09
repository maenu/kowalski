package ch.unibe.scg.kowalski.worker.match;

import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;

import ch.unibe.scg.kowalski.task.Match;

@org.springframework.context.annotation.Configuration(Configuration.PACKAGE + ".configuration")
public class Configuration {

	public static final String PACKAGE = "ch.unibe.scg.kowalski.worker.match";

	@Bean(PACKAGE + ".processorFactory")
	public Supplier<Processor> processorFactory() {
		return () -> new Processor(new Match());
	}

}