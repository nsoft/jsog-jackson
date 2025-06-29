package com.voodoodyne.jackson.jsog;


import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Knows how to take a JSOGRef and print it as @id or @ref as appropriate.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class JSOGRefSerializer extends JsonSerializer<JSOGRef>
{
	@Override
	public void serialize(JSOGRef value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
		if (value.used) {
			jgen.writeStartObject();
			Object attribute = provider.getAttribute(JSOGGenerator.DEFAULT_TYPING_ATTRIBUTE);
			if (attribute != null) {
				jgen.writeObjectField(attribute.toString(), value.refTo.getClass().getName());
			}
			jgen.writeObjectField(JSOGRef.REF_KEY, value.ref);
			jgen.writeEndObject();
		} else {
			value.used = true;
			jgen.writeObject(value.ref);
		}
	}

// Side Note: This never gets called, so we cant rely on it despite the fact we need its equivalent in the
// deserializer. I suspect ID serialization is not a first class citizen and jackson isn't expecting typing.
//
//	@Override
//	public void serializeWithType(JSOGRef value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
//		super.serializeWithType(value, gen, serializers, typeSer);
//	}
}
