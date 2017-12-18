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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.tools.ant.types.FileSet;
import org.apache.xmlgraphics.util.MimeConstants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.tsl2.nano.action.IConstraint;
import de.tsl2.nano.collection.ArrSegList;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.FloatArray;
import de.tsl2.nano.core.Argumentator;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ITransformer;
import de.tsl2.nano.core.classloader.NestedJarClassLoader;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.AntRunner;
import de.tsl2.nano.execution.ScriptUtil;
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
@SuppressWarnings({ "unchecked", "rawtypes" })
public class CommonTest {
    private static final Log LOG = LogFactory.getLog(CommonTest.class);
    private static final String BASE_DIR_COMMON = "../tsl2.nano.common/";
    private static final String POSTFIX_TEST = "target/test/";

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
    public void testCollectionUtil() throws Exception {
        //1. converting from arrays to a list
        Object[] arr1 = new Object[] { "Hans M�ller", "Hans Mueller" };
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
            "Hans M�ller"), sortedList);

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

        String keyValues = Argumentator.staticKeyValues(IConstraint.class, int.class);
        System.out.println("constraint types and styles:");
        System.out.println(keyValues);

        keyValues = Argumentator.staticValues(MimeConstants.class, String.class);
        System.out.println("mime types:");
        System.out.println(keyValues);
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
}
