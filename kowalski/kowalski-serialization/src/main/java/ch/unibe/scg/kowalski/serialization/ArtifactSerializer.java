package ch.unibe.scg.kowalski.serialization;

import java.io.IOException;
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@SuppressWarnings("serial")
public class ArtifactSerializer extends StdSerializer<Artifact> {

	protected static final String GROUP_ID = "groupId";
	protected static final String ARTIFACT_ID = "artifactId";
	protected static final String CLASSIFIER = "classifier";
	protected static final String EXTENSION = "extension";
	protected static final String VERSION = "version";
	protected static final String PROPERTIES = "properties";
	protected static final String FILE = "file";

	public ArtifactSerializer() {
		this(Artifact.class);
	}

	public ArtifactSerializer(Class<Artifact> vc) {
		super(vc);
	}

	@Override
	public void serialize(Artifact value, JsonGenerator generator, SerializerProvider provider) throws IOException {
		generator.writeStartObject();
		generator.writeStringField(GROUP_ID, value.getGroupId());
		generator.writeStringField(ARTIFACT_ID, value.getArtifactId());
		generator.writeStringField(CLASSIFIER, value.getClassifier());
		generator.writeStringField(EXTENSION, value.getExtension());
		generator.writeStringField(VERSION, value.getVersion());
		if (value.getFile() != null) {
			generator.writeStringField(FILE, value.getFile().getPath());
		}
		if (value.getProperties() != null) {
			generator.writeFieldName(PROPERTIES);
			generator.writeStartObject();
			for (Map.Entry<String, String> entry : value.getProperties().entrySet()) {
				generator.writeStringField(entry.getKey(), entry.getValue());
			}
			generator.writeEndObject();
		}
		generator.writeEndObject();
	}

}
