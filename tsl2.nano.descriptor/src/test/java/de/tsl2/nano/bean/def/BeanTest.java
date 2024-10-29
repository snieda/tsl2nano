package de.tsl2.nano.bean.def;

import static de.tsl2.nano.bean.def.IPresentable.UNDEFINED;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.action.IActivable;
import de.tsl2.nano.autotest.TypeBean;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.enhance.BeanEnhancer;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.messaging.ChangeEvent;
import de.tsl2.nano.core.messaging.IListener;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.currency.CurrencyUnit;
import de.tsl2.nano.currency.CurrencyUtil;
import de.tsl2.nano.format.GenericTypeMatcher;
import de.tsl2.nano.format.RegExpFormat;

public class BeanTest implements ENVTestPreparation {
    private static final Log LOG = LogFactory.getLog(BeanTest.class);

    @Before
    public void setUp() {
    	Locale.setDefault(Locale.GERMANY);
    	ENVTestPreparation.super.setUp("descriptor");
    }

    @After
    public void tearDown() {
    	// ENVTestPreparation.tearDown();
    }
    

    @Test
    public void testBeanDefinition() throws Exception {
        /*
         * BeanDefinition e.g. as ListDescriptor
         */
        BeanDefinition<TypeBean> beanType = new BeanDefinition<TypeBean>(TypeBean.class);
        beanType.setAttributeFilter("string", "immutableInteger");
        beanType.addAttribute("additionalColumn", "testValue", null, null, null).setColumnDefinition(UNDEFINED,
            0,
            true,
            50);
        assertEquals(50, beanType.getAttribute("additionalColumn").getColumnDefinition().getWidth());

        /*
         * Bean, BeanValue, Listener and relations
         */
        final TypeBean inst1 = new TypeBean();
        final TypeBean inst2 = new TypeBean();
        inst1.setObject(inst2);

        final Bean b1 = new Bean(inst1);
        final Bean b2 = new Bean(inst2);

        b1.setAttributeFilter("string", "bigDecimal", "object");
        b2.setAttributeFilter("primitiveChar", "immutableInteger");

        b1.setAttrDef("string", 5, false, new RegExpFormat("[A-Z]+", 5), null, null);
        final ArrayList<String> handled = new ArrayList<String>();
        b2.connect("primitiveChar", b1.getAttribute("string"), new CommonAction<Object>() {
            @Override
            public Object action() throws Exception {
                LOG.info("starting connection callback...");
                handled.add("connect");
                return null;
            }
        });

        b1.observe("string", new IListener<ChangeEvent>() {
            @Override
            public void handleEvent(ChangeEvent changeEvent) {
                LOG.info(changeEvent);
                handled.add("observe");
            }
        });

        b1.setValue("string", "TEST");
        b1.check();

        assertTrue(handled.size() > 1 && handled.get(0).equals("connect") && handled.get(1).equals("observe"));

        b1.setValue("string", "test");
        try {
            b1.check();
            fail("check on " + b1.getValue("string") + " must fail!");
        } catch (final IllegalArgumentException ex) {
            //ok
        }
        b1.setValue("string", "xxxxxxxxxxxxxxxxxxxxxxx");
        try {
            b1.check();
            fail("check on " + b1.getValue("string") + " must fail!");
        } catch (final IllegalArgumentException ex) {
            //ok
        }
        b2.setValue("primitiveChar", 'X');
        b2.setValue("immutableInteger", 99);
        LOG.info(b1.getAttribute("object").getRelation("immutableInteger").getValue());

        /*
         * test creating a virual bean (without instance) with an action
         */
        final Bean range = new Bean();
        range.addAttribute("from", new Integer(0), null, null, null);
        range.addAttribute("to", new Integer(1), null, null, null);
        //create a simple adding action
        range.addAction(new CommonAction() {
            @Override
            public Object action() throws Exception {
                return (Integer) range.getValue("from") + (Integer) range.getValue("to");
            }
        });
        assertTrue(range.getAttributes().size() == 2);
        assertArrayEquals(new String[] { "from", "to" }, range.getAttributeNames());
        range.setValue("from", 1);
        range.setValue("to", 2);
        assertTrue((Integer) range.getValue("from") == 1);
        assertTrue((Integer) range.getValue("to") == 2);
        assertArrayEquals(new Object[] { 1, 2 }, range.getValues("from", "to").toArray());
        range.check();
        //check the adding action
        assertTrue((Integer) ((IAction) range.getActions().iterator().next()).activate() == 3);
        //check serialization
        FileUtil.saveXml(range, ENV.getConfigPath() + "test.xml");
    }

