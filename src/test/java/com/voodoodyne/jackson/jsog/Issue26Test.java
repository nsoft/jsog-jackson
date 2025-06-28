package com.voodoodyne.jackson.jsog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.junit.Test;
import org.testng.collections.Lists;

public class Issue26Test {

  public static final String WIHOUT_DEFAULT_TYPING = "[{\"@id\":\"1\",\"inner\":{\"@id\":\"2\",\"outer\":{\"@ref\":\"1\"}}},{\"@ref\":\"2\"}]";
  public static final String TEST_JSON="[\"java.util.ArrayList\",[" +

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
    @SuppressWarnings("rawtypes")
    ArrayList list = mapper.readValue(TEST_JSON, ArrayList.class);
    assertEquals(2, list.size());
    assertEquals(Outer.class, list.get(0).getClass()); // prove it's not a list of map objects
    assertEquals(Inner.class, list.get(1).getClass());
    assertSame(((Outer)list.get(0)).inner, list.get(1));
    assertSame(((Inner)list.get(1)).outer, list.get(0));
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
    // sadly the following throws a null pointer exception
    // mapper.getSerializerProvider().setAttribute(JSOGGenerator.DEFAULT_TYPING, "@class");

    // probably there is a better way to do this but it wasn't easy to find quickly.
    SerializationConfig config = mapper.getSerializationConfig().withAttribute(JSOGGenerator.DEFAULT_TYPING_ATTRIBUTE, "@class");
    mapper = new ObjectMapper()
        .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.EVERYTHING,
            JsonTypeInfo.As.PROPERTY).setConfig(config);
    List<Object> source = Lists.newArrayList(outer, inner);
    String json = mapper.writeValueAsString(source);
    assertEquals(TEST_JSON,json);
  }

  // This will also be caught by various other tests, but for completeness of this issue
  // we should also explicitly test it here with the same objects
  @Test
  public void testSerializeWithoutDefaultTypeing() throws JsonProcessingException {
    Outer outer = new Outer();
    Inner inner = new Inner(outer);
    // Turn on type info
    ObjectMapper mapper = new ObjectMapper();

    List<Object> source = Lists.newArrayList(outer, inner);
    String json = mapper.writeValueAsString(source);

    // make sure our test json is truly valid - this round trips through a list of maps
    @SuppressWarnings("rawtypes")
    ArrayList l = mapper.readValue(WIHOUT_DEFAULT_TYPING, ArrayList.class);
    assertEquals(WIHOUT_DEFAULT_TYPING, mapper.writeValueAsString(l));

    // type info should not be written unless Default typing is on
    assertEquals(WIHOUT_DEFAULT_TYPING,json);
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
