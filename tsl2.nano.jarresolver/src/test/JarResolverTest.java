import org.apache.commons.logging.Log;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.jarresolver.JarResolver;
import static junit.framework.Assert.*;

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
        //positive real tests
        assertTrue(download("org.apache.log4j.config.PropertyGetter", "log4j.*.jar"));
        assertTrue(download("log4j", "log4j.*.jar"));

        //negative tests
        try {
            download("org.anonymous123.product23.TestClass", null);
            fail("this package shouldn't be found!");
        } catch (Exception ex) {
            //Ok - shouldn't be found
        }
        try {
            download("org.apache.product23.TestClass", null);
            fail("this package shouldn't be found!");
        } catch (Exception ex) {
            //Ok - shouldn't be found
        }
    }

    /**
     * download
     */
    private boolean download(String pckOrName, String jarName) {
        new JarResolver().start(new String[] { pckOrName });
        return jarName != null ? loaded(jarName) : false;
    }

    /**
     * checks, if one of the given file was really stored/downloaded.
     * 
     * @param string
     * @return
     */
    private boolean loaded(String string) {
        return FileUtil.getFiles(BASEDIR, "log4j.*.jar").length > 0;
    }

    @Test
    public void testFindJar() throws Exception {
        String jarName = new JarResolver().findJarOnline("org.apache.log4j.config.PropertyGetter");
        LOG.info("jar-file: " + jarName);
        assertTrue(StringUtil.extract(jarName, "\\w+").equals("log4j"));
    }
    
    @AfterClass
    public static void tearDown() {
        //TODO: delete all loaded jars
    }
}
