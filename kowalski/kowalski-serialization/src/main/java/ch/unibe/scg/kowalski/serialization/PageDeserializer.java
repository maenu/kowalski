package ch.unibe.scg.kowalski.serialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import ch.unibe.scg.kowalski.task.Page;

@SuppressWarnings({ "serial", "rawtypes" })
public class PageDeserializer extends StdDeserializer<Page> {

	protected ObjectMapper mapper;

	public PageDeserializer(ObjectMapper mapper) {
		this(Page.class, mapper);
	}

	protected PageDeserializer(Class<?> vc, ObjectMapper mapper) {
		super(vc);
		this.mapper = mapper;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Page deserialize(JsonParser parser, DeserializationContext context)
			throws IOException, JsonProcessingException {
		parser.nextValue();
		String elementClassName = parser.readValueAs(String.class);
		List elements = new ArrayList();
		try {
			Class<?> elementClass = Class.forName(elementClassName);
			parser.nextValue();
			elements = this.mapper.readValue(parser,
					context.getTypeFactory().constructCollectionType(List.class, elementClass));
		} catch (ClassNotFoundException exception) {
			throw new InvalidFormatException("elementClass can not be found: " + exception.getMessage(),
					elementClassName, Class.class);
		}
		if (parser.nextValue() != JsonToken.END_OBJECT) {
			String keyClassName = parser.readValueAs(String.class);
			try {
				Class<?> keyClass = Class.forName(keyClassName);
				parser.nextValue();
				Object key = parser.readValueAs(keyClass);
				return new Page(Optional.of(key), elements);
			} catch (ClassNotFoundException exception) {
				throw new InvalidFormatException("keyClass can not be found: " + exception.getMessage(), keyClassName,
						Class.class);
			}
		}
		return new Page(Optional.empty(), elements);
	}

}
