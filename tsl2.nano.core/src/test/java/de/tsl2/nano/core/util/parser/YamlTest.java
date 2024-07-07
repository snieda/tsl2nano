package de.tsl2.nano.core.util.parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class YamlTest {

    @Test
    public void testIsYaml() {
        assertTrue(new Yaml().isParseable("test: 1"));
        assertTrue(new Yaml().isParseable(getExpected()));

        assertFalse(new Yaml().isParseable(""));

        assertFalse(new Yaml().isParseable(new JSon().serialize(new TypeBean(0, "test", 0.1, null))));
        assertFalse(new Xml().isParseable(new JSon().serialize(new TypeBean(0, "test", 0.1, null))));
    }

    @Test
    public void testSerialization() {
        StructParserTest.testRoundtrip(new Yaml(), getExpected());
    }

    private String getExpected() {
        String expected = "TypeBean:\n" + //
                "\tindex: 2\n" + //
                "\tname: \"test2\"\n" + //
                "\tvalue: 2.2\n" + //
                "\tconnections: \n" + //
                "\t\t- TypeBean:\n" + //
                "\t\t\tindex: 1\n" + //
                "\t\t\tname: \"test1\"\n" + //
                "\t\t\tvalue: 1.1\n" + //
                "\t\t\tbyteStream: [49, 50, 51]\n" + //
                "\t\t\tintStream: [1, 2, 3]\n" + //
                "\t\t- TypeBean: \"@0\"";
        return expected;
    }

}
