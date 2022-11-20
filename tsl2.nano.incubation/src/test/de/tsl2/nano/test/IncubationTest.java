/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Nov 11, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.test;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.autotest.TypeBean;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.Finished;
import de.tsl2.nano.core.execution.IRunnable;
import de.tsl2.nano.core.secure.Crypt;
import de.tsl2.nano.core.secure.Permutator;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.graph.SVGChart;
import de.tsl2.nano.util.XmlGenUtil;

/**
 * basic tests for algorithms to be refactored to the project tsl2nano.common in future.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class IncubationTest implements ENVTestPreparation {

    @Before
    public void setUp() {
        ENVTestPreparation.super.setUp("incubation");
    }

    @AfterClass
    public static void tearDown() {
        ENVTestPreparation.tearDown();
    }
    
    
    @Test
    public void testPermutator() throws Exception {
        final Map p = MapUtil.asMap("data", "Meier", "algorithm", Crypt.ALGO_PBEWithMD5AndDES);
        final String transformer =
            "transformer=\"de.tsl2.nano.core.util.Crypt encrypt ${data} ${password} ${algorithm}\"";
        final String backward = "backward=\"de.tsl2.nano.core.util.Crypt decrypt ${data} ${password} ${algorithm}\"";

        int len = 7;
        Permutator perm = new Permutator(len);
        InputStream in = perm.permute();
        try {
            ByteUtil.forEach(in, len, new IRunnable<Object, byte[]>() {
                @Override
                public Object run(byte[] context, Object... extArgs) {
                    p.put("password", new String(context));
                    String t = StringUtil.insertProperties(transformer, p);
                    String b = StringUtil.insertProperties(backward, p);

                    Permutator.main(new String[] { "source=deutsche-namen.txt", t, b });
                    return null;
                }
            });
        } catch (Finished f) {
            log(f.getMessage());
        }
    }

    @Test
    public void testSimpleXml() throws Exception {
        //check java classloading of an array
        String clsName = byte[].class.getName();//"[B";//"java.lang.byte[]";
        Class cls = getClass().forName(clsName);
        //try it again through loadClass() - this fails!
//        Class cls = getClass().getClassLoader().loadClass(clsName);
        assertEquals(cls.getName(), clsName);

        TypeBean bean = new TypeBean();
        bean.setPrimitiveChar(' ');
        bean.setType(cls);

        String xmlfile = "test/typebean.xml";
        File file = FileUtil.userDirFile(xmlfile);
        file.getParentFile().mkdirs();
        XmlGenUtil.saveSimpleXml_(file.getPath(), bean);
        TypeBean bean1 = XmlGenUtil.loadSimpleXml_(file.getPath(), TypeBean.class, true);
        assertEquals(bean.getType(), bean1.getType());
    }

    @Test
    public void testXml2Text() throws Exception {
//        log(StringUtil.removeXMLTags(String.valueOf(FileUtil.getFileData("printing-xml-with-fop.html", "UTF-8"))));
    }

    @Test
    public void testSVGGraph() throws Exception {
        new File(ENV.getTempPath()).mkdirs();
        SVGChart.createGraph("Nano Graph", "X", "Y", 500, 400, 2d, 1d, 0d);

        //test the file import
        String t = " # Test-Graph\n#generiert...\nX Y1  Y2\n1.0   2.0   3.0\n4    5   6\n7    8   9";
        SVGChart.createChart(StringUtil.toInputStream(t));
    }

    static void log_(String msg) {
        System.out.print(msg);
    }

    static void log(String msg) {
        System.out.println(msg);
    }
}