    @Test
    public void testBeanSerialization() throws Exception {
        /*
         * de-/serializing BeanDefinitions
         */
        //remove old definitions
        BeanDefinition.getBeanDefinition(TypeBean.class);
        BeanDefinition.deleteDefinitions();

        TypeBean tb = new TypeBean();
        LinkedHashMap map = new LinkedHashMap();
        map.put("key1", "value1");
        map.put("key2", "value2");
        tb.setObject(map);
        FileUtil.saveXml(tb, ENV.getConfigPath() + "testmap.xml");

        BeanClass bc = BeanClass.getBeanClass(TypeBean.class);
        FileUtil.saveXml(bc, ENV.getConfigPath() + "beanclass.xml");

        final BeanDefinition<TypeBean> beandef = BeanDefinition.getBeanDefinition(TypeBean.class);
        beandef.getAttribute("immutableFloat").setBasicDef(10,
            false,
            RegExpFormat.createCurrencyRegExp(),
            null,
            null);
        beandef.addAction(new CommonAction<Object>("mytestaction.id", "mytestaction", "mytestaction") {
            @Override
            public Object action() throws Exception {
                return beandef.toString();
            }
        });
        //check standard serialization
        FileUtil.save(ENV.getConfigPath() + "beandef.ser", beandef);
        FileUtil.load(ENV.getConfigPath() + "beandef.ser");

        beandef.saveDefinition();

        BeanDefinition.clearCache();
        BeanDefinition<TypeBean> beandefFromXml = BeanDefinition.getBeanDefinition(TypeBean.class);
        assertEquals(beandef, beandefFromXml);
        assertEquals(beandef.getClazz(), beandefFromXml.getClazz());
        assertEquals(beandef.getName(), beandefFromXml.getName());
        assertEquals(beandef.toString(), beandefFromXml.toString());
//        assertTrue(BeanUtil.equals(beandef.getPresentable(), beandefFromXml.getPresentable()));
//        assertTrue(BeanUtil.equals(beandef.getAttributes(), beandefFromXml.getAttributes()));
        assertArrayEquals(beandef.getAttributeNames(), beandefFromXml.getAttributeNames());
        assertEquals(beandef.getActions(), beandefFromXml.getActions());

        /*
         * create a full beandefinition
         */
        IPresentable p = beandef.getPresentable();
        p.setStyle(UNDEFINED);
        p.setType(UNDEFINED);
        p.setVisible(true);
        p.setEnabler(IActivable.ACTIVE);
        //TODO: linkedhashmap of layout is not supported by simple-xml
//        p.setLayout(MapUtil.asMap("columns", 2, "rows", 2));
//        p.setLayoutConstraints(MapUtil.asMap("colspan", 2, "rowspan", 2));
        p.setPresentationDetails(IPresentable.COLOR_BLACK, IPresentable.COLOR_GREEN, "favicon.png");

        beandef.setValue(tb, "bigDecimal", new BigDecimal(20.00));
        beandef.getAttribute("bigDecimal").setBasicDef(UNDEFINED, false, RegExpFormat.createCurrencyRegExp(),
            new BigDecimal(1.00), "test-description");
        beandef.setValue(tb, "bigDecimal", new BigDecimal(30.00));

        beandef.saveDefinition();

        /*
         * create a full beancollector
         */
        BeanCollector<Collection<TypeBean>, TypeBean> collector =
            BeanCollector.getBeanCollector(TypeBean.class, null, IBeanCollector.MODE_ALL, null);
        p = collector.getPresentable();
        p.setStyle(UNDEFINED);
        p.setType(UNDEFINED);
        p.setVisible(true);
        p.setEnabler(IActivable.ACTIVE);
//        p.setLayout(MapUtil.asMap("columns", 2, "rows", 2));
//        p.setLayoutConstraints(MapUtil.asMap("colspan", 2, "rowspan", 2));
        p.setPresentationDetails(IPresentable.COLOR_BLACK, IPresentable.COLOR_GREEN, "favicon.png");

        collector.setValue(tb, "bigDecimal", new BigDecimal(20.00));
        collector.getAttribute("bigDecimal").setBasicDef(UNDEFINED, false, RegExpFormat.createCurrencyRegExp(),
            new BigDecimal(1.00), "test-description");
        collector.setValue(tb, "bigDecimal", new BigDecimal(30.00));

        collector.addColumnDefinition("bigDecimal", 0, 0, false, 200);
        collector.getBeanFinder().setMaxResultCount(100);
        collector.saveDefinition();
    }

