package com.voodoodyne.jackson.jsog;


import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Knows how to take either form of a JSOGRef (string or {@ref:string} and convert it back into a JSOGRef.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class JSOGRefDeserializer extends JsonDeserializer<JSOGRef>
{
	@Override
	public JSOGRef deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
		JsonNode node = ctx.readValue(jp, JsonNode.class);
		if (node.isTextual()) {
			return new JSOGRef(node.asText());
		} else {
			return new JSOGRef(node.get(JSOGRef.REF_KEY).asText());
		}
	}

	@Override
	public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
		// in the event of default typing, this gets called instead of deserialize above, and it seems to get
		// called for each @id (the first case) and for each {"@ref":"#"} (the second case)
		if (p.currentToken() == JsonToken.VALUE_STRING) {
			return new JSOGRef(p.getText());
		}
		if (p.currentToken() == JsonToken.FIELD_NAME) {
			// we are being called for { "@ref":"#" }
			//   and starting here ------^
			p.nextToken();
			if (p.currentToken() != JsonToken.VALUE_STRING) {
				throw new IllegalStateException("@ref attribute should be followed by a value?");
			}
			String text = p.getText(); //grab our id
			p.nextToken(); // consume END_OBJECT
			return new JSOGRef(text);
		} else {
			throw new IllegalStateException("Unexpected Token Type:" + p.currentToken().name());
		}
	}
}


