package de.tsl2.nano.jarresolver;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.apache.commons.logging.Log;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;

/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 19.11.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */

/**
 * Tests jar resolving
 * 
 * @author Tom
 * @version $Revision$
 */
public class JarResolverTest   implements ENVTestPreparation {
    private static final Log LOG = LogFactory.getLog(JarResolverTest.class);

    private static String BASE_DIR_JARRESOLVER;

    @BeforeClass
    public static void setUp() {
        BASE_DIR_JARRESOLVER = ENVTestPreparation.setUp("jarresolver", false) + TARGET_TEST;
    }

    @AfterClass
    public static void tearDown() {
//        ENVTestPreparation.tearDown();
    }
    
    /**
     * NEEDS TO BE ONLINE!
     * <p/>
     * positive real tests for searching jars and negative tests for not existing packages are done. The check, if the
     * downloaded jar holds the desired class is not done!
     * 
     * @throws Exception
     */
    @Ignore("maven does not start with the base dir (target/test) given in this test")
    @Test
    public void testJarResolving() throws Exception {
        if (!NetUtil.isOnline()) {
            System.out.println("ignoring test 'testFindJar() - we are offline!");
            return;
        }

        //positive real tests
        assertTrue(download("org.apache.log4j.config.PropertyGetter", "log4j.*.jar"));
        assertTrue(download("log4j", "log4j.*.jar"));
        assertTrue(download("org.apache.tools.ant.launch.Locator", "ant.*.jar"));

        //negative tests
        try {
            assertFalse(download("org.anonymous123.product23.TestClass", null));
//            fail("this package shouldn't be found!");
        } catch (Exception ex) {
            //Ok - shouldn't be found
        }
        try {
            assertFalse(download("org.apache.product23.TestClass", null));
//            fail("this package shouldn't be found!");
        } catch (Exception ex) {
            //Ok - shouldn't be found
        }
        
        delete("(dist|target|temp, action)");
    }

    private void delete(String expression) {
        FileUtil.forEach(BASE_DIR_JARRESOLVER, expression, FileUtil.DO_DELETE);
    }

    /**
     * download
     */
    private boolean download(String pckOrName, String jarName) {
        if (jarName != null) {
            delete(jarName);
        }
        new JarResolver(BASE_DIR_JARRESOLVER).start(new String[] { pckOrName });
        return jarName != null ? loaded(jarName) : false;
    }

    /**
     * checks, if one of the given file was really stored/downloaded - and deletes that file.
     * 
     * @param string
     * @return
     */
    private boolean loaded(String name) {
        boolean found = FileUtil.getFiles(BASE_DIR_JARRESOLVER, name).length > 0;
        if (found) {
            delete(name);
        }
        return found;
    }

    @Test
    public void testFindJar() throws Exception {
        if (NetUtil.isOnline()) {
            String jarName = new JarResolver(BASE_DIR_JARRESOLVER).findJarOnline("org.apache.log4j.config.PropertyGetter");
            LOG.info("jar-file: " + jarName);
            assertTrue(StringUtil.extract(jarName, "\\w+").equals("log4j"));
        } else {
            System.out.println("ignoring test 'testFindJar() - we are offline!");
        }
    }
}
