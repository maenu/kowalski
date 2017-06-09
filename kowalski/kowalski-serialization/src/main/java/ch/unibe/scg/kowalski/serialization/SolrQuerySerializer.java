package ch.unibe.scg.kowalski.serialization;

import java.io.IOException;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.codehaus.plexus.util.Base64;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@SuppressWarnings("serial")
public class SolrQuerySerializer extends StdSerializer<SolrQuery> {

	protected static final String SERIALIZATION = "serialization";

	public SolrQuerySerializer() {
		this(SolrQuery.class);
	}

	public SolrQuerySerializer(Class<SolrQuery> vc) {
		super(vc);
	}

	@Override
	public void serialize(SolrQuery value, JsonGenerator generator, SerializerProvider provider) throws IOException {
		generator.writeStartObject();
		generator.writeStringField(SERIALIZATION, new String(Base64.encodeBase64(SerializationUtils.serialize(value))));
		generator.writeEndObject();
	}

}
