package ch.unibe.scg.kowalski.serialization;

import java.io.IOException;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.codehaus.plexus.util.Base64;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

@SuppressWarnings("serial")
public class SolrQueryDeserializer extends StdDeserializer<SolrQuery> {

	protected static final String SERIALIZATION = "serialization";

	public SolrQueryDeserializer() {
		this(SolrQuery.class);
	}

	public SolrQueryDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public SolrQuery deserialize(JsonParser parser, DeserializationContext context)
			throws IOException, JsonProcessingException {
		JsonNode node = parser.getCodec().readTree(parser);
		String serialization = node.get(SERIALIZATION).textValue();
		return SerializationUtils.deserialize(Base64.decodeBase64(serialization.getBytes()));
	}

}
