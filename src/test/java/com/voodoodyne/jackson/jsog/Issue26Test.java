package com.voodoodyne.jackson.jsog;

import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.EVERYTHING;
import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testng.collections.Lists;

@RunWith(Parameterized.class)
public class Issue26Test {

  public static final String WITHOUT_DEFAULT_TYPING = "[{\"@id\":\"1\",\"inner\":{\"@id\":\"2\",\"outer\":{\"@ref\":\"1\"}}},{\"@ref\":\"2\"}]";

  // @formatter:off
  public static final String TEST_JSON=
    "[\"java.util.ArrayList\",[" +
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

  private final DefaultTyping defaultTyping;

  public Issue26Test(DefaultTyping defaultTyping) {
    this.defaultTyping = defaultTyping;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    ArrayList<Object[]> objects = new ArrayList<Object[]>();
    // sadly these don't work I tried adding a second attribute to let us know which
    // mode we are in, but adding @class in these cases throws an exception because
    // @class is not expected in one place and not adding it throws exceptions
    // when @class isn't found in another. There's some tricky subtlety in these formats
    // relating to typing I haven't figured out
    //    objects.add(new Object[]{DefaultTyping.JAVA_LANG_OBJECT});
    //    objects.add(new Object[]{DefaultTyping.OBJECT_AND_NON_CONCRETE});
    //    objects.add(new Object[]{DefaultTyping.NON_CONCRETE_AND_ARRAYS});

    // These work
    objects.add(new Object[]{NON_FINAL});
    objects.add(new Object[]{EVERYTHING});
    return objects;
  }


  @Test
  public void testDeserializeWithDefaultTyping() throws JsonProcessingException {
    PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build();
    ObjectMapper mapper = new ObjectMapper()
        .activateDefaultTyping(ptv, EVERYTHING,
            JsonTypeInfo.As.PROPERTY);
    System.out.println(defaultTyping);
    // also test this when jackson is upgraded...
    // List<Object> target = mapper.readerForListOf(Object.class).readValue(json);
    @SuppressWarnings("rawtypes")
    ArrayList list = mapper.readValue(TEST_JSON, ArrayList.class);
    assertEquals(2, list.size());
    assertEquals(Outer.class, list.get(0).getClass()); // prove it's not a list of map objects
    assertEquals(Inner.class, list.get(1).getClass());
    assertSame(((Outer) list.get(0)).inner, list.get(1));
    assertSame(((Inner) list.get(1)).outer, list.get(0));
  }

  @Test
  public void testRoundTrip() throws JsonProcessingException {

    Outer outer = new Outer();
    Inner inner = new Inner(outer);
    // Turn on type info
    PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build();
    ObjectMapper mapper = new ObjectMapper()
        .activateDefaultTyping(ptv, defaultTyping,
            JsonTypeInfo.As.PROPERTY);
    // sadly the following throws a null pointer exception
    // mapper.getSerializerProvider().setAttribute(JSOGGenerator.DEFAULT_TYPING, "@class");

    // probably there is a better way to do this without double instantiating the mapper,
    // but it wasn't easy to find. One can also set this directly when invoking the write
    // operations with mapper.writer().withAttribute(JSOGGenerator.DEFAULT_TYPING, "@class")
    // if single instantiation is important, but then you need to do it every time you write.
    SerializationConfig config = mapper.getSerializationConfig()
        // obviously this attribute must be coordinated with the actual value used in the JSON
        // (could also be "@c" or a custom name depending on config)
        .withAttribute(JSOGGenerator.DEFAULT_TYPING_ATTRIBUTE, "@class");

    mapper = new ObjectMapper()
        // FAIL_ON_UNKNOWN_PROPERTIES = false is dangerous if combined with any of
        // JAVA_LANG_OBJECT, OBJECT_AND_NON_CONCRETE, NON_CONCRETE_AND_ARRAYS
        // The final line of this test demonstrates that it can lead to duplication of objects.
        // .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .activateDefaultTyping(ptv, defaultTyping,
            JsonTypeInfo.As.PROPERTY).setConfig(config);
    List<Object> source = Lists.newArrayList(outer, inner);
    String json = mapper.writeValueAsString(source);

    // also test this when jackson is upgraded...
    // List<Object> target = mapper.readerForListOf(Object.class).readValue(json);
    @SuppressWarnings("rawtypes")
    ArrayList list = mapper.readValue(json, ArrayList.class);
    assertEquals(2, list.size());
    assertEquals(Outer.class, list.get(0).getClass()); // prove it's not a list of map objects
    assertEquals(Inner.class, list.get(1).getClass());
    assertSame(((Outer) list.get(0)).inner, list.get(1));

    // This is critical. It fails when .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    // and any of JAVA_LANG_OBJECT, OBJECT_AND_NON_CONCRETE, NON_CONCRETE_AND_ARRAYS are used;
    assertSame(((Inner) list.get(1)).outer, list.get(0));
  }

  @Test
  public void testSerializeWithDefaultTyping() throws JsonProcessingException {
    Outer outer = new Outer();
    Inner inner = new Inner(outer);
    // Turn on type info
    PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build();
    ObjectMapper mapper = new ObjectMapper()
        .activateDefaultTyping(ptv, EVERYTHING,
            JsonTypeInfo.As.PROPERTY);
    // sadly the following throws a null pointer exception
    // mapper.getSerializerProvider().setAttribute(JSOGGenerator.DEFAULT_TYPING, "@class");

    // probably there is a better way to do this but it wasn't easy to find quickly.
    SerializationConfig config = mapper.getSerializationConfig().withAttribute(JSOGGenerator.DEFAULT_TYPING_ATTRIBUTE, "@class");
    mapper = new ObjectMapper()
        .activateDefaultTyping(ptv, EVERYTHING,
            JsonTypeInfo.As.PROPERTY).setConfig(config);
    List<Object> source = Lists.newArrayList(outer, inner);
    String json = mapper.writeValueAsString(source);
    assertEquals(TEST_JSON, json);
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
    ArrayList l = mapper.readValue(WITHOUT_DEFAULT_TYPING, ArrayList.class);
    assertEquals(WITHOUT_DEFAULT_TYPING, mapper.writeValueAsString(l));

    // type info should not be written unless Default typing is on
    assertEquals(WITHOUT_DEFAULT_TYPING, json);
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
