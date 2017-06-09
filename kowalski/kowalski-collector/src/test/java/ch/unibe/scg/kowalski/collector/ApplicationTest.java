package ch.unibe.scg.kowalski.collector;

import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.activemq.artemis.junit.EmbeddedJMSResource;
import org.eclipse.aether.artifact.Artifact;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ch.unibe.scg.kowalski.task.Page;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class ApplicationTest {

	@org.springframework.context.annotation.Configuration
	@Import({ Application.class })
	public static class Configuration {

	}

	@Rule
	public EmbeddedJMSResource resource;
	@Autowired
	private Application application;
	@Autowired
	private JmsTemplate jmsTemplate;

	public ApplicationTest() {
		this.resource = new EmbeddedJMSResource();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testJob() throws Exception {
		ActiveMQQueue destinationInput = new ActiveMQQueue("input");
		ActiveMQQueue destinationOutput = new ActiveMQQueue("output");
		this.application.start(new String[] { "g:com.google.code.gson", destinationInput.getQueueName() });
		Page<Artifact, Artifact> output = (Page<Artifact, Artifact>) this.jmsTemplate
				.receiveAndConvert(destinationOutput);
		Assert.assertTrue(output.getKey().get() instanceof Artifact);
		Assert.assertNotNull(output.getKey().get().getProperties().get("timestamp"));
		Assert.assertEquals(output.getElements().size(), 209);
		Assert.assertTrue(output.getElements().get(0) instanceof Artifact);
		output = (Page<Artifact, Artifact>) this.jmsTemplate.receiveAndConvert(destinationOutput);
		Assert.assertTrue(output.getKey().get() instanceof Artifact);
		Assert.assertEquals(output.getElements().size(), 207);
		Assert.assertTrue(output.getElements().get(0) instanceof Artifact);
		this.jmsTemplate.setReceiveTimeout(100);
		Assert.assertNull(this.jmsTemplate.receive(destinationOutput));
	}

}
