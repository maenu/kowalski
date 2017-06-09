package ch.unibe.scg.kowalski.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import ch.unibe.scg.kowalski.task.Page;

@SuppressWarnings({ "serial", "rawtypes" })
public class PageSerializer extends StdSerializer<Page> {

	protected static final String ELEMENT_CLASS = "elementClass";
	protected static final String ELEMENTS = "elements";
	protected static final String KEY_CLASS = "keyClass";
	protected static final String KEY = "key";

	public PageSerializer() {
		this(Page.class);
	}

	public PageSerializer(Class<Page> vc) {
		super(vc);
	}

	@Override
	public void serialize(Page value, JsonGenerator generator, SerializerProvider provider) throws IOException {
		generator.writeStartObject();
		generator.writeStringField(ELEMENT_CLASS, value.getElements().isEmpty() ? Object.class.getName()
				: value.getElements().get(0).getClass().getName());
		generator.writeObjectField(ELEMENTS, value.getElements());
		if (value.getKey().isPresent()) {
			Object key = value.getKey().get();
			generator.writeStringField(KEY_CLASS, key.getClass().getName());
			generator.writeObjectField(KEY, key);
		}
		generator.writeEndObject();
	}

}
