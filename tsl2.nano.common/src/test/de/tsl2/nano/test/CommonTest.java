/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 16, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Policy;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.json.JsonObject;
import javax.json.JsonStructure;

import org.apache.commons.logging.Log;
import org.apache.tools.ant.types.FileSet;
import org.apache.xmlgraphics.util.MimeConstants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.simpleframework.xml.Element;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.collection.ArrSegList;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.FloatArray;
import de.tsl2.nano.core.AppLoader;
import de.tsl2.nano.core.Argumentator;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ITransformer;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.classloader.NestedJarClassLoader;
import de.tsl2.nano.core.classloader.NetworkClassLoader;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.execution.ThreadState;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.AnnotationProxy;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.Crypt;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.DefaultFormat;
import de.tsl2.nano.core.util.EHttpClient;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.PKI;
import de.tsl2.nano.core.util.Period;
import de.tsl2.nano.core.util.SimpleXmlAnnotator;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.TrustedOrganisation;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.YamlUtil;
import de.tsl2.nano.execution.AntRunner;
import de.tsl2.nano.execution.ScriptUtil;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.historize.HistorizedInputFactory;
import de.tsl2.nano.historize.Volatile;
import de.tsl2.nano.util.PrintUtil;
import de.tsl2.nano.util.Translator;
import de.tsl2.nano.util.test.TypeBean;