    @Test
    public void testBeanPresentation() throws Exception {
        BeanDefinition.deleteDefinitions();
        BeanDefinition def = BeanDefinition.getBeanDefinition(TypeBean.class);
        Map<String, String> lMap = MapUtil.asMap("width", "99");
        def.getAttribute("object").getPresentation().setLayout((Serializable) lMap);
        Map<String, String> lcMap = MapUtil.asMap("type", "image/svg+xml");
        def.getAttribute("object").getPresentation().setLayout((Serializable) lcMap);
        def.saveDefinition();

        BeanDefinition.clearCache();

        def = BeanDefinition.getBeanDefinition(TypeBean.class);
        lMap = def.getAttribute("object").getPresentation().getLayout();
        assertTrue("image/svg+xml".equals(lMap.get("type")));
    }

    @Test
    public void testBeanAttributeStress() throws Exception {
        long stress = 1000000;
        //method access and stress test
        final TypeBean typeBean = new TypeBean();
        Profiler.si().stressTest("beanutil", stress, new Runnable() {
            BeanAttribute ba = BeanAttribute.getBeanAttribute(TypeBean.class, "string");

            @Override
            public void run() {
                ba.getValue(typeBean);
            }
        });
        Profiler.si().stressTest("standard", stress, new Runnable() {
            @Override
            public void run() {
                typeBean.getString();
            }
        });

        /*
         * test the performance - using reflections
         */

        //object creation

        Profiler.si().stressTest("beanutil", stress, new Runnable() {
            TypeBean p = null;
            BeanClass bc = BeanClass.getBeanClass(TypeBean.class);

            @Override
            public void run() {
                p = (TypeBean) bc.createInstance();
            }
        });
        Profiler.si().stressTest("standard", stress, new Runnable() {
            TypeBean p = null;

            @Override
            public void run() {
                p = new TypeBean();
            }
        });

    }

//    @Ignore("works only sometimes - perhaps setup/teardown doesn't work")
    @Test
    public void testValueExpression() throws Exception {
        /*
         * first: using the c-style printf format
         */
        ValueExpression<TypeBean> ve = new ValueExpression("printf:%string$10s-%primitiveInt$2d==>%primitiveShort$2d",
            TypeBean.class);
        TypeBean tb = new TypeBean();
//        tb.setString("test");
//        tb.setPrimitiveInt(1);
//        tb.setPrimitiveShort((short)2);
//        assertEquals("test-1==>2", ve.to(tb));
//        
//        tb = ve.from("test-1==>2");
//        assertTrue(tb.getString().equals("test") && tb.getPrimitiveInt() == 1 && tb.getPrimitiveShort() == (short)2);

        /*
         * second: using the MessageFormat style
         */
        ve = new ValueExpression<TypeBean>("{string}-{primitiveInt}==>{primitiveShort}", TypeBean.class);
        tb = new TypeBean();
        tb.setString("test");
        tb.setPrimitiveInt(1);
        tb.setPrimitiveShort((short) 2);
        //TODO: check field-order after serializing/deserializing
        assertEquals("test-1==>2", ve.to(tb));

        tb = ve.from("test-1==>2");
        assertTrue(tb.getString().equals("test") && tb.getPrimitiveInt() == 1 && tb.getPrimitiveShort() == (short) 2);

        /*
         * third: using a collection
         */
//        BeanDefinition.clearCache();
        BeanDefinition<TypeBean> beandef = BeanDefinition.getBeanDefinition(TypeBean.class);
        beandef.setAttributeFilter("string", "primitiveInt", "primitiveShort");
        beandef.setValueExpression(new ValueExpression("{string}-{primitiveInt}==>{primitiveShort}", TypeBean.class));
        CollectionExpressionFormat<TypeBean> cef = new CollectionExpressionFormat<TypeBean>(TypeBean.class);

        TypeBean tb1 = new TypeBean();
        tb1.setString("nix");
        tb1.setPrimitiveInt(9);
        tb1.setPrimitiveShort((short) 9);
        Collection<TypeBean> c = Arrays.asList(tb, tb1);
        assertEquals("test-1==>2; nix-9==>9", cef.format(c));
        assertEquals(2, ((Collection) cef.parseObject("test-1==>2; nix-9==>9")).size());
        
        //test value stream (import/export)
        assertTrue(ByteUtil.equals(tb, ValueStream.read(ByteUtil.getInputStream("test-1==>2".getBytes()), ve).iterator().next()));

        Bean.getBean(tb).setValueExpression(ve);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ValueStream.write(out, Arrays.asList(tb));
        assertEquals("test-1==>2\n", new String(out.toByteArray()));
    }

