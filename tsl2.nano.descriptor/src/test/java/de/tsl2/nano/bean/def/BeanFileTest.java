package de.tsl2.nano.bean.def;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.autotest.TypeBean;
import de.tsl2.nano.autotest.WeekdayEnum;
import de.tsl2.nano.bean.BeanFileUtil;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;

public class BeanFileTest implements ENVTestPreparation {
	@Before
	public void setUp() {
		ENVTestPreparation.super.setUp("descriptor");
	}

	@AfterClass
	public static void tearDown() {
		ENVTestPreparation.tearDown();
	}

    @Test
    public void testBeanUtilFromFlatFile() throws Exception {
        /*
         * test it , using a separation string
         */
        //first, create an example file and test bean
        String testFile = ENV.getConfigPath() + "commontest-fromflatfile.txt";
        String s = "0123 456789 Monday\n1234 567890 Tuesday";
        FileUtil.writeBytes(s.getBytes(), testFile, false);

        //read it with fixed-columns
        Collection<TypeBean> result = BeanFileUtil.fromFlatFile(testFile,
            " ",
            TypeBean.class,
            "primitiveShort",
            "primitiveLong",
            "weekdayEnum");
        assertTrue(result.size() == 2);

        Iterator<TypeBean> resultIt = result.iterator();
        TypeBean typeLine1 = new TypeBean();
        typeLine1.setPrimitiveShort((short) 123);
        typeLine1.setPrimitiveLong(456789l);
        typeLine1.setWeekdayEnum(WeekdayEnum.Monday);
        assertTrue(BeanUtil.equals(resultIt.next(), typeLine1));

        TypeBean typeLine2 = new TypeBean();
        typeLine2.setPrimitiveShort((short) 1234);
        typeLine2.setPrimitiveLong(567890l);
        typeLine2.setWeekdayEnum(WeekdayEnum.Tuesday);
        assertTrue(BeanUtil.equals(resultIt.next(), typeLine2));

        /*
         * test it , using another separation string and an empty line at the end
         */
        //first, create an example file and test bean
        testFile = ENV.getConfigPath() + "commontest-fromflatfile.txt";
        s = "\"0123\",\"456789\",\"Monday\"\n\"1234\",\"567890\",\"Tuesday\"\n";
        FileUtil.writeBytes(s.getBytes(), testFile, false);

        //first, create an example file and test bean
        testFile = ENV.getConfigPath() + "commontest-fromflatfile.txt";
        s = "header\n\"0123\",\"456,789\",\"Monday\"\n\"1234\",\"567890\",\"Tuesday\"\n";
        FileUtil.writeBytes(s.getBytes(), testFile, false);

        //this reader will eliminate the " quotations!
        Reader reader = FileUtil.getTransformingReader(FileUtil.getFile(testFile), ' ', ' ', true);

        //read it with fixed-columns
        result = BeanFileUtil.fromFlatFile(reader,
            "\",\"",
            TypeBean.class,
            null,
            "primitiveShort",
            "string",
            "weekdayEnum");
        assertTrue(result.size() == 2);

        resultIt = result.iterator();
        typeLine1 = new TypeBean();
        typeLine1.setPrimitiveShort((short) 123);
        typeLine1.setString("456,789");
        typeLine1.setWeekdayEnum(WeekdayEnum.Monday);
        assertTrue(BeanUtil.equals(resultIt.next(), typeLine1));

        typeLine2 = new TypeBean();
        typeLine2.setPrimitiveShort((short) 1234);
        typeLine2.setString("567890");
        typeLine2.setWeekdayEnum(WeekdayEnum.Tuesday);
        assertTrue(BeanUtil.equals(resultIt.next(), typeLine2));

        BeanUtil.resetValues(typeLine2);
        assertTrue(typeLine2.primitiveShort == 0 && typeLine2.primitiveLong == 0 && typeLine2.weekdayEnum == null);

        //delete the test file, don't check
        new File(testFile).delete();

        /*
         * test it , using fixed column positions
         */
        //first, create an example file and test bean
        s = "0123456789\n1234567890";
        FileUtil.writeBytes(s.getBytes(), testFile, false);

        //read it with fixed-columns
        result = BeanFileUtil.fromFlatFile(testFile, TypeBean.class, "1-5:primitiveShort", "5-11:primitiveLong");
        assertTrue(result.size() == 2);

        resultIt = result.iterator();
        typeLine1 = new TypeBean();
        typeLine1.setPrimitiveShort((short) 123);
        typeLine1.setPrimitiveLong(456789l);
        assertTrue(BeanUtil.equals(resultIt.next(), typeLine1));

        typeLine2 = new TypeBean();
        typeLine2.setPrimitiveShort((short) 1234);
        typeLine2.setPrimitiveLong(567890l);
        assertTrue(BeanUtil.equals(resultIt.next(), typeLine2));

        //delete the test file, don't check
        new File(testFile).delete();

    }

