/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 02.08.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.anonymous.project.Address;
import org.anonymous.project.Charge;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;

import de.tsl2.nano.action.IStatus;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.IBeanContainer;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.IValueDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.classloader.NestedJarClassLoader;
import de.tsl2.nano.core.classloader.RuntimeClassloader;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.messaging.ChangeEvent;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.execution.AntRunner;
import de.tsl2.nano.h5.NanoHTTPD.Method;
import de.tsl2.nano.h5.NanoHTTPD.Response;
import de.tsl2.nano.h5.configuration.BeanConfigurator;
import de.tsl2.nano.h5.expression.QueryPool;
import de.tsl2.nano.h5.navigation.Workflow;
import de.tsl2.nano.h5.timesheet.Timesheet;
import de.tsl2.nano.incubation.specification.ParType;
import de.tsl2.nano.incubation.specification.actions.ActionPool;
import de.tsl2.nano.incubation.specification.rules.RulePool;
import de.tsl2.nano.incubation.specification.rules.RuleScript;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;
import de.tsl2.nano.util.codegen.PackageGenerator;
import de.tsl2.nano.util.test.BaseTest;
import my.app.MyApp;
import my.app.Times;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class NanoH5Test implements ENVTestPreparation {
    static String getServiceURL() {
        return "http://localhost:" + NetUtil.getFreePort();
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
        String response = NetUtil.getRest(url/*, responseType*/, args);
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
                createStartPage();
                createBeanCollectors(null);
            }
        }, null, Times.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void createAndTest(NanoH5 app, Properties anywayMapper, Class... beanTypesToCheck) throws Exception {
        assert beanTypesToCheck != null && beanTypesToCheck.length > 0 : "at least one beantype must be given!";
        String name = BeanClass.getDefiningClass(app.getClass()).getSimpleName().toLowerCase();
        final String DIR_TEST = createENV(name);
        //first: generate all configurations....
        if (anywayMapper != null) {
            //TODO: map names. e.g. : Charge --> TimeEntry
        }
        String pckName = beanTypesToCheck[0].getPackage().getName();
        System.setProperty("bean.generation.packagename", pckName);
        System.setProperty("bean.generation.outputpath", DIR_TEST);
        
        PackageGenerator.main(new String[] { "target/test-classes/" + pckName.replace('.', '/') });

        Persistence.current().save();
        
        ENV.addService(BeanPresentationHelper.class, new Html5Presentation<>());

        initServices();
        
        app.start();
//        Translator.translateBundle(ENV.getConfigPath() + "messages", Messages.keySet(), Locale.ENGLISH,
//            Locale.getDefault());
        ENV.persist();

        //now we reload the configurations...
        new Html5Presentation().reset();
        ENV.reset();

        //preload class DOMExtender to be found by ClassFinder
        DOMExtender preloadClass = new DOMExtender();
        System.out.print(preloadClass.toString());
        
        ENV.create(DIR_TEST);
        initServices();
        NanoH5Session session = app.createSession(NetUtil.getInetAddress());
        
        Socket sampleSocket = new Socket();
        app.serve("/", Method.POST, MapUtil.asMap("socket", sampleSocket), new HashMap<>(), new HashMap<>());
        String html = null, exptectedHtml;
        for (int i = 0; i < beanTypesToCheck.length; i++) {
            Bean bean = Bean.getBean(BeanClass.createInstance(beanTypesToCheck[i]));
            bean.onActivation(null);
            System.out.println(bean.toValueMap(null));
            Bean.getBean(bean.getPresentable()).toValueMap(null);
            bean.getActions();
            bean.getPlugins();
            String[] attributeNames = bean.getAttributeNames();
            IValueDefinition attr;
            for (int j = 0; j < attributeNames.length; j++) {
                attr = bean.getAttribute(attributeNames[j]);
                IStatus status = attr.getStatus();
                if (status != null && status.error() != null)
                    throw new IllegalStateException(status.error());
                attr.changeHandler().fireEvent(ChangeEvent.createEvent(attr, null, null, false));
                //trigger all possible rulecovers
                Bean.getBean(attr.getConstraint()).toValueMap(null);
                Bean.getBean(attr.getPresentation()).toValueMap(null);
                if (attr.getColumnDefinition() != null)
                    Bean.getBean(attr.getColumnDefinition()).toValueMap(null, false, false, false, "value");
            }
            ((Html5Presentation)bean.getPresentationHelper()).build(session, bean, "test", true, session.getNavigationStack());
            Response response = app.serve("/" + i+1, Method.POST, MapUtil.asMap("socket", sampleSocket), new HashMap<>(), new HashMap<>());
            html = ByteUtil.toString(response.getData(), "UTF-8");
            assertTrue(html.contains(DOMExtender.class.getName())); // see DOMExtender class
            bean.onDeactivation(null);

            //check session and collector
//            BeanCollector<Collection<Object>,Object> beanCollector = BeanCollector.getBeanCollector(Arrays.asList(bean.getInstance()), BeanCollector.MODE_ALL);
//            beanCollector.onActivation(null);
//            ((Html5Presentation)beanCollector.getPresentationHelper()).build(session, beanCollector, "test", true, session.getNavigationStack());
//            beanCollector.onDeactivation(null);
        }

        // check encoding (only if german!)
         assertTrue(!Locale.getDefault().equals(Locale.GERMANY) || html.contains("S&amp;chlie&szlig"));

         //create a new expected file (after new changes in the gui)
         String expFileName = "test-" + name + "-output.html";
         FileUtil.writeBytes(html.getBytes(), ENV.getConfigPath() + expFileName, false);
         
        //static check against last expteced state
       exptectedHtml = new String(FileUtil.getFileBytes(expFileName, null));
       BaseTest.assertEquals(exptectedHtml, html, true, MapUtil.asMap("\\:[0-9]{5,5}", ":XXXXX",
           "[0-9]{1,6} Msec", "XXX Msec", "statusinfo-[0-9]{13,13}\\.txt", "statusinfo-XXXXXXXXXXXXX.txt",
           BaseTest.REGEX_DATE_US, BaseTest.XXX,
           BaseTest.REGEX_DATE_DE, BaseTest.XXX,
           BaseTest.REGEX_TIME_DE, BaseTest.XXX,
           "startedAt", BaseTest.XXX,
           "endedAt", BaseTest.XXX,
           "Started At", BaseTest.XXX,
           "Ended At", BaseTest.XXX,
           ".quicksearch", "?quicksearch" // the '?' does not match between the two sources!
           ));
       
        //check xml failed files - these are written, if simple-xml has problems on deserializing from xml
        List<File> failed = FileUtil.getTreeFiles(DIR_TEST, ".*.xml.failed");
        assertTrue(failed.toString(), failed.size() == 0);
        
        //check workflow and specifications
        Workflow workflow = ENV.get(Workflow.class);
        if (workflow != null)
            assertTrue(!workflow.isEmpty());
        
        ENV.get(RulePool.class);
        ENV.get(QueryPool.class);
        ENV.get(ActionPool.class);

        app.stop();
        
        //create xsd from trang.jar
        final String PATH_TRANG_JAR = "../../../../tsl2.nano.common/lib-tools/trang.jar";
        assertTrue(new File(DIR_TEST + "/" + PATH_TRANG_JAR).exists());
        assertTrue(SystemUtil.execute(new File(DIR_TEST), "cmd", "/C",
            "java -jar " + PATH_TRANG_JAR + " presentation/*.xml presentation/beandef.xsd")
            .exitValue() == 0);

        //extract language messages
        String basedir = new File(DIR_TEST).getParent() + "/";
        String srcPath = "de/tsl2/nano/h5/timesheet/";
        String path = "src/test/" + srcPath;
        String initDB = "init-" + name + "-anyway.sql";
        final String BIN_DIR = "target/test-classes/";
        //TODO: create myapp test db
//        assertTrue(FileUtil.copy(path + initDB, DIR_TEST + "/" + initDB));
        assertTrue(FileUtil.copy(path + "ICSChargeImport.java", DIR_TEST + "/generated-src/" + srcPath + "ICSChargeImport.java"));
        assertTrue(FileUtil.copy(path + "ActionImportHolidays.java", DIR_TEST +  "/generated-src/" + srcPath + "/ActionImportHolidays.java"));
        assertTrue(FileUtil.copy(path + "ActionImportCalendar.java", DIR_TEST +  "/generated-src/" + srcPath + "/ActionImportCalendar.java"));
        assertTrue(FileUtil.copy(BIN_DIR + srcPath + "ICSChargeImport.class", DIR_TEST + "/generated-bin/" + srcPath + "ICSChargeImport.class"));
        assertTrue(FileUtil.copy(BIN_DIR + srcPath + "ActionImportHolidays.class", DIR_TEST +  "/generated-bin/" + srcPath + "/ActionImportHolidays.class"));
        assertTrue(FileUtil.copy(BIN_DIR + srcPath + "ActionImportCalendar.class", DIR_TEST +  "/generated-bin/" + srcPath + "/ActionImportCalendar.class"));

        assertTrue(FileUtil.copy(path + "messages_de.properties", DIR_TEST + "/messages_de.properties"));
        assertTrue(FileUtil.copy(path + "messages_de_DE.properties", DIR_TEST + "/messages_de_DE.properties"));
        assertTrue(FileUtil.copy("src/resources/run.bat", basedir + "run.bat"));
        assertTrue(FileUtil.copy("src/resources/run.sh", basedir + "run.sh"));

        //create  run configuration
        FileUtil.writeBytes(("run.bat " + new File(DIR_TEST).getName()).getBytes(), basedir + name + ".cmd", false);
        FileUtil.writeBytes(("run.sh " + new File(DIR_TEST).getName()).getBytes(), basedir + name + ".sh", false);
        
        //workaround: replace path 'test/.nanoh5.timesheet' with '.nanoh5.timesheet'
        AntRunner.runRegexReplace("(target[/]test[/])([.]nanoh5[.]timesheet)[/](icons)", "\\3", new File(DIR_TEST).getParent(), "**");
        AntRunner.runRegexReplace("(target[/]test[/])([.]nanoh5[.]timesheet)", "\\2", new File(DIR_TEST).getParent(), "**");
        
        //create a deployable package
        String destFile = "target/" + name + ".zip";
        Properties p = new Properties();
        p.put("destFile", destFile);
        AntRunner.runTask(AntRunner.TASK_ZIP, p, FileUtil.replaceToJavaSeparator(new File(DIR_TEST).getParent()) + "/:{**/*" + name + "*/**}{timesheet.*}");
        
        //delete the test output
//        ConcurrentUtil.sleep(10000);
        new File(DIR_TEST).deleteOnExit();
//        p.clear();
//        p.put("dir", DIR_TEST);
//        AntRunner.runTask(AntRunner.TASK_DELETE, p, (String)null);
    }

    private void initServices() {
        ENV.addService(BeanPresentationHelper.class, new Html5Presentation<>());
        String userName = Persistence.current().getConnectionUserName();
        Authorization auth = Authorization.create(userName, false);
        ENV.addService(IAuthorization.class, auth);
        ConcurrentUtil.setCurrent(auth);

        BeanContainer.initEmtpyServiceActions();
//        GenericLocalBeanContainer.initLocalContainer(Thread.currentThread().getContextClassLoader(), false);
        ENV.addService(IBeanContainer.class, BeanContainer.instance());
        ConcurrentUtil.setCurrent(BeanContainer.instance());
    }

    /**
     * createENV
     * @param name
     * @return
     */
    String createENV(String name) {
        final String DIR_TEST = "target/test/" + ENV.PREFIX_ENVNAME + name;
        
        Bean.clearCache();
//        new File(DIR_TEST).delete();
//        Files.deleteIfExists(Paths.get(DIR_TEST));
        FileUtil.deleteRecursive(new File(DIR_TEST));

        ENV.create(DIR_TEST);
        RuntimeClassloader cl = new RuntimeClassloader(new URL[0]);
        cl.addFile(ENV.getConfigPath());
        ENV.addService(ClassLoader.class, cl);
        return DIR_TEST;
    }

    //@Ignore("problems with velocity generation")
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
    
    @Test
    public void testAttributeExpression() throws Exception {
        createENV("restful");
        new NanoH5();
        BeanConfigurator<Address> bconf = BeanConfigurator.create(Address.class).getInstance();
        bconf.actionAddAttribute("", "@http://openstreetmap.org/search?query={city}<<GET:text/html");
        bconf.actionAddAttribute("", "?select count(*) from Address");
        
        Bean.clearCache();
        ENV.reload();

        Address address = new Address();
        address.setCity("München");
        address.setStreet("Frankfurter Strasse 1");
        Bean<Address> bean = Bean.getBean(address);

        IValueDefinition attr = bean.getAttribute("openstreetmap.org");
        System.out.println(attr.getValue());

        //TODO: queries are defined in 'specification'.
        //TODO: name clash...
        attr = bean.getAttribute(FileUtil.getValidFileName("?select count(*) from Address"));
        System.out.println(attr.getValue());
    }
    
    @Test
    public void testCSheet() throws Exception {
        createENV("csheet");
        /*
         * test csheet as logic table like excel
         */
        ENV.addService(BeanPresentationHelper.class, new Html5Presentation<>());
        CSheet cSheet = new CSheet("test", 3, 3);
        cSheet.set(0, 1, 2, 3);
        cSheet.set(1, 4, 5, "=A2*B2");
        //test the bean button
        cSheet.getActions().iterator().next().activate();
        //and reload it
        Bean.clearCache();
        BeanDefinition<?> loadedSheet = BeanDefinition.getBeanDefinition("virtual.test");
        Assert.assertTrue(loadedSheet instanceof CSheet);
        cSheet = (CSheet) loadedSheet;
        Assert.assertTrue(cSheet.getLogicForm().getRowCount() == 3);
        Assert.assertTrue(cSheet.getLogicForm().get(1, 2).equals(new BigDecimal(20)));

        cSheet.set(1, 0, new BigDecimal(5));
        Assert.assertTrue(cSheet.get(1, 2).equals(new BigDecimal(25)));
        
        
    }
    
    /**
     * testJarClassloader
     */
    @Test
    @Ignore("the jar file will be created after test...")
    public void testJarClassloader() {
        ENV.create("target/test/classloader");
        ENV.assignENVClassloaderToCurrentThread();
        FileUtil.copy("../build.properties", ENV.getConfigPath() + "build-version.properties");
        String version = getCurrentVersion();
        assertTrue(version != null);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        NestedJarClassLoader cl = new NestedJarClassLoader(contextClassLoader, "standalone") {
            @Override
            protected String getRootJarPath() {
                return "target/tsl2.nano.h5-" + version + "-standalone.jar";
            }
//
//            @Override
//            protected ZipInputStream getJarInputStream(String jarName) {
//                return getExternalJarInputStream(jarName);
//            }
        };

        // filter the 'standalones'
        assertEquals(26, cl.getNestedJars().length);
    }

//    @Ignore("don't do that automatically") 
    @Test
    public void testNetUtilDownload() throws Exception {
        //the test checks the current download path of sourceforge...
        if (NetUtil.isOnline()) {
            Profiler.si().stressTest("downloader", 2, new Runnable() {
                @Override
                public void run() {
                    String url;
                    //https://sourceforge.net/projects/tsl2nano/files/latest/download?source=navbar
                    //http://downloads.sourceforge.net/project/tsl2nano/1.1.0/tsl2.nano.h5.1.1.0.jar
                    //http://netcologne.dl.sourceforge.net/project/tsl2nano/1.1.0/tsl2.nano.h5.1.1.0.jar
                    File download = NetUtil.download(url =
                        "https://iweb.dl.sourceforge.net/project/tsl2nano/1.1.0/tsl2.nano.h5.1.1.0.jar",
                        "target/test/", true, true);
                    NetUtil.check(url, download, 3 * 1024 * 1024);
                }
            });
        }
    }

    private String getCurrentVersion() {
        Properties props = FileUtil.loadProperties("build-version.properties");
        return props.getProperty("tsl2.nano.h5.version");
//        return "2.0.0-SNAPSHOT";
    }

}
