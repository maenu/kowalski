package ch.unibe.scg.kowalski.serialization;

import org.eclipse.aether.artifact.Artifact;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;

public class ArtifactDeserializerModifier extends BeanDeserializerModifier {

	@Override
	public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc,
			JsonDeserializer<?> deserializer) {
		if (Artifact.class.isAssignableFrom(beanDesc.getType().getRawClass())) {
			return new ArtifactDeserializer();
		}
		return super.modifyDeserializer(config, beanDesc, deserializer);
	}

}
