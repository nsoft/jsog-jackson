package com.voodoodyne.jackson.jsog;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.junit.Test;
import org.testng.collections.Lists;

public class Issue26Test {

  // @formatter:off
  String TEST_JSON="[\"java.util.ArrayList\",[" +

      "{" +
        "\"@class\":\"com.voodoodyne.jackson.jsog.Issue26Test$Outer\"," +
        "\"@id\":\"1\"," +
        "\"inner\":{" +
          "\"@class\":\"com.voodoodyne.jackson.jsog.Issue26Test$Inner\"," +
          "\"@id\":\"2\"," +
          "\"outer\":{" +
            "\"@class\":\"com.voodoodyne.jackson.jsog.Issue26Test$Outer\"," + // <-- added for issue 26
            "\"@ref\":\"1\"" +
          "}" +
        "}" +
      "}," +

      "{\"" +
        "@class\":\"com.voodoodyne.jackson.jsog.Issue26Test$Inner\"," + // <-- added for issue 26
        "\"@ref\":\"2\"}" +
      "]]";

  // @formatter:on
  @Test
  public void testDeserializeWithDefaultTyping() throws JsonProcessingException {
    PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build();
    ObjectMapper mapper = new ObjectMapper()
        .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.EVERYTHING,
            JsonTypeInfo.As.PROPERTY);
    // also test this when jackson is upgraded...
    // List<Object> target = mapper.readerForListOf(Object.class).readValue(json);
    mapper.readValue(TEST_JSON, ArrayList.class);

  }

  @Test
  public void testSerializeWithDefaultTypeing() throws JsonProcessingException {
    Outer outer = new Outer();
    Inner inner = new Inner(outer);
    // Turn on type info
    PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build();
    ObjectMapper mapper = new ObjectMapper()
        .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.EVERYTHING,
            JsonTypeInfo.As.PROPERTY);


    List<Object> source = Lists.newArrayList(outer, inner);
    String json = mapper.writeValueAsString(source);

    assertEquals(TEST_JSON,json);
  }

  // classes used in this test

  @SuppressWarnings("unused")
  @JsonIdentityInfo(generator = JSOGGenerator.class)
  public static class Outer {
    private Inner inner;

    public Inner getInner() {
      return inner;
    }

    public void setInner(Inner inner) {
      this.inner = inner;
    }
  }

  @SuppressWarnings("unused")
  @JsonIdentityInfo(generator = JSOGGenerator.class)
  public static class Inner {

    private Outer outer;

    Inner() {
    }

    public Inner(Outer outer) {
      this.outer = outer;
      outer.inner = this;
    }

    public Outer getOuter() {
      return outer;
    }

    public void setOuter(Outer outer) {
      this.outer = outer;
    }
  }


}
