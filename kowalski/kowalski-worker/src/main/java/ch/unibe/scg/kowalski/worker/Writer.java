package ch.unibe.scg.kowalski.worker;

import java.util.List;

import javax.jms.Destination;

import org.springframework.batch.item.ItemWriter;
import org.springframework.jms.core.JmsTemplate;

public class Writer<O> implements ItemWriter<O> {

	protected JmsTemplate jmsTemplate;
	protected Destination destination;

	public Writer(JmsTemplate jmsTemplate, Destination destination) {
		this.jmsTemplate = jmsTemplate;
		this.destination = destination;
	}

	@Override
	public void write(List<? extends O> items) throws Exception {
		items.stream().forEach(item -> this.jmsTemplate.convertAndSend(this.destination, item));
	}

}
