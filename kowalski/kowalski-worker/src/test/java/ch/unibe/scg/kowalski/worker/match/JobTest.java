package ch.unibe.scg.kowalski.worker.match;

import java.util.function.Supplier;

import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.activemq.artemis.junit.EmbeddedJMSResource;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ch.unibe.scg.kowalski.task.Page;
import ch.unibe.scg.kowalski.worker.Reader;
import ch.unibe.scg.kowalski.worker.Writer;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class JobTest {

	@org.springframework.context.annotation.Configuration
	@Import(ch.unibe.scg.kowalski.worker.TestConfiguration.class)
	public static class Configuration {

		@Autowired
		private JobBuilderFactory jobBuilderFactory;
		@Autowired
		private StepBuilderFactory stepBuilderFactory;
		@Autowired
		private JmsTemplate jmsTemplate;

		@Bean
		public Job job(
				@Qualifier(ch.unibe.scg.kowalski.worker.match.Configuration.PACKAGE
						+ ".processorFactory") Supplier<Processor> processorFactory,
				ActiveMQQueue destinationInput, ActiveMQQueue destinationOutput) {
			ItemReader<SolrQuery> reader = new Reader<>(this.jmsTemplate, destinationInput);
			ItemWriter<Page<SolrQuery, SolrDocument>> writer = new Writer<>(this.jmsTemplate, destinationOutput);
			Step step = this.stepBuilderFactory.get(ch.unibe.scg.kowalski.worker.match.Configuration.PACKAGE)
					.<SolrQuery, Page<SolrQuery, SolrDocument>>chunk(1).reader(reader).processor(processorFactory.get())
					.writer(writer).build();
			Job job = this.jobBuilderFactory.get(ch.unibe.scg.kowalski.worker.match.Configuration.PACKAGE).start(step)
					.build();
			return job;
		}

	}

	@Rule
	public EmbeddedJMSResource resource;
	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;
	@Autowired
	private JmsTemplate jmsTemplate;
	@Autowired
	private ActiveMQQueue destinationInput;
	@Autowired
	private ActiveMQQueue destinationOutput;

	public JobTest() {
		this.resource = new EmbeddedJMSResource();
	}

	@Test
	public void testJob() throws Exception {
		SolrQuery input = new SolrQuery("g:org.neo4j");
		this.jmsTemplate.convertAndSend(this.destinationInput, input);
		BatchStatus status = this.jobLauncherTestUtils.launchJob().getStatus();
		Assert.assertEquals(BatchStatus.COMPLETED, status);
		@SuppressWarnings("unchecked")
		Page<SolrQuery, SolrDocument> output = (Page<SolrQuery, SolrDocument>) this.jmsTemplate
				.receiveAndConvert(this.destinationOutput);
		Assert.assertTrue(output.getKey().get() instanceof SolrQuery);
		Assert.assertTrue(output.getElements().get(0) instanceof SolrDocument);
		Assert.assertEquals(output.getElements().size(), 10);
		Assert.assertNull(this.jmsTemplate.receive(this.destinationOutput));
	}

	@Test
	public void testDuplication() throws Exception {
		this.jmsTemplate.convertAndSend(this.destinationInput, new SolrQuery("g:org.neo4j"));
		this.jmsTemplate.convertAndSend(this.destinationInput, new SolrQuery("g:org.neo4j"));
		this.jobLauncherTestUtils.launchJob().getStatus();
		Assert.assertNotNull(this.jmsTemplate.receive(this.destinationOutput));
		Assert.assertNull(this.jmsTemplate.receive(this.destinationOutput));
	}

	@Test
	public void testNoDuplication() throws Exception {
		this.jmsTemplate.convertAndSend(this.destinationInput, new SolrQuery("g:org.neo4j.*"));
		this.jmsTemplate.convertAndSend(this.destinationInput, new SolrQuery("g:org.neo4j"));
		this.jobLauncherTestUtils.launchJob().getStatus();
		Assert.assertNotNull(this.jmsTemplate.receive(this.destinationOutput));
		Assert.assertNotNull(this.jmsTemplate.receive(this.destinationOutput));
	}

}