/**
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes", "serial" })
public class CommonTest {
    private static final Log LOG = LogFactory.getLog(CommonTest.class);
    private static final String BASE_DIR_COMMON = "../tsl2.nano.common/";
    private static final String POSTFIX_TEST = "test/";

    @BeforeClass
    public static void setUp() {
        ENV.create(BASE_DIR_COMMON + POSTFIX_TEST);
        ENV.setProperty(ENV.KEY_CONFIG_PATH, POSTFIX_TEST);
//        Environment.setProperty("app.strict.mode", true);
        ENV.deleteEnvironment();
    }

    @AfterClass
    public static void tearDown() {
        ENV.deleteEnvironment();
    }
    
    @Test
    public void testStringUtil() throws Exception {
        //split and concat
        String str = "diesisteineinfachertext";
        String split = StringUtil.split(str, " ", 4, 7, 10, 19);
        assertEquals("dies ist ein einfacher text", StringUtil.concat(" ".toCharArray(), split));

        //substring, extracts
        assertEquals("ist", StringUtil.substring(str, "dies", "ein"));
        assertEquals("text", StringUtil.substring(str, "einfacher", null));
        assertEquals("dies", StringUtil.substring(str, null, "ist"));
//        assertEquals(str, StringUtil.substring(str, null, null));

        assertEquals("text", StringUtil.extract(str, "[etx]{4,4}"));
        StringBuilder sbstr = new StringBuilder(str);
        assertEquals("text", StringUtil.extract(sbstr, "[etx]{4,4}", "spruch"));
        assertEquals("diesisteineinfacherspruch", sbstr.toString());

        //test hex
        String hex = StringUtil.toHexString(str.getBytes());
        assertEquals(str, StringUtil.fromHexString(hex));
        
        //test crypto
        String[] passwds = new String[] { "meinpass", "12345678", "azAzï¿½ï¿½ï¿½ï¿½" };
        for (int i = 0; i < passwds.length; i++) {
            byte[] cryptoHash = StringUtil.cryptoHash("ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½");
            LOG.info(passwds[i] + " ==> " + "(" + cryptoHash.length + ") " + StringUtil.toString(cryptoHash, 1000));
            LOG.info(passwds[i] + " crypto-hex: " + StringUtil.toHexString(cryptoHash));
            LOG.info(passwds[i] + "        hex: " + StringUtil.toHexString(passwds[i].getBytes()));
            LOG.info(
                passwds[i] + "           : " + StringUtil.fromHexString(StringUtil.toHexString(passwds[i].getBytes())));
        }

        //complex extracting
        String url = "jdbc:mysql://db4free.net:3306/0zeit";
        String port = StringUtil.extract(url, "[:](\\d+)[:/;]\\w+");
        assertEquals("3306", port);

        port = StringUtil.extract(url, "[:](\\d+)([:/;]\\w+)", 1);
        assertEquals("3306", port);

        url = "jdbc:mysql://db4free.net:3306";
        port = StringUtil.extract(url, "[:](\\d+)([:/;]\\w+)?", 1);
        assertEquals("3306", port);
    }

    @Ignore("old ant version 1.65 is on the classpath, result in error")
    @Test
    public void testScriptUtil() throws Exception {
        final Properties p = new Properties();
        //par is not used
        p.put("tsl2nano.test.parameter", "test");
        p.put("client.lib.dir", "lib");
        p.put("shared.lib.dir", "lib");
        p.put("server.lib.dir", "lib");
        p.put("lib-tools.dir", "lib");
        if (!ScriptUtil.ant(BASE_DIR_COMMON + "src/resources/" + "shell.xml", "help", p)) {
            fail("ant call didn't work!");
        }

        //works only on windows
        assertTrue(ScriptUtil.execute("cmd", "/C", "echo", "hello").exitValue() == 0);
        assertTrue(ScriptUtil.execute("cmd", "/C", System.getProperty("user.dir") + "/" + BASE_DIR_COMMON + "runsh.bat").exitValue() == 0);
        //works only on windows
//        ScriptUtil.execute("c:/Program Files (x86)/Adobe/Reader 10.0/Reader/AcroRd32.exe", "c:/eigen/SVN-Eclipse-Einrichtung.pdf");
//        ScriptUtil.executeRegisteredWindowsPrg("c:/eigen/SVN-Eclipse-Einrichtung.pdf");
    }

    @Test
    public void testFileUtil() throws Exception {
        final String fileName = FileUtil.getValidFileName("A<>|;,bstimm-SummeID=${(/\\123)}");
        if (!fileName.equals("A_____bstimm-SummeID______123__")) {
            fail("getValidFileName() didn't work");
        }

        Collection<File> files = FileUtil.getTreeFiles("./", ".*/resources");
        assertTrue(files.size() == 1 && files.iterator().next().getName().equals("resources"));

        files = FileUtil.getFileset(BASE_DIR_COMMON, "**/resources/**/*class.vm");
        assertTrue(files.size() == 1 && files.iterator().next().getName().equals("beanclass.vm"));

    }

    @Test
    public void testNumberUtil() throws Exception {
        Long fixLengthNumber = NumberUtil.fixLengthNumber(10l, 5, '0');
        assertEquals((Long) 10000l, fixLengthNumber);

        assertEquals(true, NumberUtil.isNegative(new BigDecimal(-10)));
        assertEquals(false, NumberUtil.isPositive(new BigDecimal(-10)));
        assertEquals(true, NumberUtil.isPositive(new BigDecimal(10)));
        assertEquals(false, NumberUtil.isNegative(new BigDecimal(10)));
        assertEquals(false, NumberUtil.isPositive(new BigDecimal(0)));
        assertEquals(false, NumberUtil.isNegative(new BigDecimal(0)));
        assertEquals(true, NumberUtil.isNotNegative(new BigDecimal(10)));
        assertEquals(true, NumberUtil.isNotNegative(new BigDecimal(0)));
        assertEquals(false, NumberUtil.isNotNegative(new BigDecimal(-10)));
        assertEquals(false, NumberUtil.isZero(new BigDecimal(1)));
        assertEquals(true, NumberUtil.isZero(new BigDecimal(0)));

        assertEquals(true, NumberUtil.isEmpty(null));
        assertEquals(true, NumberUtil.isEmpty(BigDecimal.ZERO));

        assertEquals(true, NumberUtil.isGreater(2f, 1.9f));
        assertEquals(true, NumberUtil.isGreater(2, 1));
        assertEquals(false, NumberUtil.isGreater(1.9f, 2f));
        assertEquals(false, NumberUtil.isGreater(1, 2));
        assertEquals(false, NumberUtil.isLower(2f, 1.9f));
        assertEquals(false, NumberUtil.isLower(2, 1));
        assertEquals(true, NumberUtil.isLower(1.9f, 2f));
        assertEquals(true, NumberUtil.isLower(1, 2));

        assertEquals(3, NumberUtil.getDelta(new BigDecimal(-1), new BigDecimal(2)), 0f);
        assertEquals(true, NumberUtil.intersects(-1, 3, 2, 4));
        assertEquals(true, NumberUtil.intersects(-1, 3, 3, 4));
        assertEquals(false, NumberUtil.intersects(-1f, 3f, 3.1f, 4f));
        assertEquals(false, NumberUtil.includes(-1, 3, 3, 4));
        assertEquals(false, NumberUtil.includes(-1, 3, 2, 4));
        assertEquals(true, NumberUtil.includes(-1, 3, 1, 2));

        assertEquals(new BigDecimal(1), NumberUtil.extractNumber("1"));
        assertEquals(new BigDecimal(1.1f, new MathContext(2)), NumberUtil.extractNumber("1,1"));
        assertEquals(null, NumberUtil.extractNumber("x,1"));
        assertEquals(null, NumberUtil.extractNumber("1,x"));
        assertEquals(null, NumberUtil.extractNumber("100x"));
        assertEquals(null, NumberUtil.extractNumber("xxx"));
        assertEquals(null, NumberUtil.extractNumber(""));

        //works only in locale de!
        BigDecimal bigDecimal = new BigDecimal(-999.99);
        bigDecimal = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);
        assertEquals(bigDecimal, NumberUtil.getBigDecimal("-999,99"));

        //comparator test
        List<String> textAndNumbers = Arrays.asList("", "-1.000,00", "-1.2", //on german notation it will be -12
            "-1,1",
            "0",
            "1",
            "1,1",
            "2",
            "10",
            "11",
            "1.2", //on german notation it will be 12
            "20",
            "21",
            "22",
            "1.000,00",
            "a",
            "aa",
            "ab",
            "b",
            "bb");
        //do some randomized tests
        for (int i = 0; i < 20; i++) {
            List<String> randomized = new ArrayList<String>(textAndNumbers);
            Collections.shuffle(randomized);
            List<String> sortedList = (List<String>) CollectionUtil.getSortedList(randomized,
                new DefaultFormat(),
                "test");
            LOG.info(StringUtil.toFormattedString(sortedList, 1000, true));
            //check the sorting - there is no util method for that
//            if (!Collections.isEqualCollection(textAndNumbers, sortedList))
            for (int k = 0; k < sortedList.size(); k++) {
                if (!textAndNumbers.get(k).equals(sortedList.get(k))) {
                    fail("sorting of text and numbers failed: " + textAndNumbers.get(k) + " ==> " + sortedList.get(k));
                }
            }
        }

        //Bitfields
        int bitfield = 2 | 4;
        assertEquals(true, NumberUtil.hasBit(bitfield, 2) && NumberUtil.hasBit(bitfield, 4));
        assertEquals(false, NumberUtil.hasBit(bitfield, 1));
        assertEquals(false, NumberUtil.hasBit(bitfield, 8));

        assertEquals(4, NumberUtil.highestOneBit(bitfield));
        assertEquals(8, NumberUtil.bitToDecimal(3));

        assertEquals(0, NumberUtil.filterBits(bitfield, 1, 2, 4));
        assertEquals(4, NumberUtil.filterBits(bitfield, 2));
        assertEquals(2, NumberUtil.filterBits(bitfield, 4));

        assertEquals(4, NumberUtil.filterBitRange(bitfield, 0, 1));
        assertEquals(0, NumberUtil.filterBitRange(bitfield, 1, 2));
        assertEquals(2, NumberUtil.filterBitRange(bitfield, 2, 3));
        assertEquals(6, NumberUtil.filterBitRange(bitfield, 3, 4));

        assertEquals(0, NumberUtil.retainBits(bitfield, 0, 1));
        assertEquals(2, NumberUtil.retainBits(bitfield, 1, 2));
        assertEquals(6, NumberUtil.retainBits(bitfield, 2, 4));
        assertEquals(4, NumberUtil.retainBits(bitfield, 4, 8));

        //simple math functions
        assertEquals(3f, NumberUtil.getDelta(1.5f, 2f, 3f, 4.5f), 0f);
        assertEquals(0f, NumberUtil.getDelta(1.5f, 1.5f), 0f);
        assertEquals(1f, NumberUtil.getDeltaCompare(1.5f, 2f, 3f, 4.5f), 0f);
        assertEquals(0f, NumberUtil.getDeltaCompare(1.5f, 1.5f), 0f);
        assertEquals(new BigDecimal(10.0f), NumberUtil.add(1.5f, 2f, 3.5f, 3f).setScale(0));

