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
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class NanoH5IT implements ENVTestPreparation {
    private static String TEST_DIR;
    static boolean nanoAlreadyRunning = true;//Boolean.getBoolean("app.server.running");

    static final String BEANCOLLECTORLIST = (BeanCollector.class.getSimpleName() 
            + Messages.getString("tsl2nano.list")).toLowerCase();
    
    @BeforeClass
    public static void setUp() {
        if (!nanoAlreadyRunning)
            TEST_DIR = ENVTestPreparation.setUp("h5", false) + TARGET_TEST;
        else {
            System.out.println("NanoH5IT: nanoAlreadyRunning=true ==> trying to connect to external NanoH5");
            ConcurrentUtil.sleep(15000);
        }
    }

    @AfterClass
    public static void tearDown() {
//        ENVTestPreparation.tearDown();
    }

    static String getServiceURL(boolean nextFreePort) {
        return "http://localhost:" + (nextFreePort ? NetUtil.getNextFreePort(8067) : 8067);
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
        ENV.setProperty("app.app.show.startpage", false);
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
        String serviceURL = getServiceURL(!nanoAlreadyRunning);
//        runNano(serviceURL);
        Process process = null;
        HtmlPage page = null;
        PipedOutputStream myOut = new PipedOutputStream();
        InputStream testIn = new PipedInputStream(myOut);
        System.setIn(testIn);
        try {
            if (!nanoAlreadyRunning)
                process = runNanoFromJar();
            
            System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "info");
            WebClient webClient = new WebClient();
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setTimeout(1200000); //20min
            webClient.getOptions().setPrintContentOnFailingStatusCode(true);
            webClient.getOptions().setCssEnabled(true);
            page = webClient.getPage(serviceURL);
            page = submit(page, "tsl2nano.login.ok");
            page = submit(page, BEANCOLLECTORLIST + ".selectall");
            page = submit(page, BEANCOLLECTORLIST + ".open");

            //-> all collectors now open - closing them in the testObjectCreation()
            int beanTypeCount = 1;//TODO: test it for all bean-types!
            for (int i = 0; i < beanTypeCount; i++) {
                //create and delete objects of all sample types
//                HtmlCheckBoxInput checkbox = page.getElementByName(String.valueOf(i));
//                page = submit(page, "beancollectorliste.open");
                page = testObjectCreation(page);
            }

        } finally {
            if (page != null) {
                int i = 0;
                String id;
                //TODO: does not work yet!!!
                while (i ++ < 30) {
                    id = page.getBody().getId();
                    if (!Util.isEmpty(id) && id.toLowerCase().equals(BEANCOLLECTORLIST)) {
                        page = submit(page, BEANCOLLECTORLIST + ".administration");
                        page = submit(page, BEANCOLLECTORLIST + ".shutdown");
                        break;
                    }
                    page = back(page);
                }
            }
            if (process != null) {
                System.out.println("trying to shutdown nanoh5 server through ENTER...");
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
        try {
            return ((HtmlButton)page.getElementById(buttonName)).click();
        } catch (Exception e) {
            String asXml = "<!--\nbutton not found: " + buttonName + "\n" +
                    ManagedException.toString(e) + "\n-->\n" + page.asXml();
            FileUtil.writeBytes(asXml.getBytes(), ENV.getTempPath() + "page-failed.html", false);
            ManagedException.forward(e);
            return page;
        }
//        return form.getInputByName(buttonName).click();
    }

    private HtmlPage back(HtmlPage page) throws Exception {
        page.getWebClient().getWebWindows().get(0).getHistory().back();
        return (HtmlPage) page;//.refresh();
    }

    private HtmlPage testObjectCreation(HtmlPage page) throws Exception {
        String pageId = page.getBody().getId();
        if (Util.isEmpty(pageId))
            throw new IllegalStateException("pageId is empty!");
        String beanName = StringUtil.toFirstLower(pageId); 
        String beanList = beanName +  Messages.getString("tsl2nano.list").toLowerCase();
        
        //TODO: check pages with with saved last current state
        page = submit(page, beanList + "." + "search");
        page = submit(page, beanList + "." + "forward");
        page = submit(page, beanList + "." + "back");
//        submit(page, beanList + "." + "print");
//        page = back(page);
//        submit(page, beanList + "." + "export");
//        page = back(page);

        page = submit(page, beanList + "." + "new");
        page = submit(page, "de.tsl2.nano.action.action_cancelled");
//        page = submit(page, beanName + "." + "save");
//        page = submit(page, beanList + "." + "delete");
//        page = submit(page, beanName + "." + "reset");
        return page;
    }

}
