package de.tsl2.nano.core.util.parser;

import static org.junit.Assert.assertEquals;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class StructParserTest {
    static <S extends StructParser.ASerializer> void testRoundtrip(S s, String expected) {
        testRoundtrip(s, expected, true);
    }

    static <S extends StructParser.ASerializer> void testRoundtrip(S s, String expected, boolean checkDeserializing) {
        TypeBean o1 = new TypeBean(1, "test1", 1.1, new LinkedList<>(), new int[] { 1, 2, 3 }, "123".getBytes());
        TypeBean o2 = new TypeBean(2, "test2", 2.2, new LinkedList(Arrays.asList(o1)), null, null);
        o2.connections.add(o2); // -> recursion

        if (expected != null)
            assertEquals(expected, s.serialize(o2));
        else
            expected = s.serialize(o2);
        System.out.println("Serialized:\n" + expected);

        // final String finalExpected = expected; // on printing to console, the self referencing of maps will result in a stackoverflow
        // System.out.println("Structure:\n" + Util.trY(() -> s.toStructure(finalExpected), false));

        if (checkDeserializing) {
            TypeBean deserialized = s.toObject(TypeBean.class, expected);
            assertEquals(expected, s.serialize(deserialized));
        }
    }

}

@SerialClass(attributeOrder = { "index", "name", "value", "byteStream", "intStream", "connections", "fieldToIgnore",
        "namedBean" })
class TypeBean {
    byte[] byteStream;
    int[] intStream;
    Collection<TypeBean> connections;
    double value;
    String name;
    int index;

    @Serial(ignore = true)
    String fieldToIgnore;

    @Serial(name = "annotated", type = String.class, formatter = TypeBeanNameFormat.class)
    TypeBean namedBean;

    public TypeBean() {
    }

    public TypeBean(int index, String name, double value, Collection<TypeBean> connections) {
        this(index, name, value, connections, null, null);
    }

    public TypeBean(int index, String name, double value, Collection<TypeBean> connections, int[] intStream,
            byte[] byteStream) {
        this.index = index;
        this.name = name;
        this.value = value;
        this.connections = connections;
        this.intStream = intStream;
        this.byteStream = byteStream;
        fieldToIgnore = "ignoreMe!";
        namedBean = this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, connections);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TypeBean other = (TypeBean) obj;
        return Objects.equals(name, other.name)
                && Double.doubleToLongBits(value) == Double.doubleToLongBits(other.value)
                && (connections == null && other.connections == null)
                || connections != null && other.connections != null && connections.containsAll(other.connections);
    }

    @Override
    public String toString() {
        return "TypeBean [index=" + index + ", name=" + name + ", value=" + value + ", connections=" + connections
                + "]";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Collection<TypeBean> getConnections() {
        return connections;
    }

    public void setConnections(Collection<TypeBean> connections) {
        this.connections = connections;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public byte[] getByteStream() {
        return byteStream;
    }

    public void setByteStream(byte[] byteStream) {
        this.byteStream = byteStream;
    }

    public int[] getIntStream() {
        return intStream;
    }

    public void setIntStream(int[] intStream) {
        this.intStream = intStream;
    }
}

class TypeBeanNameFormat extends Format {

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(((TypeBean) obj).name);
        return toAppendTo;
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
        return new TypeBean(0, source, 0, null);
    }

}
