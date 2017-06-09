package ch.unibe.scg.kowalski.callgraph.batch.analysis;

import javax.jms.Destination;

import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.jms.core.JmsTemplate;

import ch.unibe.scg.kowalski.task.Page;

public class Reader extends ch.unibe.scg.kowalski.worker.Reader<Page<Artifact, Artifact>> {

	public static final String PATIENT = "patient";
	private static final Logger LOGGER = LoggerFactory.getLogger(Reader.class);

	public Reader(JmsTemplate jmsTemplate, Destination destination) {
		super(jmsTemplate, destination);
	}

	@Override
	public Page<Artifact, Artifact> read()
			throws UnexpectedInputException, ParseException, NonTransientResourceException, Exception {
		MDC.remove(PATIENT);
		Page<Artifact, Artifact> page = super.read();
		while (page != null) {
			Artifact patient = page.getKey().get();
			if (patient.getFile() == null) {
				LOGGER.info("Skip {} as it is not fetched", patient);
				page = super.read();
				continue;
			}
			if (!patient.getExtension().equals("jar")) {
				LOGGER.info("Skip {} as it is not packaged as jar", patient);
				page = super.read();
				continue;
			}
			if (patient.isSnapshot()) {
				LOGGER.info("Skip {} as it is snapshot", patient);
				page = super.read();
				continue;
			}
			LOGGER.info("Processing {}...", patient);
			MDC.put(PATIENT, patient.toString());
			return page;
		}
		return null;
	}

}
