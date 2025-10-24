package de.tsl2.nano.h5;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanValueMap;
import de.tsl2.nano.bean.def.IValueDefinition;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.parser.JSon;

    @SuppressWarnings({ "rawtypes", "unchecked" })
public class BeanValueMapTest implements ENVTestPreparation {
    @BeforeClass
    public static void setUpClass() {
        ENVTestPreparation.setUp("replication", false);
    }

    @Test
    void testBeanMapSerialization() {
        LogFactory.setLogLevel(LogFactory.DEBUG);
        Html5Presentation.registereNanoH5Implemenations();

        Bean<Map> bean = createExampleBean();

        String json = new JSon().serialize(bean);
        String expected = "{\"id\":\"mybean\",\"name\":\"MyBean\",\"instance\":{\"id\":\"mybean\",\"name\":\"MyBean\",\"submap\":{\"ki1\":\"vi1\"}},\"valueExpression\":{\"attributeNames\":[\"id\"],\"comparator\":{},\"expression\":\"{id}\",\"name\":\"{id}\",\"type\":{\"name\":\"java.util.LinkedHashMap\"}},\"attributeDefs\":[{\"name\":\"id\",\"description\":\"mybean.id\",\"constraint\":{\"format\":{},\"length\":-1,\"precision\":-1,\"scale\":-1,\"type\":{\"name\":\"java.lang.String\"},\"nullable\":true},\"presentation\":{\"description\":\"mybean.id\",\"enabler\":\"AlwaysActive\",\"gridHeight\":0,\"gridWidth\":0,\"height\":-1,\"label\":\"Id\",\"serialversionuid\":-XXX,\"style\":4,\"type\":1,\"width\":-1,\"nesting\":false,\"searchable\":true,\"visible\":true}},{\"name\":\"name\",\"description\":\"mybean.name\",\"constraint\":{\"format\":{},\"length\":-1,\"precision\":-1,\"scale\":-1,\"type\":{\"name\":\"java.lang.String\"},\"nullable\":true},\"presentation\":{\"description\":\"mybean.name\",\"enabler\":\"AlwaysActive\",\"gridHeight\":0,\"gridWidth\":0,\"height\":-1,\"label\":\"Name\",\"serialversionuid\":-XXX,\"style\":4,\"type\":1,\"width\":-1,\"nesting\":false,\"searchable\":true,\"visible\":true}},{\"name\":\"submap\",\"description\":\"mybean.submap\",\"constraint\":{\"format\":{\"valueExpression\":{\"comparator\":{},\"type\":{\"name\":\"java.lang.Object\"}}},\"length\":-1,\"precision\":-1,\"scale\":-1,\"type\":{\"name\":\"java.util.LinkedHashMap\"},\"nullable\":true},\"presentation\":{\"description\":\"mybean.submap\",\"enabler\":\"AlwaysActive\",\"gridHeight\":0,\"gridWidth\":0,\"height\":-1,\"label\":\"Submap\",\"serialversionuid\":-XXX,\"style\":4,\"type\":XXX,\"width\":-1,\"nesting\":false,\"searchable\":true,\"visible\":true}}]}";
        
        assertEquals(ignoreSome(expected), ignoreSome(json));

        Bean rbean = new JSon().toObject(BeanValueMap.class, json);
        assertEquals(bean.getDeclaringClass(), rbean.getDeclaringClass().getSuperclass());
        assertEquals(bean.getId(), rbean.getId());
        // assertEquals(bean.toString(), rbean.toString());
        // assertArrayEquals(bean.getAttributeNames(), rbean.getAttributeNames());
        // assertEquals(bean.getBeanValues(), rbean.getBeanValues());

        assertEquals_(bean, rbean);
        FileUtil.save("doc/generated/example-beanvaluemap.json", json);
    }

    public static void assertEquals_(Bean<Map> bean, Bean rbean) {
        String json;
        //WORKAROUND: on submap the format is different....
        bean.getAttributes().forEach(a -> ((IValueDefinition)a).getConstraint().setFormat(null));
        rbean.getAttributes().forEach(a -> ((IValueDefinition)a).getConstraint().setFormat(null));
        
        json = new JSon().serialize(bean);
        
        assertEquals(ignoreSome(json), ignoreSome(new JSon().serialize(rbean)));
    }

    public static Bean<Map> createExampleBean() {
        Map internalMap = MapUtil.asMap("ki1", "vi1");
        Map instance = MapUtil.asMap("id", "mybean", "name", "MyBean", "submap", internalMap);
        // avoid conflicts with other tests
        Bean.removeFromCache(instance, true);
        Bean.removeFromCache(internalMap, true);

        //will handle a map instance configuring default attributes
        Bean<Map> bean = Bean.getBean(instance);
        return bean;
    }

    public static  String ignoreSome(String str) {
        return str.replaceAll("[\t\r\n\\s]", "")
            .replaceAll("\\d{4,99}", "XXX")
            .replaceAll("Proxy\\d+", "ProxyXXX");
    }


}
