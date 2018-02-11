/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 03.11.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.h5;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.History;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class NanoH5IntegrationTest implements ENVTestPreparation {
    private static String TEST_DIR;

    @BeforeClass
    public static void setUp() {
        TEST_DIR = ENVTestPreparation.setUp("h5", false) + TARGET_TEST;
    }

    @AfterClass
    public static void tearDown() {
//        ENVTestPreparation.tearDown();
    }

    static String getServiceURL() {
        return "http://localhost:" + NetUtil.getNextFreePort(8067);
    }

    private void runNano(String serviceURL) throws IOException {
        //TODO: start nano.h5 and hsqldb
        new NanoH5(serviceURL, null);
    }

    private Process runNanoFromJar() throws IOException {
        String jarName = "tsl2.nano.h5-" + System.getProperty("build.version") + "-standalone.jar";
        String targetPath = "target/";
        assertTrue(FileUtil.copy(targetPath + jarName, TEST_DIR));
        
        ENV.setProperty("websocket.use", false);
        ENV.persist();
        assertTrue(FileUtil.copy(TEST_DIR + "environment.xml", TEST_DIR + ENV.PREFIX_ENVNAME + ENV.CONFIG_NAME + "/environment.xml"));
        
//        Process process = Runtime.getRuntime().exec(new String[] {"cmd.exe", "/C", "java", "-jar", jarName}, null, new File(TEST_DIR));
        Process process = SystemUtil.execute(new File(TEST_DIR), false, "cmd.exe", "/C", "java", "-jar", jarName);
        ConcurrentUtil.sleep(15000);
        return process;
    }


    @Test
//    @Ignore
    public void testNano() throws Exception {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");
        System.setProperty("JAVA_OPTS", "-Xmx512m -Djava.awt.headless -agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n");
        System.setProperty("tsl2nano.offline", "true");
        String serviceURL = getServiceURL();
//        runNano(serviceURL);
        Process process = null;
        PipedOutputStream myOut = new PipedOutputStream();
        InputStream testIn = new PipedInputStream(myOut);
        System.setIn(testIn);
        try {
            
            process = runNanoFromJar();
            
            WebClient webClient = new WebClient();
            webClient.getOptions().setTimeout(1200000); //20min
            HtmlPage page = webClient.getPage(serviceURL);
            page = submit(page, "tsl2nano.login.ok");
            page = submit(page, "beancollectorliste.selectall");
            page = submit(page, "beancollectorliste.open");

            for (int i = 0; i < 7; i++) {
                //create and delete objects of all sample types
                page = testObjectCreation(page);
            }
        } finally {
            if (process != null) {
                myOut.write("\n\n".getBytes());
//                process.destroy();
            }
        }
    }

    private HtmlPage submit(HtmlPage page, String buttonName) throws Exception {
//        List<HtmlForm> forms = page.getForms();
//        for (HtmlForm htmlForm : forms) {
//            System.out.println(htmlForm.getNameAttribute());
//        }
//        HtmlForm form = page.getFormByName("page.form");
//        HtmlForm  form = (HtmlForm) page.getElementById("page.form");
        System.out.println("htmlUnit testing button: " + buttonName);
        return ((HtmlButton)page.getElementById(buttonName)).click();
//        return form.getInputByName(buttonName).click();
    }

    private History back(HtmlPage page) throws Exception {
        return page.getWebClient().getWebWindows().get(0).getHistory().back();
    }

    private HtmlPage testObjectCreation(HtmlPage page) throws Exception {
        String beanName = page.getBody().getId();
        page = submit(page, beanName + "." + "search");
        page = submit(page, beanName + "." + "forward");
        submit(page, beanName + "." + "print");
        back(page);
        submit(page, beanName + "." + "export");
        back(page);

        page = submit(page, beanName + "." + "new");
        page = submit(page, beanName + "." + "save");
        page = submit(page, beanName + "." + "delete");
        page = submit(page, beanName + "." + "reset");
        page = submit(page, beanName + "." + "cancel");
        return page;
    }

}
