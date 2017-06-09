package ch.unibe.scg.kowalski.worker;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.solr.client.solrj.SolrQuery;
import org.eclipse.aether.artifact.Artifact;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageType;

import com.fasterxml.jackson.databind.ObjectWriter;

import ch.unibe.scg.kowalski.serialization.Factory;
import ch.unibe.scg.kowalski.task.Page;

public class MessageConverter extends MappingJackson2MessageConverter {

	public MessageConverter() {
		super();
		// TODO set properties on mapper as in converter?
		this.setTargetType(MessageType.TEXT);
		this.setTypeIdPropertyName("_type");
		this.setObjectMapper((new Factory()).newObjectMapper());
	}

	@Override
	protected Message toMessage(Object object, Session session, ObjectWriter objectWriter)
			throws JMSException, MessageConversionException {
		Message message = super.toMessage(object, session, objectWriter);
		this.setDuplicateDetectionId(object, message);
		return message;
	}

	@Override
	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		Message message = super.toMessage(object, session);
		this.setDuplicateDetectionId(object, message);
		return message;
	}

	protected void setDuplicateDetectionId(Object object, Message message) throws JMSException {
		message.setStringProperty(org.apache.activemq.artemis.api.core.Message.HDR_DUPLICATE_DETECTION_ID.toString(),
				"" + this.toHash(object));
	}

	private int toHash(Object object) {
		// FIXME ugly af..
		if (object instanceof Artifact) {
			return ((Artifact) object).hashCode();
		} else if (object instanceof URI) {
			return ((URI) object).hashCode();
		} else if (object instanceof SolrQuery) {
			return ((SolrQuery) object).toString().hashCode();
		} else if (object instanceof Page) {
			Page<?, ?> page = ((Page<?, ?>) object);
			Optional<?> keyOptional = page.getKey();
			if (keyOptional.isPresent()) {
				return this.toHash(keyOptional.get());
			}
			return page.getElements().stream().map(element -> this.toHash(element))
					.collect(Collectors.reducing((a, b) -> a + b)).orElse(page.hashCode());
		}
		throw new IllegalArgumentException(
				String.format("Must be of ARtifact, URI, SolrQuery or Page, but was '%s'", object.toString()));
	}

}
