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

import static de.tsl2.nano.bean.def.IPresentable.UNDEFINED;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.Reader;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.InetAddress;
import java.net.URI;
import java.security.Policy;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.json.JsonObject;
import javax.json.JsonStructure;

import org.apache.commons.logging.Log;
import org.apache.tools.ant.types.FileSet;
import org.apache.xmlgraphics.util.MimeConstants;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.simpleframework.xml.Element;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.action.IActivable;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.CollectionExpressionFormat;
import de.tsl2.nano.bean.def.IBeanCollector;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.bean.def.ValueMatcher;
import de.tsl2.nano.bean.enhance.BeanEnhancer;
import de.tsl2.nano.collection.ArrSegList;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.FloatArray;
import de.tsl2.nano.core.AppLoader;
import de.tsl2.nano.core.Argumentator;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ITransformer;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.classloader.NestedJarClassLoader;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.AnnotationProxy;
import de.tsl2.nano.core.util.Crypt;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.PrintUtil;
import de.tsl2.nano.core.util.SimpleXmlAnnotator;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.currency.CurrencyUnit;
import de.tsl2.nano.currency.CurrencyUtil;
import de.tsl2.nano.execution.AntRunner;
import de.tsl2.nano.execution.ScriptUtil;
import de.tsl2.nano.format.DefaultFormat;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.messaging.ChangeEvent;
import de.tsl2.nano.messaging.IListener;
import de.tsl2.nano.util.Period;
import de.tsl2.nano.util.Translator;
import de.tsl2.nano.util.operation.CRange;
import de.tsl2.nano.util.operation.ConditionOperator;
import de.tsl2.nano.util.operation.IConvertableUnit;
import de.tsl2.nano.util.operation.NumericOperator;
import de.tsl2.nano.util.operation.OperableUnit;
import de.tsl2.nano.util.operation.Operator;

