package ch.unibe.scg.kowalski.serialization;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SuppressWarnings("serial")
public class ArtifactDeserializer extends StdDeserializer<Artifact> {

	protected static final String GROUP_ID = "groupId";
	protected static final String ARTIFACT_ID = "artifactId";
	protected static final String CLASSIFIER = "classifier";
	protected static final String EXTENSION = "extension";
	protected static final String VERSION = "version";
	protected static final String PROPERTIES = "properties";
	protected static final String FILE = "file";

	public ArtifactDeserializer() {
		this(Artifact.class);
	}

	public ArtifactDeserializer(Class<Artifact> vc) {
		super(vc);
	}

	@Override
	public Artifact deserialize(JsonParser parser, DeserializationContext context)
			throws IOException, JsonProcessingException {
		JsonNode node = parser.getCodec().readTree(parser);
		String groupId = node.get(GROUP_ID).textValue();
		String artifactId = node.get(ARTIFACT_ID).textValue();
		String classifier = node.get(CLASSIFIER).textValue();
		String extension = node.get(EXTENSION).textValue();
		String version = node.get(VERSION).textValue();
		Map<String, String> properties = null;
		File file = null;
		if (node.has(FILE)) {
			file = new File(node.get(FILE).textValue());
		}
		if (node.has(PROPERTIES)) {
			properties = new HashMap<>();
			ObjectNode propertiesNode = (ObjectNode) node.get(PROPERTIES);
			Iterator<Map.Entry<String, JsonNode>> propertyEntryIterator = propertiesNode.fields();
			while (propertyEntryIterator.hasNext()) {
				Map.Entry<String, JsonNode> propertyEntry = propertyEntryIterator.next();
				properties.put(propertyEntry.getKey(), propertyEntry.getValue().textValue());
			}
		}
		return new DefaultArtifact(groupId, artifactId, classifier, extension, version, properties, file);
	}

}
