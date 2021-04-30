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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
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
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.action.IConstraint;
import de.tsl2.nano.core.Argumentator;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.execution.AntRunner;
import de.tsl2.nano.execution.ScriptUtil;
import de.tsl2.nano.util.AdapterProxy;
import de.tsl2.nano.util.PrintUtil;
import de.tsl2.nano.util.Translator;
import de.tsl2.nano.util.test.inverse.AutoFunctionTest;

/**
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(Suite.class)
@SuiteClasses({ AutoFunctionTest.class })
public class CommonTest implements ENVTestPreparation {
    private static final Log LOG = LogFactory.getLog(CommonTest.class);
    private static String BASE_DIR_COMMON;

    @BeforeClass
    public static void setUp() {
        BASE_DIR_COMMON = ENVTestPreparation.setUp();
    }

    @AfterClass
    public static void tearDown() {
        ENVTestPreparation.tearDown();
    }
    
    
    @Ignore("old ant version 1.65 is on the classpath, result in error")
    @Test
    public void testScriptUtil() throws Exception {
        final Properties p = new Properties();
        new File(BASE_DIR_COMMON + "target/lib").getAbsoluteFile().mkdirs();
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
        assertTrue(ScriptUtil.executeShell(new File("./").getAbsoluteFile(), "echo", "hello").exitValue() == 0);
        assertTrue(ScriptUtil.execute(BASE_DIR_COMMON + "src/resources/runsh.sh").exitValue() == 0);
        //works only on windows
//        ScriptUtil.execute("c:/Program Files (x86)/Adobe/Reader 10.0/Reader/AcroRd32.exe", "c:/eigen/SVN-Eclipse-Einrichtung.pdf");
//        ScriptUtil.executeRegisteredWindowsPrg("c:/eigen/SVN-Eclipse-Einrichtung.pdf");
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
        String destFile = "test.jar";
        File basedir = new File("./");
        FileSet[] fileSets = AntRunner.createFileSets("./:{**/*.*ml}**/*.xml;" + basedir.getPath() + ":{*.txt}");
        Properties props = new Properties();
        props.put("destFile", ENVTestPreparation.testpath(destFile));
        AntRunner.runTask("Jar", props, fileSets);
        assertTrue(new File(destFile).getAbsoluteFile().exists());
        new File(destFile).getAbsoluteFile().delete();
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
	public void testAdapterProxy() {
        IAttribute a = AdapterProxy.create(IAttribute.class);
        a.setName("mytest");
        a.setValue(null, "myvalue");

        assertEquals("mytest", a.getName());
        assertEquals("myvalue", a.getValue(null));
	}

	@Test
	public void testAdapterProxyInMap() throws Exception {
        ISession session = AdapterProxy.create(ISession.class);
        Hashtable<Object, Object> map = new Hashtable<>();
        map.put(Boolean.FALSE, "0");
        map.put(session, "1");
        map.put(Boolean.TRUE, "2");
        assertEquals(/*"1"*/null, map.remove(session));
	}

}
