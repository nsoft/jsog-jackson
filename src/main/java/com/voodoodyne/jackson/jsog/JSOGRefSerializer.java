package com.voodoodyne.jackson.jsog;


import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

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

}
