package de.tsl2.nano.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import de.tsl2.nano.core.util.Yaml;

public class YamlTest {

    @Test
    public void testIsYaml() {
        assertTrue(new Yaml().isParseable("test: 1"));
    }

    @Test
    public void testSerialization() {
        TypeBean o1 = new TypeBean(1, "test1", 1.1, null);
        TypeBean o2 = new TypeBean(2, "test2", 2.2, Arrays.asList(o1));
        System.out.println(new Yaml().serialize(o2));
        String expected = "TypeBean:\n" + //
                "\tconnections:\n" + //
                "\t\t- TypeBean:\n" + //
                "\t\t\tindex: 1\n" + //
                "\t\t\tname: \"test1\"\n" + //
                "\t\t\tvalue: 1.1\n" + //
                "\tindex: 2\n" + //
                "\tname: \"test2\"\n" + //
                "\tvalue: 2.2\n" + //
                "";
        assertEquals(expected, new Yaml().serialize(o2));
        TypeBean deserialized = new Yaml().toObject(TypeBean.class, expected);
        assertEquals(expected, new Yaml().serialize(deserialized));
    }

}
