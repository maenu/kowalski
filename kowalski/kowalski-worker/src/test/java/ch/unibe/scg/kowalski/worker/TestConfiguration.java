package ch.unibe.scg.kowalski.worker;

import javax.jms.ConnectionFactory;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;

@org.springframework.context.annotation.Configuration
@Import(ch.unibe.scg.kowalski.worker.Configuration.class)
public class TestConfiguration {

	@Bean
	public ConnectionFactory connectionFactory() {
		return new ActiveMQJMSConnectionFactory("vm://0");
	}

	@Bean
	public JmsTemplate jmsTemplate(MessageConverter messageConverter, ConnectionFactory connectionFactory) {
		JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		jmsTemplate.setMessageConverter(messageConverter);
		jmsTemplate.setReceiveTimeout(100);
		return jmsTemplate;
	}

	@Bean
	public JobLauncherTestUtils jobLauncherTestUtils() {
		return new JobLauncherTestUtils();
	}

	@Bean
	public ActiveMQQueue destinationInput() {
		return new ActiveMQQueue("input");
	}

	@Bean
	public ActiveMQQueue destinationOutput() {
		return new ActiveMQQueue("output");
	}

}