    @Test
    public void testCurrency() throws Exception {
        final Format f = RegExpFormat.createCurrencyRegExp();
        LOG.info(f);
        final String[] values = { "0",
            "10",
            "10,0",
            "10,00",
            "100",
            "1000",
            "1000,00",
            "1.000,00",
            "1.000",
            "1000000,00",
            "1.000.000,00",
            "100000000", //=100.000.000,00
            "-",
            "-0",
            "-1",
            "-1,0",
            "-1.000,0" };
        for (int i = 0; i < values.length; i++) {
            final Object pv = f.parseObject(values[i]);
            final String fv = f.format(pv);
            LOG.info("Currency-String:" + values[i] + "==> parse:" + pv + " format:" + fv);
        }

//        /*
//         * compare length of input-string and bigdecimal output
//         */
//        String input = values[8];
//        BigDecimal pv = (BigDecimal) f.parseObject(input);
//        if (String.valueOf(pv.doubleValue()).length() > input.length())
//            fail("parsed BigDecimal should not be longer than input string");
    }

    @Test
    public void testCurrenciesHistorical() throws Exception {
        Date d = DateUtil.getDate(1990, 1, 1);
        /*
         * Deutsche Mark ueber ISO 4217 (siehe auch Wikipedia ISO 4217)
         * Es ist uber jdk nicht moeglich, ueber ein datum eine waehrung zu erhalten!
         * Das ICU Projekt unter IBM kann dies - verbraucht jedoch 7MB, benoetigt lange
         * zum Starten kann keine Faktoren berechnen (ALT-->NEU) und ist quasi mit 
         * Kanonen auf Spatzen geschossen.
         */
        // 
        LOG.info(Currency.getInstance("DEM"));

        // Das ICU Projekt unter IBM stellt historische Waehrungen..
//        ULocale loc = ULocale.getDefault();
//        String[] currencyCodes = com.ibm.icu.util.Currency.getAvailableCurrencyCodes(loc, d);
//        LOG.info(StringUtil.toString(currencyCodes, 80));
        // Aber keine Alt-Waehrungs-Umrechnung
//        CurrencyAmount amount = new CurrencyAmount(100, com.ibm.icu.util.Currency.getInstance(currencyCodes[0]));
//        LOG.info(amount.getNumber());

        // Einfache Eigen-Zusatz-Implementierung
        CurrencyUnit oldCurrency = CurrencyUtil.getCurrency(d);
        CurrencyUnit newCurrency = CurrencyUtil.getCurrency();

        Double f = 1.95583d;
        assertEquals(f, oldCurrency.getFactor(), 0);
        assertEquals("DEM", oldCurrency.getCurrencyCode());
        assertEquals("EUR", newCurrency.getCurrencyCode());

        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance();
        df.applyPattern("###,###,###.00 ï¿½");
        LOG.info(df.format(123456789));

        //test the factor and rounding mode
        assertEquals(f, CurrencyUtil.getFactor(d));
        assertEquals(new BigDecimal(51.13d, new MathContext(4)), CurrencyUtil.getActualValue(100f, d));
    }

