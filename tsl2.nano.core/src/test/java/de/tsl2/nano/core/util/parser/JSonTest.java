package de.tsl2.nano.core.util.parser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IValueAccess;
import de.tsl2.nano.core.util.AdapterProxy;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.ValueHolder;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class JSonTest {

    @Test
    public void testIsJSon() {
        JSon jSon = new JSon();
        assertTrue(jSon.isParseable("{}"));
        assertTrue(jSon.isParseable("[]"));
        assertTrue(jSon.isParseable("[0,1]"));
        assertTrue(jSon.isParseable("{\"a\": 0}"));
        // assertTrue(JSon.isJSon("[\"v1\", 1]"));

        String txt = "{\"id\": \"1,00\",\"name\": \"Einzug Max.Mustermann\",\"bic\": \"BICBANKXXX\",\"iban\": \"BICBANKXXX0123456789\"}, {\"id\": \"1\"}]";
        assertTrue(jSon.isParseable(txt));

        txt = "{\"name\": \"test\", \"value\": 72.68648043493326, \"active\": true, \"stream\": [0,1,2,3,4,5,6,7,8,9]}";
        assertTrue(jSon.isParseable(txt));

        assertFalse(jSon.isParseable(""));
        assertFalse(jSon.isParseable("{seppl=0}"));
        assertFalse(jSon.isParseable("[{seppl=0, depp is true"));
        assertFalse(jSon.isParseable("[{seppl=0}, depp is true]"));
        assertFalse(jSon.isParseable("beanName is not a known entity!"));
        assertFalse(jSon.isParseable("{interface de.tsl2.nano.bean.IBeanContainer=de.tsl2.nano.bean.BeanContainer@7c70315, class de.tsl2.nano.bean.def.BeanPresentationHelper=Html5Presentation(null)}"));
        
        String css = "{   display: table-row-group; }thead, .thead {   display: table-header-group;   background-color: var(--table-bg-color);   font-weight: bold; };";
        assertFalse(jSon.isParseable(css));
        assertTrue(JSon.NO_JSON_PATTERN.matcher(css).find());
    }

    @Test
    public void testJSonMap() {
        Map m = MapUtil.asMap("k1", "v1,v2", "k2", "v2;v3");

        String json = new JSon().serialize(m);
        assertTrue(new JSon().isParseable(json));
        Map m2 = (Map) new JSon().toStructure(json);
        assertEquals(json, new JSon().serialize(m2));
        assertEquals(MapUtil.asArray(m), MapUtil.asArray(m2));
    }

    @Test
    public void testJSonMapDeep() {
        ValueHolder value1 = new ValueHolder(Arrays.asList("v4", "v5"));
        ValueHolder value2 = new ValueHolder(value1);
        List<String> list1 = Arrays.asList("v1", "v2");
        Map m = MapUtil.asMap("k1", list1, "k2", "(\"v2\";v3)", "k3", value1, "k4", value2);

        String json = new JSon().serialize(m);
        assertTrue(new JSon().isParseable(json));
        Map m2 = (Map) new JSon().toStructure(json);

        toValueHolder(m2, "k3");
        toValueHolder(m2, "k4");
        m2.put("k3", value1); // workaround: as the deserializer cannot instantiate the valueholder without type informations
        m2.put("k4", value2); // workaround: as the deserializer cannot instantiate the valueholder without type informations
        assertEquals(json, new JSon().serialize(m2));
        // TODO: check, why assertEquals() not is working
        // assertEquals(MapUtil.asArray(m), MapUtil.asArray(m2));
    }

    private void toValueHolder(Map m2, String key) {
        BeanClass<ValueHolder> bcValueHolder = BeanClass.getBeanClass(ValueHolder.class);
        m2.put(key, bcValueHolder.map().fromValueMap((Map<String, Object>) m2.get(key)));
    }

    @Test
    public void testJSONReference() {
        String json = "{\"cause\": \"@0\",\"detailMessage\": 63.7721618630485}";
        new JSon().toObject(Exception.class, json);
    }

    @Test
    public void testJSONRecursion() {
        ValueHolder v1 = new ValueHolder(null);
        ValueHolder v2 = new ValueHolder(v1);
        v1.setValue(v2);
        String result = Util.toJson(v2);
        System.out.println(result);
        assertEquals(
                "{\"type\": {\"name\": \"de.tsl2.nano.core.util.ValueHolder\"},\"value\": {\"type\": {\"name\": \"de.tsl2.nano.core.util.ValueHolder\"},\"value\": \"@0\"}}",
                        result);
    }

    @Test
    public void testJSONProxyRecursion() {
        IValueAccess proxy1 = AdapterProxy.create(IValueAccess.class);
        IValueAccess proxy2 = AdapterProxy.create(IValueAccess.class, Map.of("value", proxy1));
        proxy1.setValue(proxy2);

        String result = Util.toJson(proxy2);
        System.out.println(result);
        assertEquals("{\"value\": {\"value\": \"@0\"}}", result);
    }

    @Test
    public void testJsonObject() {
        TypeBean t = new TypeBean(1, "test", 1.1, null/*Arrays.asList(new TypeBean("sub", 2, null))*/);
        TypeBean c = new JSon().toObject(TypeBean.class, new JSon().serialize(t));
        assertEquals(t, c);
    }

    @Test
    public void testJsonValues() {
        TypeBean t = new TypeBean(1, "test", 1.1, Arrays.asList(new TypeBean(2, "sub", 2.2, null)));
        String serializedJson = new JSon().serialize(t);
        assertTrue(JSon.isJSon(serializedJson));
        TypeBean c = new JSon().toObject(TypeBean.class, serializedJson);
        assertEquals(t, c);
    }

    @Test
    public void testListSplitting() {
        String json = "[{de.tsl2.nano.autotest.TypeBean},1,\"test\",[\"30,14111020445995\",\"30,14111020445995\"]]";
        String[] expectedSplit = new String[] { "{de.tsl2.nano.autotest.TypeBean}", "1", "test",
                "[\"30,14111020445995\",\"30,14111020445995\"]" };
        assertArrayEquals(expectedSplit, new JSon().splitArray(json));
    }

    @Test
    public void testJSonSimpleList() {
        List<TypeBean> list = Arrays.asList(new TypeBean(1, "test", 1.1, null), new TypeBean(1, "sub", 2.2, null));
        String json = new JSon().serialize(list);
        List mList = (List) new JSon().toStructure(json);
        for (int i = 0; i < mList.size(); i++) {
            mList.set(i, BeanClass.getBeanClass(TypeBean.class).map().fromValueMap((Map) mList.get(i)));
        }
        assertEquals(list, mList);
    }

    @Test
    public void testJsonEmptyList() {
        String jSon = new JSon().serialize(new ArrayList<>(0));
        assertEquals(0, ((List) new JSon().toStructure(jSon)).size());
    }

    @Test
    public void testJsonList() {
        List<TypeBean> list = Arrays.asList(new TypeBean(1, "test", 1.1, null), new TypeBean(2, "sub", 2.2, null));
        List<TypeBean> list2 = new JSon().toList(TypeBean.class, new JSon().serialize(list));
        assertEquals(list, list2);
    }

    @Test
    public void testJsonArray() {
        TypeBean[] arr = new TypeBean[] { new TypeBean(1, "test", 1.1, null), new TypeBean(2, "sub", 2.2, null) };
        assertArrayEquals(arr, (TypeBean[]) new JSon().toArray(TypeBean.class, new JSon().serialize(arr)));
    }

    @Test
    public void testSerialization() {
        StructParserTest.testRoundtrip(new JSon(), null);
    }

}
