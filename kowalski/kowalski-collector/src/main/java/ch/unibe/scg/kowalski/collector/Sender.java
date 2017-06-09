package ch.unibe.scg.kowalski.collector;

import java.util.function.Function;

import javax.jms.Destination;

import org.springframework.jms.core.JmsTemplate;

public class Sender {

	private JmsTemplate jmsTemplate;
	private Destination destination;
	private Function<Object, Object> mapper;

	public Sender(JmsTemplate jmsTemplate, Destination destination, Function<Object, Object> mapper) {
		this.jmsTemplate = jmsTemplate;
		this.destination = destination;
		this.mapper = mapper;
	}

	public void send(Object object) {
		this.jmsTemplate.convertAndSend(this.destination, this.mapper.apply(object));
	}

}