//        //duration: 1000 * 1000 seconds
//        List store = new ArrayList<Integer>(1000);
//        for (int i = 0; i < 1000; i++) {
//            int n = NumberUtil.getLocalUniqueInt();
//            if (store.contains(n))
//                fail("Number" + n + " was stored twice");
//            store.add(n);
//            Thread.sleep(1000);
//        }

    }

    @Test
    public void testCollectionUtil() throws Exception {
        //1. converting from arrays to a list
        Object[] arr1 = new Object[] { "Hans Mï¿½ller", "Hans Mueller" };
        String[] arr2 = new String[] { "Carsten1", "Carsten0" };
        String[] arr3 = new String[] { "Berta", "Anton" };
        String[] arr4 = new String[] { "1100", "11", "111", "101", "1" };
        List<String> list = CollectionUtil.asListCombined(arr1, arr2, arr3, arr4);
        assertEquals(arr1.length + arr2.length + arr3.length + arr4.length, list.size());

        //2. sorting the list for numbers and strings
        Collection<?> sortedList = CollectionUtil.getSortedList(list);
        assertEquals(Arrays.asList("1",
            "11",
            "101",
            "111",
            "1100",
            "Anton",
            "Berta",
            "Carsten0",
            "Carsten1",
            "Hans Mueller",
            "Hans Mï¿½ller"), sortedList);

        //3. filtering data
        Collection<String> filteredBetween = CollectionUtil.getFilteringBetween(list, "Anton", "Carsten1");
        assertEquals(Arrays.asList("Anton", "Berta", "Carsten0", "Carsten1"),
            CollectionUtil.getList(filteredBetween.iterator()));

        //do it again
        filteredBetween = CollectionUtil.getFilteringBetween(list, "Anton", "Carsten1");
        assertEquals(Arrays.asList("Anton", "Berta", "Carsten0", "Carsten1"),
            CollectionUtil.getList(filteredBetween.iterator()));

        //4. concatenation
        String[] concat = CollectionUtil.concat(String[].class, arr2, arr3);
        assertArrayEquals(new String[] { "Carsten1", "Carsten0", "Berta", "Anton" }, concat);

        //5. transforming data
        TypeBean tb1 = new TypeBean();
        tb1.setString("Anton");
        TypeBean tb2 = new TypeBean();
        tb2.setString("Berta");
        List<TypeBean> transforming =
            (List<TypeBean>) Util.untyped(CollectionUtil.getTransforming(Arrays.asList(tb1, tb2),
                new ITransformer<TypeBean, String>() {
                    @Override
                    public String transform(TypeBean toTransform) {
                        return toTransform.string;
                    }
                }));
        assertEquals(Arrays.asList("Anton", "Berta"), CollectionUtil.getList(transforming.iterator()));
    }

    @Test
    public void testSimpleDateFormat() throws Exception {
        Format df = RegExpFormat.createDateRegExp();
        String valid[] = { "0",
            "1",
            "2",
            "3",
            "01",
            "10",
            "11",
            "21",
            "30",
            "31",
            "01.",
            "01.0",
            "31.",
            "31.0",
            "31.1",
            "31.01",
            "31.12",
            "31.12.",
            "31.12.1",
            "31.12.2",
            "22.11.20",
            "23.09.200",
            "13.10.2000" };
        Date d;
        for (int i = 0; i < valid.length; i++) {
            d = (Date) df.parseObject(valid[i]);
            LOG.info("parsing '" + valid[i] + "' ==> " + d);
            if (d == null) {
                fail("parsing '" + valid[i] + "' should not fail!");
            }
        }

        //invalids
        String invalid[] = { "4", "9", "32", "01.2", "30.02", "31.02", "31.11", "31.11.", "31.11.2000", "30.13.2000" };
        for (int i = 0; i < invalid.length; i++) {
            try {
                LOG.info("parsing invalid value '" + invalid[i] + "'");
                d = (Date) df.parseObject(invalid[i]);
                if (d != null) {
                    fail("parsing '" + invalid[i] + "' should fail!");
                }
            } catch (ManagedException ex) {
                //ok, parsing should fail
            }
        }
    }

    @Test
    public void testDateAndPeriod() throws Exception {
        long last = System.currentTimeMillis();
        ConcurrentUtil.sleep(1500);
        float sec = DateUtil.seconds(System.currentTimeMillis() - last);
        assertTrue(1.5 <= sec && sec < 2.5);
        
        //print all date time locale formats
        Locale[] availableLocales = Locale.getAvailableLocales();
        Date today = DateUtil.getToday();
        for (int i = 0; i < availableLocales.length; i++) {
            System.out.println(availableLocales[i].getDisplayName() + "("
                + availableLocales[i]
                + "): "
                + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, availableLocales[i])
                    .format(today));
        }
        System.out.println(String.format("Test of String.format(): %1$TD", new Date()));

        //test should date: 01.01.0001
        final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        final SimpleDateFormat dff = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
        final String strZeroDate = "01.01.0001";
        Date shouldDate = df.parse(strZeroDate);
        shouldDate = DateUtil.change(shouldDate, Calendar.HOUR_OF_DAY, 0);
        shouldDate = DateUtil.change(shouldDate, Calendar.HOUR_OF_DAY, 0);
        shouldDate = DateUtil.change(shouldDate, Calendar.MINUTE, 0);
        shouldDate = DateUtil.change(shouldDate, Calendar.SECOND, 0);
        shouldDate = DateUtil.change(shouldDate, Calendar.MILLISECOND, 0);

        final Date zeroDate = DateUtil.getDate(0, 1, 1);
        final String strDate = DateUtil.getFormattedDate(zeroDate);
        if (!strDate.equals(strZeroDate)) {
            fail("date should be " + strZeroDate + " but was " + strDate);
            //TODO: check the failure!!!
//        if (!zeroDate.equals(shouldDate))
//            fail("date should be " + dff.format(shouldDate) + " but was " + dff.format(zeroDate));
        }

        //test date field maximum
        shouldDate = df.parse("31.12.3000");
        shouldDate = DateUtil.setMaximum(shouldDate, Calendar.HOUR_OF_DAY);
        shouldDate = DateUtil.setMaximum(shouldDate, Calendar.MINUTE);
        shouldDate = DateUtil.setMaximum(shouldDate, Calendar.SECOND);
        shouldDate = DateUtil.setMaximum(shouldDate, Calendar.MILLISECOND);

        final Date maxDate = DateUtil.getDate(3000, -1, -1);
        if (!maxDate.equals(shouldDate)) {
            fail("date should be " + dff.format(shouldDate) + " but was " + dff.format(maxDate));
        }

        //TODO: impl.

        //add and diff
