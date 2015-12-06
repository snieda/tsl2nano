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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import my.app.MyApp;
import my.app.Times;

import org.anonymous.project.Charge;
import org.apache.tools.ant.taskdefs.Classloader;
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
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.classloader.RuntimeClassloader;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.execution.AntRunner;
import de.tsl2.nano.execution.SystemUtil;
import de.tsl2.nano.h5.Html5Presentation;
import de.tsl2.nano.h5.NanoH5;
import de.tsl2.nano.h5.navigation.Parameter;
import de.tsl2.nano.h5.timesheet.Timesheet;
import de.tsl2.nano.incubation.specification.ParType;
import de.tsl2.nano.incubation.specification.rules.RulePool;
import de.tsl2.nano.incubation.specification.rules.RuleScript;
import de.tsl2.nano.persistence.GenericLocalBeanContainer;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;
import de.tsl2.nano.test.TypeBean;
import de.tsl2.nano.util.Translator;
import de.tsl2.nano.util.codegen.PackageGenerator;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class NanoH5Test {

    static String getServiceURL() {
        return "http://localhost:" + NetUtil.getFreePort();
    }

    @Test
    @Ignore
    public void testNano() throws Exception {
        String serviceURL = getServiceURL();
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
    public void testJS() throws Exception {
        LinkedHashMap<String, ParType> p = new LinkedHashMap<String, ParType>();
        Charge charge = new Charge();
        charge.setFromdate(DateUtil.getToday());
        p.put("charge", new ParType(Charge.class));
        p.put("formatter", new ParType(new SimpleDateFormat("EE")));
//        RuleScript<String> script = new RuleScript<String>("weekday", "var options = {weekday: 'short'}; charge.getFromdate().toLocaleDateString('de-DE', options);", p);
        RuleScript<String> script = new RuleScript<String>("weekday", "formatter.format(charge.getFromdate());", p);
        String result = script.run(MapUtil.asMap("charge", charge));
        assertTrue(new SimpleDateFormat("EE").format(DateUtil.getToday()).equals(result));
    }
    
    @Test
    public void testMyApp() throws Exception {
        createAndTest(new MyApp(getServiceURL(), null) {
            @Override
            public void start() {
                createBeanCollectors(null);
            }
        }, null, Times.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void createAndTest(NanoH5 app, Properties anywayMapper, Class... beanTypesToCheck) throws Exception {
        assert beanTypesToCheck != null && beanTypesToCheck.length > 0 : "at least one beantype must be given!";
        String name = BeanClass.getDefiningClass(app.getClass()).getSimpleName().toLowerCase();
        final String DIR_TEST = "test/.nanoh5." + name;
//        new File(DIR_TEST).delete();
        Files.deleteIfExists(Paths.get(DIR_TEST));

        ENV.create(DIR_TEST);
        RuntimeClassloader cl = new RuntimeClassloader(new URL[0]);
        cl.addFile(ENV.getConfigPath());
        ENV.addService(ClassLoader.class, cl);
        //first: generate all configurations....
        if (anywayMapper != null) {
            //TODO: map names. e.g. : Charge --> TimeEntry
        }
        String pckName = beanTypesToCheck[0].getPackage().getName();
        System.setProperty("bean.generation.packagename", pckName);
        System.setProperty("bean.generation.outputpath", DIR_TEST);
        PackageGenerator.main(new String[] { "bin/" + pckName.replace('.', '/') });

        ENV.addService(BeanPresentationHelper.class, new Html5Presentation<>());
        try {
            Persistence.current().save();
        } catch (IOException e) {
            ManagedException.forward(e);
        }
        String userName = Persistence.current().getConnectionUserName();
        ENV.addService(IAuthorization.class, Authorization.create(userName, false));

//      BeanContainer.initEmtpyServiceActions();
        GenericLocalBeanContainer.initLocalContainer(ENV.get(ClassLoader.class), false);
        ENV.addService(IBeanContainer.class, BeanContainer.instance());
        app.start();
//        Translator.translateBundle(ENV.getConfigPath() + "messages", Messages.keySet(), Locale.ENGLISH,
//            Locale.getDefault());
        ENV.persist();

        //now we reload the configurations...
        new Html5Presentation().reset();
        ENV.reset();

        ENV.create(DIR_TEST);
        for (int i = 0; i < beanTypesToCheck.length; i++) {
            Bean bean = Bean.getBean(BeanClass.createInstance(beanTypesToCheck[i]));
            System.out.println(bean.toValueMap(null));
        }

        //check xml failed files - these are written, if simple-xml has problems on deserializing from xml
        List<File> failed = FileUtil.getTreeFiles(DIR_TEST, ".*.xml.failed");
        assertTrue(failed.toString(), failed.size() == 0);
        
        //create xsd from trang.jar
        assertTrue(new File(DIR_TEST + "/" + "../../../tsl2.nano.common/lib-tools/trang.jar").exists());
        assertTrue(SystemUtil.execute(new File(DIR_TEST), "cmd", "/C",
            "java -jar ../../../tsl2.nano.common/lib-tools/trang.jar presentation/*.xml presentation/beandef.xsd")
            .exitValue() == 0);

        //extract language messages
        String basedir = new File(DIR_TEST).getParent() + "/";
        String path = "src/test/de/tsl2/nano/h5/timesheet/";
        String initDB = "init-" + name + "-anyway.sql";
        assertTrue(FileUtil.copy(path + initDB, DIR_TEST + "/" + initDB));
        assertTrue(FileUtil.copy(path + "messages_de.properties", DIR_TEST + "/messages_de.properties"));
        assertTrue(FileUtil.copy(path + "messages_de_DE.properties", DIR_TEST + "/messages_de_DE.properties"));
        assertTrue(FileUtil.copy("src/resources/run.bat", basedir + "run.bat"));
        assertTrue(FileUtil.copy("src/resources/run.sh", basedir + "run.sh"));

        //create  run configuration
        FileUtil.writeBytes(("run.bat " + new File(DIR_TEST).getName()).getBytes(), basedir + name + ".bat", false);
        FileUtil.writeBytes(("run.sh " + new File(DIR_TEST).getName()).getBytes(), basedir + name + ".sh", false);
        
        //create a deployable package
        Properties p = new Properties();
        p.put("destFile", "target/" + name + ".zip");
        AntRunner.runTask(AntRunner.TASK_ZIP, p, new File(DIR_TEST).getParent() + ":{**}");
        
        //delete the test output
//        ConcurrentUtil.sleep(10000);
        new File(DIR_TEST).deleteOnExit();
//        p.clear();
//        p.put("dir", DIR_TEST);
//        AntRunner.runTask(AntRunner.TASK_DELETE, p, (String)null);
    }

    @Test
    public void testTimesheet() throws Exception {
        Properties mapper = new Properties();
        createAndTest(new Timesheet(getServiceURL(), null) {
            @Override
            public void start() {
                createBeanCollectors(null);
            }
        }, mapper, Charge.class);
    }
}
