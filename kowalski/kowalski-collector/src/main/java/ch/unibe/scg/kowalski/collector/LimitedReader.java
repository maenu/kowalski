package ch.unibe.scg.kowalski.collector;

import javax.jms.Destination;

import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.jms.core.JmsTemplate;

import ch.unibe.scg.kowalski.worker.Reader;

public class LimitedReader<I> extends Reader<I> {

	private int limit;

	public LimitedReader(JmsTemplate jmsTemplate, Destination destination, int limit) {
		super(jmsTemplate, destination);
		this.limit = limit;
	}

	@Override
	public I read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		if (this.limit <= 0) {
			return null;
		}
		this.limit = this.limit - 1;
		return super.read();
	}

}