//        final long oneMinute = 1000 * 60;
//        DateUtil.addMillis(date, oneMinute);

        //start and end of day

        //concat date and time

        //period
        final Period p = Period.getDaySpan(zeroDate);
        if (p.getDelta() != 1000 * 60 * 60 * 24 - 1) {
            fail("error on day span!");
        }

        //parsing illegal dates

//        Format df1 = FormatUtil.getDefaultFormat(Date.class, true);
//        df1.parseObject("31.12.1999");
//        try {
//            df1.parseObject("32.13.1999");
//            fail("invalid date: 32.13.1999");
//        } catch (Exception ex) {
//            //ok
//            LOG.info(ex);
//        }

        //testing quarters
        Date d = DateUtil.getDate(2002, 1, 1);
        assertEquals(1, DateUtil.getCurrentQuarter(d));
        assertEquals(2, DateUtil.getNextQuarter(d));
        assertEquals(d, DateUtil.getQuarter(2002, DateUtil.Q1));

        d = DateUtil.getQuarter(1970, DateUtil.Q4);
        assertEquals(4, DateUtil.getCurrentQuarter(d));
        assertEquals(1, DateUtil.getNextQuarter(d));

        //test date parts
        Date clearTime = DateUtil.clearTime(null);
        long cutTime = DateUtil.cutTime(System.currentTimeMillis());
        assertEquals(clearTime.getTime(), cutTime - DateUtil.getTimeZoneOffset(cutTime));
    }

    @Test
    public void testBeanClass() throws Exception {
        String cls = "org.company123.my123product.My_ClassName";
        assertTrue(BeanClass.isPublicClassName(cls));
        assertEquals("org.company123.my123product", BeanClass.getPackageName(cls));

        String nocls = "org.company123.my123product.resource";
        assertFalse(BeanClass.isPublicClassName(nocls));

        nocls = "org.company123.my123product.My_ClassName$1";
        assertFalse(BeanClass.isPublicClassName(nocls));

        nocls = "nix";
        assertFalse(BeanClass.isPublicClassName(nocls));
        
        //create array instances
        assertTrue(Arrays.equals(new String[]{"a", "b", "c"}, BeanClass.createInstance(String[].class, "a", "b", "c")));
        //TODO: check, why Arrays.equals() doesn't return true!
        /*assertTrue(Arrays.equals(new Long[3][3], */BeanClass.createInstance(Long[].class, 3, 3);//));
    }

    @Ignore("...running long...")
    @Test
    public void testArrayPerformance() {
        final int c = 2;//1000;
        String description =
            "Test adding and getting one million elements on:\n0. FloatArray\n1. ArrayList\n2. Typed ArrayList\n3. ArrSegList\n4. LinkedList";
        Profiler.si().compareTests(description, 1000, new Runnable() {
            @Override
            public void run() {
                FloatArray numArrayList = new FloatArray();
                for (int i = 0; i < c; i++) {
                    numArrayList.add(i);
                }
                for (int i = 0; i < c; i++) {
                    numArrayList.get(i);
                }
                numArrayList.toArray();
            }
        }, new Runnable() {
            @Override
            public void run() {
                ArrayList numArrayList = new ArrayList();
                for (int i = 0; i < c; i++) {
                    numArrayList.add(i);
                }
                for (int i = 0; i < c; i++) {
                    numArrayList.get(i);
                }
                numArrayList.toArray();
            }

        }, new Runnable() {
            @Override
            public void run() {
                ArrayList<Float> numArrayList = new ArrayList<Float>();
                for (int i = 0; i < c; i++) {
                    numArrayList.add((float) i);
                }
                for (int i = 0; i < c; i++) {
                    numArrayList.get(i);
                }
                numArrayList.toArray(new Float[0]);
            }

        }, new Runnable() {
            @Override
            public void run() {
                ArrSegList<float[], Float> numArrayList = new ArrSegList<float[], Float>(float.class);
                for (int i = 0; i < c; i++) {
                    numArrayList.add((float) i);
                }
                for (int i = 0; i < c; i++) {
                    numArrayList.get(i);
                }
                numArrayList.toSegmentArray();
            }

        }, new Runnable() {
            @Override
            public void run() {
                LinkedList numArrayList = new LinkedList();
                for (int i = 0; i < c; i++) {
                    numArrayList.add(i);
                }
                for (int i = 0; i < c; i++) {
                    numArrayList.get(i);
                }
                numArrayList.toArray();
            }
        });
    }

    /**
     * testJarClassloader
     */
    @Ignore
    @Test
    public void testJarClassloader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        NestedJarClassLoader cl = new NestedJarClassLoader(contextClassLoader, "standalone") {
            @Override
            protected String getRootJarPath() {
                return "../../target/test.h5.sample/tsl2.nano.h5.1.1.0-standalone.jar";
            }
//
//            @Override
//            protected ZipInputStream getJarInputStream(String jarName) {
//                return getExternalJarInputStream(jarName);
//            }
        };

        // filter the 'standalones'
        assertTrue(cl.getNestedJars().length == 26);
    }

    @Ignore("don't do that automatic") 
    @Test
    public void testNetUtilDownload() throws Exception {
        if (NetUtil.isOnline()) {
            Profiler.si().stressTest("downloader", 20, new Runnable() {
                @Override
                public void run() {
                    String url;
                    //https://sourceforge.net/projects/tsl2nano/files/latest/download?source=navbar
                    //http://downloads.sourceforge.net/project/tsl2nano/1.1.0/tsl2.nano.h5.1.1.0.jar
                    File download = NetUtil.download(url =
                        "http://netcologne.dl.sourceforge.net/project/tsl2nano/1.1.0/tsl2.nano.h5.1.1.0.jar",
                        "test/", true, true);
                    NetUtil.check(url, download, 3 * 1024 * 1024);
                }
            });
        }
    }

    @Test
    public void testFileChecksum() throws Exception {
        //use a verified example from internet...
        String test = "sha1 this string";
        String expectedHash = "cf23df2207d99a74fbe169e3eba035e633b65d94";
        
        String file = ENV.getConfigPath() + "testchecksum";
        FileUtil.writeBytes(test.getBytes(), file, false);
        assertTrue(test.equals(String.valueOf(FileUtil.getFileData(file, null))));
        
        FileUtil.checksum(file, "SHA-1", expectedHash);
    }
    
    @Test
    public void testNetUtilProxies() throws Exception {
//        LOG.info("gateway is:" + NetUtil.gateway());

        LOG.info(NetUtil.proxy("http://mvnrepository.com"));
        LOG.info(NetUtil.proxy("http://mvnrepository.com", "proxy.myorg.de:8080"));

        LOG.info(NetUtil.proxy("https://mvnrepository.com"));
        LOG.info(NetUtil.proxy("https://mvnrepository.com", "proxy.myorg.de:8080"));

        LOG.info(NetUtil.proxy("ftp://mvnrepository.com"));
        LOG.info(NetUtil.proxy("ftp://mvnrepository.com", "proxy.myorg.de:8080"));

        LOG.info(NetUtil.proxy("socket://mvnrepository.com"));
        LOG.info(NetUtil.proxy("socket://mvnrepository.com", "proxy.myorg.de:8080"));
    }

    @Test
    public void testNetUtilScan() throws Exception {
        //check for exception only
        NetUtil.isOpen(InetAddress.getByName("localhost"), 666);
        //not a real test - only to see it working!
        NetUtil.scans(0, 10000);
    }

    @Ignore
    @Test
    public void testNetUtilWCopy() throws Exception {
        //not a real test - only to see it working!
        NetUtil.wcopy("http://mobile.chefkoch.de", "test/", null, null);
    }

    @Ignore("problems on loading dependency javax.json on test")
    @Test
    public void testNetUtilJSON() throws Exception {
        if (!NetUtil.isOnline()) {
            LOG.warn("SKIPPING online tests for JSON");
            return;
        }
        JsonStructure structure = NetUtil
            .getRestfulJSON("http://headers.jsontest.com/"/*"https://graph.facebook.com/search?q=java&type=post"*/);
        System.out.println(StringUtil.toString(structure, -1));

        //TODO: in cause of bean dependencies commented
//        Bean<JsonStructure> bean = Bean.getBean(structure);
//        System.out.println(bean);

        String echoService = "http://echo.jsontest.com";
        structure = NetUtil.getRestfulJSON(echoService, "mykey1", "myvalue1", "mykey2", "myvalue2");
        System.out.println(StringUtil.toString(structure, -1));

        JsonObject obj = (JsonObject) structure;
        assertTrue(obj.get("mykey1").toString().equals("\"myvalue1\""));
        assertTrue(obj.get("mykey2").toString().equals("\"myvalue2\""));
    }

    @Test
    public void testCrypt() throws Exception {
        String txt = "test1234";
        String testfile = ENV.getTempPath() + "testfile.txt";
        FileUtil.writeBytes(txt.getBytes(), testfile, false);

        Crypt.main(new String[0]);
        Crypt.main(new String[] { "", "DES", txt });
        Crypt.main(new String[] { "", "AES", txt });
        Crypt.main(new String[] { "", Crypt.ALGO_DES, txt });
        Crypt.main(new String[] { "", Crypt.ALGO_AES, txt });
        Crypt.main(new String[] { "0123456", Crypt.ALGO_PBEWithMD5AndDES, txt });
        Crypt.main(new String[] { "0123456", Crypt.ALGO_PBEWithMD5AndDES, "-file:" + testfile, "-include:\\w+" });
        Crypt.main(new String[] { "0123456", Crypt.ALGO_PBEWithMD5AndDES, "-file:" + testfile, "-base64",
            "-include:\\w+" });
        //not available on standard jdk:
//        Crypt.main(new String[]{"0123456", Crypt.ALGO_PBEWithHmacSHA1AndDESede, txt});
//        Crypt.main(new String[]{"0123456", Crypt.ALGO_PBEWithSHAAndAES, txt});

        //validate certificates, check signification
        // TODO: implement and test!
        KeyPair keyPair = Crypt.generateKeyPair("RSA"/*Crypt.getTransformationPath("RSA", "ECB", "PKCS1Padding")*/);
        try {
            Crypt sender = new Crypt(keyPair.getPublic());
            Crypt receiver = new Crypt(keyPair.getPrivate());
            byte[] sign = sender.sign("test".getBytes(), "SHA-256", 10);
            receiver.validate(null);
            receiver.checkSignification("test".getBytes(), sign, "SHA-256", 10);
        } catch (UnsupportedOperationException ex) {
            //not implemented yet!
        } catch (ManagedException ex) {
            //not implemented yet!
        }
    }

    @Test
    public void testPKI() throws Exception {
        System.out.println(Crypt.providers());
        String data = "test data";
        String passwd = Crypt.generatePassword(8);
        TrustedOrganisation dn = new TrustedOrganisation("me", "de");
        KeyPair keyPair = Crypt.generateKeyPair("RSA");
        PKI pki = new PKI(new Crypt(keyPair.getPublic()), dn);
        
        //creating/loading/persisting keystores
        KeyStore newKeyStore = pki.createKeyStore();
        String file = ENV.getConfigPath() + "mystore";
        pki.peristKeyStore(newKeyStore, file, passwd);
        KeyStore keyStore = pki.createKeyStore(file, passwd.toCharArray());
        assertTrue(newKeyStore.size() == keyStore.size());
        
        //TODO: test certificates
//        CertPath certPath = pki.createCertPath(dn, null, null);
//        pki.write(certPath.getCertificates().get(0), new FileOutputStream(file));
//        Certificate certificate = pki.createCertificate(FileUtil.getFile(file));
//        CertPathValidatorResult valResult = pki.verifyCertPath(certPath, new PKIXBuilderParameters(keyStore, null));
//        assertTrue(((PKIXCertPathValidatorResult)valResult).getPublicKey().equals(certificate.getPublicKey()));

        //validating, signing
        byte[] signature = pki.sign(new ByteArrayInputStream(data.getBytes()), "SHA1withRSA", keyPair.getPrivate());
        assertTrue(pki.verify(new ByteArrayInputStream(data.getBytes()), signature, keyPair.getPublic(), "SHA1withRSA"));
        
    }
    
