package ch.unibe.scg.kowalski.worker;

import javax.jms.Destination;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.jms.core.JmsTemplate;

public class Reader<I> implements ItemReader<I> {

	protected JmsTemplate jmsTemplate;
	protected Destination destination;

	public Reader(JmsTemplate jmsTemplate, Destination destination) {
		this.jmsTemplate = jmsTemplate;
		this.destination = destination;
	}

	@Override
	public I read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		@SuppressWarnings("unchecked")
		I request = (I) this.jmsTemplate.receiveAndConvert(this.destination);
		if (request == null) {
			return null;
		}
		return request;
	}

}
