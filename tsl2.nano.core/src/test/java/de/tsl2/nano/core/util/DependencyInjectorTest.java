package de.tsl2.nano.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.DependencyInjector.Producer;
import de.tsl2.nano.core.util.DependencyInjectorTest.Inject;
import de.tsl2.nano.core.util.DependencyInjectorTest.TestInterface;

public class DependencyInjectorTest {

    @Before
    public void setUp() {
        LogFactory.setLogLevel(LogFactory.DEBUG);
    }

    @Test
    public void testSimpleInjection() {
        DependencyInjector di = new DependencyInjector();
        TestBean testBean = di.provideInstance(TestBean.class);
        assertTrue(testBean.testInterface != null);
        assertTrue(testBean.stdInjectableBean != null);
        assertTrue(testBean.tstInjectabletypeBean == null);
    }

    @Test
    public void testInjectionWithProvidedInstances() {
        String string = "example";
        // TestInterface testInterface = AdapterProxy.create(TestInterface.class, MapUtil.asMap("isTested", true));
        List<String> list = Arrays.asList(string);
        DependencyInjector di = new DependencyInjector(Arrays.asList(Inject.class),
                Arrays.asList(DependencyInjector.Producer.class),
                null,
                string, /*testInterface, */list);
        TestBean testBean = di.getInstance(TestBean.class);
        assertTrue(testBean.testInterface != null);
        assertTrue(testBean.tstInjectabletypeBean != null);
        assertTrue(testBean.stdInjectableBean == null);
        assertTrue(testBean.testInterface.isTested());
        assertEquals("example", testBean.name);
        assertEquals(list.get(0), testBean.list.get(0));
    }

    @Producer
    static TestInterface createTestInterfaceImpl() {
        return AdapterProxy.create(TestInterface.class, MapUtil.asMap("isTested", true));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    @interface Inject {

    }

    public interface TestInterface {
        boolean isTested();
    }
}

class TestBean {
    @Inject
    TypeBean tstInjectabletypeBean;

    @DependencyInjector.Inject
    @Inject
    TestInterface testInterface;

    @DependencyInjector.Inject
    TypeBean stdInjectableBean;

    @Inject
    String name;

    @Inject
    List<Object> list;
}

class TypeBean {
    TypeBean typeBean;
    String string;
    Integer integer;
    Double double1;
    Date date;
    int int1;
    char char1;

    Collection collection;
    Map map;

    public TypeBean getTypeBean() {
        return typeBean;
    }

    public void setTypeBean(TypeBean typeBean) {
        this.typeBean = typeBean;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public Integer getInteger() {
        return integer;
    }

    public void setInteger(Integer integer) {
        this.integer = integer;
    }

    public Double getDouble1() {
        return double1;
    }

    public void setDouble1(Double double1) {
        this.double1 = double1;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getInt1() {
        return int1;
    }

    public void setInt1(int int1) {
        this.int1 = int1;
    }

    public char getChar1() {
        return char1;
    }

    public void setChar1(char char1) {
        this.char1 = char1;
    }

    public Collection getCollection() {
        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }
}