/**
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes", "serial" })
public class CommonTest {
    private static final Log LOG = LogFactory.getLog(CommonTest.class);

    @BeforeClass
    public static void setUp() {
        ENV.setProperty(ENV.KEY_CONFIG_PATH, "test/");
//        Environment.setProperty("strict.mode", true);
        ENV.deleteAllConfigFiles();
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

        //test crypto
        String[] passwds = new String[] { "meinpass", "12345678", "azAzäÄüÜ" };
        for (int i = 0; i < passwds.length; i++) {
            byte[] cryptoHash = StringUtil.cryptoHash("äöüäöüäü");
            LOG.info(passwds[i] + " ==> " + "(" + cryptoHash.length + ") " + StringUtil.toString(cryptoHash, 1000));
            LOG.info(passwds[i] + " crypto-hex: " + StringUtil.toHexString(cryptoHash));
            LOG.info(passwds[i] + "        hex: " + StringUtil.toHexString(passwds[i].getBytes()));
            LOG.info(passwds[i] + "           : " + StringUtil.fromHexString(StringUtil.toHexString(passwds[i].getBytes())));
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

    @Test
    public void testScriptUtil() throws Exception {
        final Properties p = new Properties();
        //par is not used
        p.put("tsl2nano.test.parameter", "test");
        p.put("client.lib.dir", "lib");
        p.put("shared.lib.dir", "lib");
        p.put("server.lib.dir", "lib");
        p.put("lib-tools.dir", "lib");
        if (!ScriptUtil.ant("../tsl2.nano.common/shell.xml", "help", p)) {
            fail("ant call didn't work!");
        }

        //works only on windows
        assertTrue(ScriptUtil.execute("cmd", "/C", "echo", "hello").exitValue() == 0);
        assertTrue(ScriptUtil.execute("cmd", "/C", "runsh.bat").exitValue() == 0);
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

        files = FileUtil.getFileset("./", "**/resources/**/*class.vm");
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
        List<String> textAndNumbers = Arrays.asList("", "-1.000,00", "-1.2",//on german notation it will be -12
            "-1,1",
            "0",
            "1",
            "1,1",
            "2",
            "10",
            "11",
            "1.2",//on german notation it will be 12
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
        Object[] arr1 = new Object[] { "Hans Müller", "Hans Mueller" };
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
            "Hans Müller"), sortedList);

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
            "100000000",//=100.000.000,00
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
        df.applyPattern("###,###,###.00 €");
        LOG.info(df.format(123456789));

        //test the factor and rounding mode
        assertEquals(f, CurrencyUtil.getFactor(d));
        assertEquals(new BigDecimal(51.13d, new MathContext(4)), CurrencyUtil.getActualValue(100f, d));
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
        Collection<TypeBean> result = BeanUtil.fromFlatFile(testFile,
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
        testFile = "commontest-fromflatfile.txt";
        s = "\"0123\",\"456789\",\"Monday\"\n\"1234\",\"567890\",\"Tuesday\"\n";
        FileUtil.writeBytes(s.getBytes(), testFile, false);

        //first, create an example file and test bean
        testFile = "commontest-fromflatfile.txt";
        s = "header\n\"0123\",\"456,789\",\"Monday\"\n\"1234\",\"567890\",\"Tuesday\"\n";
        FileUtil.writeBytes(s.getBytes(), testFile, false);

        //this reader will eliminate the " quotations!
        Reader reader = FileUtil.getTransformingReader(FileUtil.getFile(testFile), ' ', ' ', true);

        //read it with fixed-columns
        result = BeanUtil.fromFlatFile(reader,
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
        result = BeanUtil.fromFlatFile(testFile, TypeBean.class, "1-5:primitiveShort", "5-11:primitiveLong");
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
            "weekdayEnum").values().retainAll(asMap.values()));

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
        System.out.println(BeanUtil.presentAsCSV(collector));
        System.out.println(BeanUtil.presentAsHtmlTable(collector));
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
    }

    /**
     * tests all classes of package 'operation'
     * 
     * @throws Exception
     */
    @Test
    public void testOperation() throws Exception {
        /*
         * create two units: EURO, DM and convert them to each other
         */

        IConvertableUnit<TypeBean, Currency> euroUnit = new IConvertableUnit<TypeBean, Currency>() {
            @Override
            public TypeBean from(Number toValue) {
                TypeBean newBean = new TypeBean();
                newBean.setPrimitiveDouble((Double) toValue);
                return newBean;
            }

            @Override
            public Number to(TypeBean fromValue) {
                return fromValue.getPrimitiveDouble();
            }

            @Override
            public Currency getUnit() {
                return NumberFormat.getCurrencyInstance().getCurrency();
            }
        };
        long e100 = 100;
        TypeBean euro_100 = new TypeBean();
        euro_100.setPrimitiveDouble(e100);

        /*
         * first, we test basic calculation and presentation with unit
         */
        OperableUnit<TypeBean, Currency> euroValue = new OperableUnit<TypeBean, Currency>(euro_100, euroUnit) {
            @Override
            public String toString() {
                return getConversion() + " " + getUnit().getSymbol();
            }
        };
        TypeBean myBean10000 = euroValue.multiply(euro_100);
        assertTrue(myBean10000.getPrimitiveDouble() == 100d * 100d);
        assertTrue(euroValue.toString().equals("100.0 €"));

        /*
         * second, test the converting
         */
        IConvertableUnit<TypeBean, Currency> demUnit = new IConvertableUnit<TypeBean, Currency>() {
            CurrencyUnit currencyUnit = CurrencyUtil.getCurrency(DateUtil.getDate(2000, 1, 1));

            @Override
            public TypeBean from(Number toValue) {
                TypeBean newBean = new TypeBean();
                newBean.setPrimitiveDouble((Double) toValue);
                return newBean;
            }

            @Override
            public Number to(TypeBean fromValue) {
                return fromValue.getPrimitiveDouble() * currencyUnit.getFactor();
            }

            @Override
            public Currency getUnit() {
                return currencyUnit.getCurrency();
            }
        };

        OperableUnit<TypeBean, Currency> demValue = euroValue.convert(demUnit);
        //factor EUR --> DEM
        double c = 100d * 1.95583d;
        assertTrue(demValue.toString().equals(c + " DEM"));

        /*
         * test ranges
         */
        TypeBean min_10 = new TypeBean();
        min_10.setPrimitiveDouble(10d);
        TypeBean max_100 = new TypeBean();
        max_100.setPrimitiveDouble(100d);

        CRange<TypeBean> range = new CRange<TypeBean>(min_10, max_100, euroUnit);
        assertTrue(range.contains(euro_100));
        assertTrue(range.intersects(euro_100, euro_100));

        TypeBean max_99 = new TypeBean();
        max_99.setPrimitiveDouble(99d);
        CRange<TypeBean> rangeOutside = new CRange<TypeBean>(min_10, max_99, euroUnit);
        assertTrue(!rangeOutside.contains(euro_100));
        assertTrue(!rangeOutside.intersects(euro_100, euro_100));

    }

    @Test
    public void testNumericOperator() {
        String f = "1+ ((x1 + x2)*3 + 4)+5";
        BigDecimal x1 = new BigDecimal(8);
        BigDecimal x2 = new BigDecimal(9);
        Map<CharSequence, BigDecimal> values = new Hashtable<CharSequence, BigDecimal>();
        values.put("x1", x1);
        values.put("x2", x2);
        assertEquals(new BigDecimal(61), new NumericOperator(values).eval(f));

        //TODO: implement this case
//        f ="-1 + (-x1 + x2)";
//        assertEquals(BigDecimal.ZERO, new NumericOperator(values).eval(f));
    }

    @Test
    public void testConditionOperator() {
        String f = "(A&B ) | C ? D : E";

        Map<CharSequence, Object> values = new Hashtable<CharSequence, Object>();
        values.put("A", true);
        values.put("C", true);
//        /*
//         * two condition value-possibilities: action or expression
//         */
//        values.put("D", new CommonAction<Object>() {
//            @Override
//            public Object action() throws Exception {
//                return "DD";
//            }
//        });
        assertEquals("D", new ConditionOperator(values).eval(f));

        values.remove(Operator.KEY_RESULT);
        values.put("A", true);
        values.put("C", false);
        values.put("E", "E");
        assertEquals("E", new ConditionOperator(values).eval(f));

        f = " A = B";
        values.put("B", true);
        assertTrue((Boolean) new ConditionOperator(values).eval(f));
        values.put("B", false);
        assertFalse((Boolean) new ConditionOperator(values).eval(f));
    }

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
    @Test
    public void testJarClassloader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        NestedJarClassLoader cl = new NestedJarClassLoader(contextClassLoader, "standalone") {
            @Override
            protected String getRootJarPath() {
                return "../../target/test.h5.sample/tsl2.nano.h5.0.8.0-standalone.jar";
            }
//
//            @Override
//            protected ZipInputStream getJarInputStream(String jarName) {
//                return getExternalJarInputStream(jarName);
//            }
        };

        // filter the 'standalones'
        assertTrue(cl.getNestedJars().length == 16);
    }

    /**
     * testEnhancer
     */
    @Test
    public void testEnhancer() throws Exception {
        /*
         * enhancing an anonymous class, checking the access to the new field
         */
        TypeBean obj = new TypeBean() {
            int test = 1;
        };
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
    public void testNetUtilDownload() throws Exception {
        if (NetUtil.isOnline()) {
            Profiler.si().stressTest("downloader", 20, new Runnable() {
                @Override
                public void run() {
                    //https://sourceforge.net/projects/tsl2nano/files/latest/download?source=navbar
                    NetUtil.download(
                        "http://sourceforge.net/projects/tsl2nano/files/0.8.0-beta/tsl2.nano.h5.0.7.0.jar/download",
                        "test/", true, true);
                }
            });
        }
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

//    @Test
    public void testNetUtilWCopy() throws Exception {
        //not a real test - only to see it working!
        NetUtil.wcopy("http://mobile.chefkoch.de", "test/", null, null);
    }

    @Test
    public void testNetUtilJSON() throws Exception {
        JsonStructure structure = NetUtil.getRestfulJSON("http://headers.jsontest.com/"/*"https://graph.facebook.com/search?q=java&type=post"*/);
        System.out.println(StringUtil.toString(structure, -1));
        
        Bean<JsonStructure> bean = Bean.getBean(structure);
        System.out.println(bean);
        
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
    public void testResourcebundleTranslation() throws Exception {
        Properties p = createTestTranslationProperties();
        Properties t = Translator.translateProperties("test", p, Locale.ENGLISH, Locale.GERMAN);
        //the words are german - so, no translation can be done --> p = t. it's only an integration test
        assertEquals(p, t);
    }

    @Test
    public void testBlockTranslation() throws Exception {
        Properties p = createTestTranslationProperties();
        Properties t = Translator.translateProperties0("test", p, Locale.ENGLISH, Locale.GERMAN);
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
        Annotation proxy = AnnotationProxy.createProxy(new AnnotationProxy(origin, "name", "ruleCover", "type", CommonTest.class));
        //this seems not work on suns jdk1.7
        Annotation[] annotations = AnnotationProxy.getAnnotations(SimpleXmlAnnotator.class, "attribute");
        
        annotations[0] = proxy;
        
        assertTrue(CommonTest.class.equals(AnnotationProxy.getAnnotation(SimpleXmlAnnotator.class, "attribute", Element.class).type()));
    }
    @Test
//    @Ignore("seems not to work on suns jdk1.7")
    public void testAnnotationValueChange() throws Exception {
        Element origin = AnnotationProxy.getAnnotation(SimpleXmlAnnotator.class, "attribute", Element.class);
        //this seems not work on suns jdk1.7
        int count = AnnotationProxy.setAnnotationValues(origin, "name", "ruleCover", "type", CommonTest.class);
        assertTrue(count == 2);
        assertTrue(CommonTest.class.equals(AnnotationProxy.getAnnotation(SimpleXmlAnnotator.class, "attribute", Element.class).type()));
    }
}
