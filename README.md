# JavaScript Object Graphs with Jackson

This is a plugin for Jackson which can serialize cyclic object graphs in the [JSOG format](https://github.com/jsog/jsog).
It can both serialize and deserialize.

Caveat: With Jackson 2.5.0, polymoprhic (ie @JsonTypeInfo) objects cannot be deserialized from JSOG. Jackson 2.5.1
fixes the issue.

## Source code

The official repository is (https://github.com/jsog/jsog-jackson)

## Download

This plugin is available in Maven Central:

	<dependency>
		<groupId>com.voodoodyne.jackson.jsog</groupId>
		<artifactId>jackson-jsog</artifactId>
		<version>please look up latest version</version>
		<scope>compile</scope>
	</dependency>

It can be downloaded directly from [http://search.maven.org/]

## Usage

To use this plugin, annotate any classes which may contain references with *@JsonIdentityInfo(generator=JSOGGenerator.class)*.

    @JsonIdentityInfo(generator=JSOGGenerator.class)
    public class Person {
        String name;
        Person secretSanta;
    }

### Usage with Default Typing

If you are using [default typing](https://github.com/FasterXML/jackson-docs/wiki/JacksonPolymorphicDeserialization) to handle polymorphism there is a little more setup you will need. 
In short, you need to supply an attribute to the serializer. 
This attribute will signal that default typing is in play and specify the attribute name to use for expressing types (Usually this is `@class`). 
Examples and important warnings can be seen in [the unit test](blob/master/src/test/java/com/voodoodyne/jackson/jsog/Issue26Test.java) for DefaultTyping. Currently only NON_FINAL and EVERYTHING modes are supported.

**WARNING:** Default typing modes other than NON_FINAL and EVERYTHING may appear to succeed when `.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)` is used **but actually cause buggy deserialization** in which objects may become duplicated.
This is why only these two modes are currently supported (see note at end of `testRoundTrip()` in the unit test for details, enhancements welcome)


    
## Author

* Jeff Schnitzer (jeff@infohazard.org)

## License

This software is provided under the [MIT license](http://opensource.org/licenses/MIT)
