import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.apache.commons.logging.Log;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.jarresolver.JarResolver;

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
public class JarResolverTest {
    private static final Log LOG = LogFactory.getLog(JarResolverTest.class);

    private static final String BASEDIR = "./";

    @BeforeClass
    public static void setUp() {
        //set another start path to store the jar files to
    }

    /**
     * NEEDS TO BE ONLINE!
     * <p/>
     * positive real tests for searching jars and negative tests for not existing packages are done. The check, if the
     * downloaded jar holds the desired class is not done!
     * 
     * @throws Exception
     */
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
        
        FileUtil.forEach("./", "(dist|target|temp, action)", FileUtil.DO_DELETE);
    }

    /**
     * download
     */
    private boolean download(String pckOrName, String jarName) {
        if (jarName != null) {
            FileUtil.forEach("./", jarName, FileUtil.DO_DELETE);
        }
        new JarResolver().start(new String[] { pckOrName });
        return jarName != null ? loaded(jarName) : false;
    }

    /**
     * checks, if one of the given file was really stored/downloaded - and deletes that file.
     * 
     * @param string
     * @return
     */
    private boolean loaded(String name) {
        boolean found = FileUtil.getFiles(BASEDIR, name).length > 0;
        if (found) {
            FileUtil.forEach("./", name, FileUtil.DO_DELETE);
        }
        return found;
    }

    @Test
    public void testFindJar() throws Exception {
        if (NetUtil.isOnline()) {
            String jarName = new JarResolver().findJarOnline("org.apache.log4j.config.PropertyGetter");
            LOG.info("jar-file: " + jarName);
            assertTrue(StringUtil.extract(jarName, "\\w+").equals("log4j"));
        } else {
            System.out.println("ignoring test 'testFindJar() - we are offline!");
        }
    }

    @AfterClass
    public static void tearDown() {
        //TODO: delete all loaded jars
    }
}
