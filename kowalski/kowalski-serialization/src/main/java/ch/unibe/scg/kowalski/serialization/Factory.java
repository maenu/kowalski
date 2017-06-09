package ch.unibe.scg.kowalski.serialization;

import org.apache.solr.client.solrj.SolrQuery;
import org.eclipse.aether.artifact.Artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import ch.unibe.scg.kowalski.task.Page;

// TODO jackson serialization is quite cumbersome, find better solution
public class Factory {

	public ObjectMapper newObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Artifact.class, new ArtifactDeserializer());
		module.addDeserializer(SolrQuery.class, new SolrQueryDeserializer());
		module.addDeserializer(Page.class, new PageDeserializer(objectMapper));
		module.addSerializer(Artifact.class, new ArtifactSerializer());
		module.addSerializer(SolrQuery.class, new SolrQuerySerializer());
		module.addSerializer(Page.class, new PageSerializer());
		module.setDeserializerModifier(new ArtifactDeserializerModifier());
		objectMapper.registerModule(module);
		return objectMapper;
	}

}
