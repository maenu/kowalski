package ch.unibe.scg.kowalski.configurationlanguage;

import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;

import ch.unibe.scg.kowalski.configurationlanguage.Parser.WorkerPoolNode;

public class Facade {

	private Parser parser;

	public Facade() {
		this.parser = Parboiled.createParser(Parser.class);
	}

	public WorkerPoolNode parse(String source) {
		return (WorkerPoolNode) (new ReportingParseRunner<>(this.parser.WorkerPool())).run(source).resultValue;
	}

}
