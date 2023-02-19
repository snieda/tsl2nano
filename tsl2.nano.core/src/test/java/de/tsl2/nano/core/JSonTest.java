package de.tsl2.nano.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Ignore;
import org.junit.Test;

import de.tsl2.nano.core.util.JSon;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.ValueHolder;

public class JSonTest {

    @Test
    public void testIsJSon() {
        String txt = "{\"id\": \"1,00\",\"name\": \"Einzug Max.Mustermann\",\"bic\": \"BICBANKXXX\",\"iban\": \"BICBANKXXX0123456789\"}, {\"id\": \"1\"}]";
        assertTrue(JSon.isJSon(txt));

        assertFalse(JSon.isJSon("{seppl=0}"));
        assertFalse(JSon.isJSon("[{seppl=0, depp is true"));
        assertFalse(JSon.isJSon("beanName is not a known entity!"));
    }

	@Test
	public void testJSonMap() {
		Map m = MapUtil.asMap("k1", "v1,v2", "k2", "v2;v3");

		String json = JSon.toJSon(m);
		assertTrue(JSon.isJSon(json));
		Map m2 = JSon.fromJSon(json);
		assertEquals(json, JSon.toJSon(m2));
		assertEquals(MapUtil.asArray(m), MapUtil.asArray(m2));
	}

	@Test
	public void testJSONRecursive() {
		System.setProperty("tsl2.json.recursive", "true");
		ValueHolder v1 = new ValueHolder(null);
		ValueHolder v2 = new ValueHolder(v1);
		v1.setValue(v2);
		String result = Util.toJson(v2);
		System.out.println(result);
		assertEquals("{\"value\": \"{\"value\": \"@0\"}{\"value\": \"{\"value\": \"@0\"}\"}", result);
	}

    @Test
    public void testJsonObject() {
        System.setProperty("tsl2.json.recursive", "true");
        TypeBean t = new TypeBean("test", 1, null/*Arrays.asList(new TypeBean("sub", 2, null))*/);
        TypeBean c = JSon.toObject(TypeBean.class,JSon.toJSon(t));
        assertEquals(t, c);
    }

    @Test
    @Ignore("not ready yet...")
    public void testJsonList() {
        List<TypeBean> list = Arrays.asList(new TypeBean("test", 1.0, null), new TypeBean("sub", 2.0, null));
        List<TypeBean> list2 = JSon.toList(TypeBean.class, JSon.toJSon(list));
        assertEquals(list, list2);
    }
}
class TypeBean {
    String name;
    double value;
    Collection<TypeBean> connections;
    
    public TypeBean() {
    }
    public TypeBean(String name, double value, Collection<TypeBean> connections) {
        this.name = name;
        this.value = value;
        this.connections = connections;
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
                && (connections == null && other.connections == null) || connections == null || connections.containsAll(other.connections);
    }
    @Override
    public String toString() {
        return "TypeBean [name=" + name + ", value=" + value + ", connections=" + connections + "]";
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
}