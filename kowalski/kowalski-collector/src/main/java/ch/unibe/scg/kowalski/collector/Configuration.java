package ch.unibe.scg.kowalski.collector;

import java.util.function.Supplier;

import javax.jms.ConnectionFactory;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jms.core.JmsTemplate;

@org.springframework.context.annotation.Configuration
@Import(ch.unibe.scg.kowalski.worker.Configuration.class)
public class Configuration {

	public static final String PACKAGE = "ch.unibe.scg.kowalski.collector";

	@Value("${" + PACKAGE + ".brokerUrl:#{\"tcp://localhost:61616\"}}")
	private String brokerUrl;

	@Bean
	public ConnectionFactory connectionFactory() {
		return new ActiveMQJMSConnectionFactory(this.brokerUrl);
	}

	@Bean
	public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
	}

	@Bean
	public WorkerPoolParser workerPoolParser(JmsTemplate jmsTemplate,
			@Qualifier(ch.unibe.scg.kowalski.worker.match.Configuration.PACKAGE
					+ ".processorFactory") Supplier<ch.unibe.scg.kowalski.worker.match.Processor> processorFactoryMatch,
			@Qualifier(ch.unibe.scg.kowalski.worker.dependent.Configuration.PACKAGE
					+ ".processorFactory") Supplier<ch.unibe.scg.kowalski.worker.dependent.Processor> processorFactoryDependent,
			@Qualifier(ch.unibe.scg.kowalski.worker.dependency.Configuration.PACKAGE
					+ ".processorFactory") Supplier<ch.unibe.scg.kowalski.worker.dependency.Processor> processorFactoryDependency) {
		return new WorkerPoolParser(jmsTemplate, processorFactoryMatch, processorFactoryDependent,
				processorFactoryDependency);
	}

}
