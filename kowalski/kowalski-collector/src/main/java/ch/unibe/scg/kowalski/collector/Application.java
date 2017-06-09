package ch.unibe.scg.kowalski.collector;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jms.core.JmsTemplate;

@org.springframework.context.annotation.Configuration
@PropertySource(value = "classpath:application.properties")
@Import(Configuration.class)
public class Application {

	public static void main(String[] args) throws IOException, InterruptedException {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Application.class)) {
			Application application = context.getBean(Application.class);
			application.start(args);
		}
	}

	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	@Autowired
	private JobLauncher jobLauncher;
	@Autowired
	private JmsTemplate jmsTemplate;
	@Autowired
	private WorkerPoolParser workerPoolsParser;

	public void start(String[] args) throws IOException, InterruptedException {
		if (args.length == 2) {
			SolrQuery query = new SolrQuery(args[0]);
			ActiveMQQueue queue = new ActiveMQQueue(args[1]);
			this.jmsTemplate.convertAndSend(queue, query);
		} else if (args.length != 0) {
			throw new IllegalArgumentException("must have 0 or two arguments: query queue");
		}
		// read properties
		PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
		propertiesFactoryBean.setLocation(new ClassPathResource("collector.properties"));
		propertiesFactoryBean.afterPropertiesSet();
		Properties properties = propertiesFactoryBean.getObject();
		// parse worker pools
		Map<String, WorkerPool> workerPools = properties.entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getKey().toString(),
						entry -> this.workerPoolsParser.parse(entry.getValue().toString())));
		// build jobs
		Map<Job, Integer> jobs = workerPools.entrySet().stream().collect(Collectors.toMap(entry -> {
			String name = entry.getKey();
			WorkerPool workerPool = entry.getValue();
			@SuppressWarnings("unchecked")
			Step step = this.stepBuilderFactory.get(name).chunk(1).faultTolerant()
					.skipPolicy(new LoggingAlwaysSkipItemSkipPolicy()).noRollback(Throwable.class)
					.reader(workerPool.getReader())
					.processor((ItemProcessor<? super Object, ? extends Object>) workerPool.getProcessor())
					.writer((ItemWriter<? super Object>) workerPool.getWriter()).build();
			return this.jobBuilderFactory.get(name).start(step).build();
		}, entry -> entry.getValue().getSize()));
		// launch jobs
		List<JobExecution> jobExecutions = jobs.entrySet().stream()
				.flatMap(entry -> IntStream.range(0, entry.getValue()).mapToObj(i -> {
					JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
					jobParametersBuilder.addString("id", entry.getKey().getName() + "-" + i, true);
					try {
						return this.jobLauncher.run(entry.getKey(), jobParametersBuilder.toJobParameters());
					} catch (JobExecutionAlreadyRunningException | JobRestartException
							| JobInstanceAlreadyCompleteException | JobParametersInvalidException exception) {
						throw new RuntimeException(exception);
					}
				})).collect(Collectors.toList());
		// wait for termination
		while (jobExecutions.stream().anyMatch(jobExecution -> jobExecution.getStatus().isRunning())) {
			Thread.sleep(1000);
		}
	}

}
