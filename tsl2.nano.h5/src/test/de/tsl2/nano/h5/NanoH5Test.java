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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import org.anonymous.project.Address;
import org.anonymous.project.Charge;
import org.junit.Assert;
import org.junit.Test;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;

import de.tsl2.nano.action.IStatus;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.IBeanContainer;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.IValueDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.classloader.RuntimeClassloader;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.messaging.ChangeEvent;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.DateUtil;
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
import de.tsl2.nano.persistence.GenericLocalBeanContainer;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;
import de.tsl2.nano.util.codegen.PackageGenerator;
import my.app.MyApp;
import my.app.Times;

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
    //@Ignore("problems with velocity generation")
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
        Socket sampleSocket = new Socket();
        app.serve("/", Method.POST, MapUtil.asMap("socket", sampleSocket), new HashMap<>(), new HashMap<>());
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
            Response response = app.serve("/" + i+1, Method.POST, MapUtil.asMap("socket", sampleSocket), new HashMap<>(), new HashMap<>());
            String html = ByteUtil.toString(response.getData(), "UTF-8");
            assertTrue(html.contains(DOMExtender.class.getName())); // see DOMExtender class
            bean.onDeactivation(null);
        }

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
        AntRunner.runTask(AntRunner.TASK_ZIP, p, FileUtil.replaceWindowsSeparator(new File(DIR_TEST).getParent()) + "/:{**/*" + name + "*/**}{timesheet.*}");
        
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
        final String DIR_TEST = "target/test/.nanoh5." + name;
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
    
}