    /**
     * testEnhancer
     */
    @Test
    @Ignore("JDK17: javaassist does not support inner classes to enhance: frozen class (cannot edit)")
    public void testEnhancer() throws Exception {
        /*
         * enhancing an anonymous class, checking the access to the new field
         */
        TypeBean obj = new TypeBean() {
            int test = 1;

            //avoid the compiler to optimize this code...
            public <T extends TypeBean> T dosomethingWithTest() {
                System.out.println(test);
                return (T) this;
            }
        }.dosomethingWithTest();
//        System.out.println("original test-value: " + BeanAttribute.getBeanAttribute(obj.getClass(), "test").getValue(obj));
        TypeBean bean = BeanEnhancer.enhance(obj, "Test", false);
        System.out.println("enhanced test-value:" + BeanAttribute.getBeanAttribute(bean.getClass(), "test")
            .getValue(bean));
        assertTrue(new Integer(1).equals(BeanAttribute.getBeanAttribute(bean.getClass(), "test").getValue(bean)));

        /*
         * enhancing a normal class, checking the access to the new field
         */
//        obj = new TypeBean() {
//            int test = 1;
//         };
//         bean = BeanEnhancer.enhance(obj, "Test", false);
//         assertTrue(new Integer(1).equals(BeanAttribute.getBeanAttribute(bean.getClass(), "test").getValue(bean)));
//
//         /*
//          * enhance using an interface and proxy
//          */
//         obj = new TypeBean() {
//             int test = 1; 
//          };
//          bean = BeanEnhancer.enhance(obj, "Test", true);
//          assertTrue(new Integer(1).equals(BeanAttribute.getBeanAttribute(bean.getClass(), "test").getValue(bean)));

        /*
         * enhance simple attributes
         */
        Map<String, Object> attributes = new Hashtable<String, Object>();
        attributes.put("test", 1);
        attributes.put("test1", Integer.class);
        Object proxyObject = BeanEnhancer.createObject("Test", true, attributes);
        assertTrue(new Integer(1).equals(BeanAttribute.getBeanAttribute(proxyObject.getClass(), "test")
            .getValue(proxyObject)));
        assertTrue(null == BeanAttribute.getBeanAttribute(proxyObject.getClass(), "test1").getValue(proxyObject));

        /*
         * aop on attributes
         */
        String before = "$0.object = \"invoked\";";
        obj = new TypeBean();
//           BeanEnhancer.enhance(obj, before, null, "object");
        TypeBean aopBean = BeanEnhancer.enhance(obj, before, null, obj.getClass().getMethod("getObject", new Class[0]));
        assertTrue("invoked".equals(aopBean.getObject()));
    }

    @Test
    public void testFiBean() throws Exception {
    	//TODO: the test doesn't work in cause of not putting the bean to the cache - an empty virtual bean will be printed!
//    	System.setProperty("app.mode.strict", "true");
        de.tsl2.nano.bean.fi.Bean fiBean = new de.tsl2.nano.bean.fi.Bean("test");
        de.tsl2.nano.bean.fi.Bean abean =
            fiBean.add("testAttribute", "testAttributeValue").constrain(18, null, false).description(new Presentable());
        System.out.println(abean);
        abean.setValue("testAttribute", "testAttributeValue1");
        assertTrue(!abean.getAttribute("testAttribute").getStatus().ok());
    }

    @Test
    public void testGenericTypeMatcher() throws Exception {
        GenericTypeMatcher typeMatcher = new GenericTypeMatcher();
        Assert.assertEquals(Boolean.TRUE, typeMatcher.materialize("true"));
        Assert.assertEquals(Boolean.FALSE, typeMatcher.materialize("false"));
        Assert.assertEquals(Integer.valueOf(100), typeMatcher.materialize("100"));
        Assert.assertEquals(Long.valueOf(10000000000l), typeMatcher.materialize("10000000000"));
        Assert.assertEquals(BigDecimal.valueOf(100.12), typeMatcher.materialize("100,12"));
        Assert.assertEquals(new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2010"),
            typeMatcher.materialize("01.01.2010"));
        Assert.assertEquals(new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2010").getTime(),
            ((Date) typeMatcher.materialize("01.01.2010")).getTime());
    }

    @Test
    public void testJSON() {
    	System.setProperty("bean.format.date.iso8601", "true");
        TypeBean o = new TypeBean();
        o.setString("test");
        o.setBigDecimal(new BigDecimal("10"));
        o.setDate(new Date());
        o.setImmutableBoolean(true);
        o.setPrimitiveChar('/'); // using a number will fail!
        
        String json = BeanUtil.toJSON(o);
        System.out.println(json);

        TypeBean o1 = BeanUtil.fromJSON(TypeBean.class, json);
        assertEquals(json, BeanUtil.toJSON(o1));
//        assertTrue(ObjectUtil.equals(o, o1)); // date will differ on seconds part
    }
}
