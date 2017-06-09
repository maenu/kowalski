package ch.unibe.scg.kowalski.worker;

import javax.jms.ConnectionFactory;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;

@org.springframework.context.annotation.Configuration
@EnableBatchProcessing
@Import({ ch.unibe.scg.kowalski.worker.dependency.Configuration.class,
		ch.unibe.scg.kowalski.worker.dependent.Configuration.class,
		ch.unibe.scg.kowalski.worker.match.Configuration.class })
public class Configuration {

	@Bean
	public MessageConverter messageConverter() {
		return new MessageConverter();
	}

	@Bean
	public JmsTemplate jmsTemplate(MessageConverter messageConverter, ConnectionFactory connectionFactory) {
		JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		jmsTemplate.setMessageConverter(messageConverter);
		return jmsTemplate;
	}

}