    @Test
    public void testBeanUtils() throws Exception {
        /*
         * toValueMap
         */
        TypeBean tbm = new TypeBean();
        Object[] args = new Object[] { "string", "test", "primitiveInt", 2 };
        Bean.getBean(tbm, args);

        Map asMap = MapUtil.asMap(args);
        assertTrue(BeanUtil.toValueMap(tbm, false, false, true, "string", "primitiveInt").values()
            .containsAll(asMap.values()));
        assertFalse(BeanUtil.toValueMap(tbm,
            "",
            false,
            false,
            "primitiveBoolean",
            "primitiveByte",
            "primitiveChar",
            "primitiveShort",
            "primitiveLong",
            "primitiveFloat",
            "primitiveDouble",
            "immutableBoolean",
            "immutableByte",
            "immutableChar",
            "immutableShort",
            "immutableInteger",
            "immutableLong",
            "immutableFloat",
            "immutableDouble",
            "bigDecimal",
            "date",
            "time",
            "timestamp",
            "object",
            "collection",
            "arrayObject",
            "arrayPrimitive",
            "type",
            "weekdayEnum",
            "map",
            "relation",
            "binary").values().retainAll(asMap.values()));

        /*
          * the value-binding and value-matching
          */
        ValueHolder<String> vh = new ValueHolder<String>("j");
        ValueMatcher vm = new ValueMatcher(vh, "[jJYy1xX]", "J", "N");
        assertTrue(vm.getValue());
        vh.setValue("n");
        assertTrue(!vm.getValue());
        vm.setValue(false);
        assertTrue(vh.getValue().equals("N"));

        final TypeBean bean = new TypeBean();
        BeanAttribute attr = BeanAttribute.getBeanAttribute(TypeBean.class, "string");
        attr.setValue(bean, "hello");
        if (!"hello".equals(bean.getString())) {
            fail("setting bean value failed!");
        }

        final TypeBean bean2 = BeanUtil.copy(bean);
        if (!BeanUtil.equals(bean2, bean)) {
            fail("bean2 should equal bean");
        }

        bean2.setImmutableInteger(98234);
        if (BeanUtil.equals(bean2, bean)) {
            fail("bean2 should not equal bean");
        }

        if ((attr.getAnnotation(Deprecated.class) instanceof Deprecated)) {
            fail("shouldn't find an annotation!");
        }
        attr = BeanAttribute.getBeanAttribute(TypeBean.class, "arrayObject");
        if (!(attr.getAnnotation(Deprecated.class) instanceof Deprecated)) {
            fail("didn't find the right annotation!");
        }
        attr = BeanAttribute.getBeanAttribute(TypeBean.class, "arrayPrimitive");
        if (!(attr.getAnnotation(Deprecated.class) instanceof Deprecated)) {
            fail("didn't find the right annotation!");
        }

        final BeanClass bclass = BeanClass.getBeanClass(TypeBean.class);
        if (bclass.findAttributes(Deprecated.class).size() != 2) {
            fail("didn't find the right annotations!");
        }

        //test the export functions - but con't check that automatically!
        BeanCollector<Collection<TypeBean>, TypeBean> collector =
            BeanCollector.getBeanCollector(Arrays.asList(bean, bean2), 0);
        System.out.println(BeanFileUtil.presentAsCSV(collector));
        System.out.println(BeanFileUtil.presentAsHtmlTable(collector));
    }
    @Test
    public void testBeanFileUtil() {
    	TypeBean typeBean1 = new TypeBean();
    	typeBean1.string = "meintest";
    	typeBean1.immutableInteger = 99;
    	TypeBean typeBean2 = new TypeBean();
    	typeBean2.string = "meintest2";
    	typeBean2.immutableInteger = 100;
    	String filename = "testflatfile";
		BeanFileUtil.toFile(Arrays.asList(typeBean1, typeBean2), filename, BeanFileUtil.FileType.TABSHEET);
    	
    	Collection<TypeBean> typeBeans = BeanFileUtil.fromFile(filename, BeanFileUtil.FileType.TABSHEET, TypeBean.class);
    	assertEquals(2, typeBeans.size());
    	Iterator<TypeBean> it = typeBeans.iterator();
		assertEquals(typeBean1, it.next());
    	assertEquals(typeBean2, it.next());
    	
    	FileUtil.delete(filename);
    }
}
