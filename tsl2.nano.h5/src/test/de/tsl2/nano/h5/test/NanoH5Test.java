/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 02.08.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import my.app.MyApp;
import my.app.Times;

import org.anonymous.project.Charge;
import org.junit.Ignore;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.History;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.IBeanContainer;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.execution.SystemUtil;
import de.tsl2.nano.h5.Html5Presentation;
import de.tsl2.nano.h5.NanoH5;
import de.tsl2.nano.h5.timesheet.Timesheet;
import de.tsl2.nano.incubation.specification.rules.RulePool;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.test.TypeBean;
import de.tsl2.nano.util.codegen.PackageGenerator;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class NanoH5Test {

    @Test
    @Ignore
    public void testNano() throws Exception {
        String serviceURL = "http://localhost:8066";
        runNano(serviceURL);

        WebClient webClient = new WebClient();
        HtmlPage page = webClient.getPage(serviceURL);
        page = submit(page, "tsl2.nano.login.ok");
        page = submit(page, "beancollector.selectall");
        page = submit(page, "beancollector.open");

        for (int i = 0; i < 7; i++) {
            //create and delete objects of all sample types
            page = testObjectCreation(page);
        }
    }

    private HtmlPage submit(HtmlPage page, String buttonName) throws Exception {
        HtmlForm form = page.getFormByName("page.form");
        return form.getInputByName(buttonName).click();
    }

    private History back(HtmlPage page) throws Exception {
        return page.getWebClient().getWebWindows().get(0).getHistory().back();
    }

    private HtmlPage testObjectCreation(HtmlPage page) throws Exception {
        String beanName = page.getTitleText();
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

    private void runNano(String serviceURL) throws IOException {
        //TODO: start nano.h5 and hsqldb
        new NanoH5(serviceURL, null);
    }

    @Test
    public void testNetUtilRestful() throws Exception {
        String url = "http://localhost/rest";
        Class<?> responseType = String.class;
//        Event event =
//            BeanProxy.createBeanImplementation(Event.class, MapUtil.asMap("type", "mouseclick", "target", null), null,
//                null);
        //TODO: how to provide parameter of any object type?
//        Point p = new Point(5,5);
        Object args[] = new Object[] { "event", "x", 5, "y", 5 };

        //create the server (see service class RestfulService, must be public!)
        HttpServer server = HttpServerFactory.create(url);
        server.start();

        //request..
        String response = NetUtil.getRestful(url/*, responseType*/, args);
        server.stop(0);

        assertTrue(response != null && responseType.isAssignableFrom(response.getClass()));
        assertTrue(response.equals("5, 5"));
    }

    @Test
    public void testMyApp() throws Exception {
        createAndTest(new MyApp() {
            @Override
            public void start() {
                createBeanCollectors(null);
            }
        }, null, Times.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void createAndTest(NanoH5 app, Properties anywayMapper, Class...beanTypesToCheck) throws Exception {
        assert beanTypesToCheck != null && beanTypesToCheck.length > 0 : "at least one beantype must be given!";
        final String DIR_TEST = "test/" + BeanClass.getDefiningClass(app.getClass()).getSimpleName().toLowerCase();
        new File(DIR_TEST).delete();
        ENV.create(DIR_TEST);
        //first: generate all configurations....
        if (anywayMapper != null) {
            //TODO: map names. e.g. : Charge --> TimeEntry
        }
        String pckName = beanTypesToCheck[0].getPackage().getName();
        System.setProperty("bean.generation.packagename", pckName);
        System.setProperty("bean.generation.outputpath", DIR_TEST);
        PackageGenerator.main(new String[] { "bin/" + pckName.replace('.', '/') });

        BeanContainer.initEmtpyServiceActions();
        ENV.addService(IBeanContainer.class, BeanContainer.instance());
        app.start();
        ENV.persist();

        //now we reload the configurations...
        new Html5Presentation().reset();
        ENV.reset();

        ENV.create(DIR_TEST);
        for (int i = 0; i < beanTypesToCheck.length; i++) {
            Bean bean = Bean.getBean(BeanClass.createInstance(beanTypesToCheck[i]));
            System.out.println(bean.toValueMap(null));
        }

        //create xsd from trang.jar
        assertTrue(new File(DIR_TEST + "/" + "../../../tsl2.nano.common/lib-tools/trang.jar").exists());
        assertTrue(SystemUtil.execute(new File(DIR_TEST), "cmd", "/C", "java -jar ../../../tsl2.nano.common/lib-tools/trang.jar presentation/*.xml presentation/beandef.xsd").exitValue() == 0);
        
        //extract language messages
        ENV.extractResource(pckName.replace('.', '/') + "/messages_de.properties");
        ENV.extractResource(pckName.replace('.', '/') + "/messages_de_DE.properties");
    }

    @Test
    public void testTimesheet() throws Exception {
        Properties mapper = new Properties();
        createAndTest(new Timesheet() {
            @Override
            public void start() {
                createBeanCollectors(null);
            }
        }, mapper, Charge.class);
    }
}