//    @Test
//    public void testSymEncryption() throws Exception {
//        SymmetricCipher c = new SymmetricCipher();
//        System.out.println(c.getTransformationPath());
//        System.out.println(c.getAlgorithmParameterSpec());
//        
//        String data = "mein wichtiger text";
//        byte[] encrypted = c.encrypt(data.getBytes());
//        byte[] decrypted = c.decrypt(encrypted);
//        System.out.println("Symmetric encryption:\n\t" + data + " --> " + new String(encrypted) + " --> " + new String(decrypted));
//        assertTrue(data.equals(new String(decrypted)));
//    }
//    
//    @Test
//    public void testAsymEncryption() throws Exception {
//        AsymmetricCipher c = new AsymmetricCipher();
//        System.out.println(c.getTransformationPath());
//        System.out.println(c.getAlgorithmParameterSpec());
//        
//        String data = "mein wichtiger text";
//        byte[] encrypted = c.encrypt(data.getBytes());
//        byte[] decrypted = c.decrypt(encrypted);
//        System.out.println("Asymmetric encryption:\n\t" + data + " --> " + new String(encrypted) + " --> " + new String(decrypted));
//        assertTrue(data.equals(new String(decrypted)));
//    }
//    
    @Test
    public void testHistorize() throws Exception {
        HistorizedInputFactory.setPath(ENV.getConfigPath());
        HistorizedInputFactory.create("test", 5, String.class);

        HistorizedInputFactory.instance("test").addAndSave("1");
        HistorizedInputFactory.instance("test").addAndSave("2");
        HistorizedInputFactory.instance("test").addAndSave("3");
        HistorizedInputFactory.instance("test").addAndSave("4");
        HistorizedInputFactory.instance("test").addAndSave("5");

        assertTrue(HistorizedInputFactory.instance("test").containsValue("1"));

        //only 5 items are allowed, so the first one was deleted!
        HistorizedInputFactory.instance("test").addAndSave("6");
        assertTrue(HistorizedInputFactory.instance("test").containsValue("6"));
        assertTrue(!HistorizedInputFactory.instance("test").containsValue("1"));

        assertTrue(HistorizedInputFactory.deleteAll());

    }

    @Test
    public void testVolatile() throws Exception {
        Volatile v = new Volatile(1000, "test");
        assertTrue(!v.expired());
        assertTrue(v.get().equals("test"));

        ConcurrentUtil.sleep(1100);
        assertTrue(v.expired());
        assertTrue(v.get() == null);

        v.set("test1");
        assertTrue(!v.expired());
        assertTrue(v.get().equals("test1"));
    }

    @Test
    public void testFieldReader() throws Exception {
        //TODO
    }

    @Test
    public void testBaseTest() throws Exception {
        //TODO
    }

    /**
     * it's not really a test but prints some informations to the console...
     */
    @Test
    public void testArgumentator() {
        //printutil is a complex use case for argumentator. without any arguments, the help screen will be printed.
        PrintUtil.main(new String[0]);

        String keyValues = Argumentator.staticKeyValues(IPresentable.class, int.class);
        System.out.println("Presentable types and styles:");
        System.out.println(keyValues);

        keyValues = Argumentator.staticValues(MimeConstants.class, String.class);
        System.out.println("mime types:");
        System.out.println(keyValues);
    }

    @Test
    public void testSecurity() {
        BeanClass.call(AppLoader.class, "noSecurity", false);
        System.out.println(System.getSecurityManager());
        System.out.println(Policy.getPolicy());
    }

    @Test
    public void testAntRunner() {
        String destFile = "test/test.jar";
        File basedir = new File("./");
        FileSet[] fileSets = AntRunner.createFileSets("./:{**/*.*ml}**/*.xml;" + basedir.getPath() + ":{*.txt}");
        Properties props = new Properties();
        props.put("destFile", new File(destFile));
        AntRunner.runTask("Jar", props, fileSets);
        assertTrue(new File(destFile).exists());
        new File(destFile).delete();
    }

    @Test
    public void testTranslateNames() {
        Properties res = createTestTranslationProperties();
        String name;
        final String TRANSLATED = "Meine Methode";
        assertEquals(TRANSLATED, getTranslated(res, name = "MM"));
        assertEquals(TRANSLATED, getTranslated(res, name = "myMethod"));
    }

    private Properties createTestTranslationProperties() {
        Properties props = new Properties();
        props.put("MM", "Meine Methode");
        props.put("my", "Meine");
        props.put("method", "Methode");
        return props;
    }

    private String getTranslated(Properties res, String name) {
        String[] words = StringUtil.splitCamelCase(name);
        StringBuilder str = new StringBuilder(name.length() * 2);
        String value;
        for (int i = 0; i < words.length; i++) {
            value = res.getProperty(words[i]);
            if (value == null)
                value = res.getProperty(StringUtil.toFirstLower(words[i]));
            str.append(value + " ");
        }
        return str.toString().trim();
    }

    @Test
    public void testTranslation() throws Exception {
        if (!NetUtil.isOnline()) {
            LOG.warn("SKIPPING online tests for JSON");
            return;
        }
        Properties p = createTestTranslationProperties();
        Properties t = Translator.translateProperties("test", p, Locale.ENGLISH, Locale.GERMAN);

        //the words are german - so, no translation can be done --> p = t. it's only an integration test
        assertEquals(p, t);
    }

    @Test
    public void testTranslationFast() throws Exception {
        if (!NetUtil.isOnline()) {
            LOG.warn("SKIPPING online tests for Online-Translation");
            return;
        }
        Map p = createTestTranslationProperties();
        Map t = Translator.translatePropertiesFast("test", p, Locale.ENGLISH, Locale.GERMAN);
        //the words are german - so, no translation can be done --> p = t. it's only an integration test
        assertEquals(p, t);
    }

    @Test
    public void testPrimitives() throws Exception {
        int i = 10;
        long l = PrimitiveUtil.convert(i, long.class);
//        Long l = long.class.cast(i);
        assertEquals(i, l);
    }

    @Test
    @Ignore("seems not to work on suns jdk1.7")
    public void testAnnotationProxy() throws Exception {
        Element origin = AnnotationProxy.getAnnotation(SimpleXmlAnnotator.class, "attribute", Element.class);
        Annotation proxy =
            (Annotation) AnnotationProxy.createProxy(new AnnotationProxy(origin, "name", "ruleCover", "type", CommonTest.class));
        //this seems not work on suns jdk1.7
        Annotation[] annotations = AnnotationProxy.getAnnotations(SimpleXmlAnnotator.class, "attribute");

        annotations[0] = proxy;

        assertTrue(CommonTest.class
            .equals(AnnotationProxy.getAnnotation(SimpleXmlAnnotator.class, "attribute", Element.class).type()));
    }

    @Test
    @Ignore("seems not to work on suns jdk1.7")
    public void testAnnotationValueChange() throws Exception {
        Element origin = AnnotationProxy.getAnnotation(SimpleXmlAnnotator.class, "attribute", Element.class);
        //this seems not to work on suns jdk1.7
        int count = AnnotationProxy.setAnnotationValues(origin, "name", "ruleCover", "type", CommonTest.class);
        assertTrue(count == 2);
        assertTrue(CommonTest.class
            .equals(AnnotationProxy.getAnnotation(SimpleXmlAnnotator.class, "attribute", Element.class).type()));
    }

    @Test
    public void testFuzzyFilter() throws Exception {
        String[] c = new String[] { "DisT", "Dies ist ein Test", "irgendwas anderes" };
        assertTrue(StringUtil.fuzzyMatch(c[0], "dist") == 1);
        assertTrue(StringUtil.fuzzyMatch(c[1], "dist") == 1d / 2 / 4);
        assertTrue(StringUtil.fuzzyMatch(c[2], "dist") == 0);
    }

    @Test
    public void testClassFinder() throws Exception {
        //trigger the classloader to load that class
        System.out.println(getClass().getClassLoader().loadClass(CommonAction.class.getName()));
        
        String[] c = new String[] { ClassFinder.class.getName(), "tslcoreclsfind", "fzzyfnd" };
        ClassFinder finder = new ClassFinder(this.getClass().getClassLoader());
        assertTrue(finder.fuzzyFind(c[0], Class.class, -1, null).containsValue(ClassFinder.class));
        assertTrue(finder.fuzzyFind(c[1], null, Modifier.PUBLIC, null).containsValue(ClassFinder.class));
        assertTrue(finder.fuzzyFind(c[2], Method.class, -1, null).containsValue(
            ClassFinder.class.getMethod("fuzzyFind", String.class, Class.class, int.class, Class.class)));
        assertTrue(finder.findClass(IAction.class).size() >= 1);
    }
    
    @Ignore
    @Test
    public void testYaml() throws Exception {
        /*
         * test it on Environments
         */
        ENV env = (ENV) BeanClass.getBeanClass(ENV.class).callMethod(null, "self");
        String dump = YamlUtil.dump(env);
        System.out.println(dump);
        ENV env2 = YamlUtil.load(dump, ENV.class);
        assertTrue(dump.equals(YamlUtil.dump(env2)));
        /*
         * test it on BeanDefinitions
         */
        TypeBean def = new TypeBean();
        def.string = "test";
        dump = YamlUtil.dump(def);
        System.out.println(dump);
        //to compare xml with yaml...
//        XmlUtil.saveXml("test.xml", def);
        
        //reload the yaml
        TypeBean def2 = YamlUtil.load(dump, TypeBean.class);
        assertTrue(def.equals(def2));
    }

    @Test
    public void testEHttpClient() throws Exception {
        //query url
        String urlQuery = EHttpClient.parameter("http://www.openstreetmap.org/search?", false, "city", "München", "street", "Berliner Str.1");
        assertEquals("http://www.openstreetmap.org/search?city=M%C3%BCnchen&street=Berliner+Str.1", urlQuery);

        //rest url
        String resource = "http://localhost:8080/myresource/";
        String urlREST = EHttpClient.parameter(resource, true, "city", "München", "street", "Berliner Str.1");
        assertEquals(resource + "city/M%C3%BCnchen/street/Berliner+Str.1", urlREST);

//        new EHttpClient(resource, true).rest("{code}/info", "GET", "application/json", null, "code", "8000", "street", null);
        
        urlREST = EHttpClient.parameter(resource + "{city}/info", true, "city", "München", "street", null);
        assertEquals(resource + "M%FCnchen/info", urlREST);
    }

    @Ignore
    @Test
    public void testCPUTime() throws Exception {
        new ThreadState().top(500);
        for (int i = 0; i < 100; i++) {
            Math.atan((double) i);
            ConcurrentUtil.sleep(100);
        }
    }

    @Test
    public void testLoadDependencies() throws Exception {
        try {
            LogFactory.setLogLevel(LogFactory.DEBUG);
            new NetworkClassLoader(getClass().getClassLoader()).findClass("paket.Irgendwas");
            fail("Classloader should simply not find that class");
        } catch (Exception ex) {
            if (!(ex instanceof ClassNotFoundException) || !ex.getMessage().contains("paket.Irgendwas"))
                fail("Classloader should simply not find that class - without any other Exceptions");
        }
    }
    
}
