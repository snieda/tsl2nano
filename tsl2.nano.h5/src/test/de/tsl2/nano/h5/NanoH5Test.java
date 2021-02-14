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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;

import org.anonymous.project.Address;
import org.anonymous.project.Charge;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import de.tsl2.nano.action.IStatus;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanProxy;
import de.tsl2.nano.bean.IBeanContainer;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.IPresentableColumn;
import de.tsl2.nano.bean.def.IValueDefinition;
import de.tsl2.nano.codegen.ACodeGenerator;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.Main;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.classloader.NestedJarClassLoader;
import de.tsl2.nano.core.classloader.RuntimeClassloader;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.DeclaredMethodComparator;
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
import de.tsl2.nano.h5.collector.CSheet;
import de.tsl2.nano.h5.configuration.BeanConfigurator;
import de.tsl2.nano.h5.navigation.Workflow;
import de.tsl2.nano.h5.timesheet.Timesheet;
import de.tsl2.nano.incubation.specification.ParType;
import de.tsl2.nano.incubation.specification.Pool;
import de.tsl2.nano.incubation.specification.rules.Rule;
import de.tsl2.nano.incubation.specification.rules.RuleScript;
import de.tsl2.nano.persistence.GenericLocalBeanContainer;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.util.test.BaseTest;
import my.app.MyApp;
import my.app.Times;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@net.jcip.annotations.NotThreadSafe
public class NanoH5Test implements ENVTestPreparation {
    static final String MVN_BUILD_PATH = "target";
    
    static String getServiceURL() {
        return "http://localhost:" + NetUtil.getFreePort();
    }

    @After
    public void tearDown() {
//    	ENV.getProperties().keySet().forEach(k->System.getProperties().remove(k));
    	ENV.reset();
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
        setENVProperties();
    	// deep scheint mit andreren Tests zu kollidieren....
        System.setProperty("nanoh5test.run.deep", "false");
        createAndTest(new MyApp(getServiceURL(), null) {
            @Override
            public void start() {
                createStartPage();
                createBeanCollectors(null);
            }
        }, null, Times.class);
    }

    protected static void setENVProperties() {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");
        System.setProperty("JAVA_OPTS", "-Xmx512m -Djava.awt.headless -agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n");
//        System.setProperty("tsl2nano.offline", "true");
//        System.setProperty("websocket.use", "false");
        System.setProperty("app.show.startpage", "false");
        System.setProperty("app.session.anticsrf", "false");
        System.setProperty("app.update.last", new java.sql.Date(0).toString());
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
        
        ACodeGenerator.start(new String[] { "target/test-classes/" + pckName.replace('.', '/'), "codegen/beanconstant.vm" }, null, 0);

        Persistence.current().save();
        
        initServices();
        
        app.start();
//        Translator.translateBundle(ENV.getConfigPath() + "messages", Messages.keySet(), Locale.ENGLISH,
//            Locale.getDefault());
        ENV.setProperty("rule.check.specifications", false);//the check was done on creation - on different jdks (e.g. graalvm) javascript returns different numeric types!
        ENV.persist();

        //now we reload the configurations...
//        new Html5Presentation().reset();
//        ENV.reset();
        app.reset();
        
        //preload class DOMExtender to be found by ClassFinder
        DOMExtender preloadClass = new DOMExtender();
        System.out.print(preloadClass.toString());
        
        DOMExtenderThymeleaf preloadThyme = new DOMExtenderThymeleaf();
        System.out.print(preloadThyme.toString());

//        ENV.create(DIR_TEST);
    	ENV.addService(Main.class, app);
        initServices();
        NanoH5Session session = app.createSession(NetUtil.getInetAddress());
        
        Map pars;
        if ((app instanceof Timesheet) && isDeepTest()) {
        	//runs model-creation and session login-ok action
        	ENV.setProperty("app.database.internal.server.run", true);
        	session.nav.next(null);
            Bean login = Bean.getBean(Persistence.current());
	        pars = login.toValueMap(null);
	        pars.remove("persistence.jdbcProperties");
	        pars.put("jtaDataSource", "nix");
	        pars.put("tsl2nano.login.ok", "true");
        } else {
        	pars = new HashMap<>();
        }
        Socket sampleSocket = new Socket();
        Map header = MapUtil.asMap("socket", sampleSocket, "cookie", "session-id=" + session.getKey() + ";");
		Response response = app.serve("/", Method.POST, header, pars, new HashMap<>());

        String html = null, exptectedHtml;
        for (int i = 0; i < beanTypesToCheck.length; i++) {
            Bean bean = Bean.getBean(BeanClass.createInstance(beanTypesToCheck[i]));
            //check session and collector
            BeanCollector<Collection<Object>,Object> beanCollector = BeanCollector.getBeanCollector(Arrays.asList(bean.getInstance()), BeanCollector.MODE_ALL);
            beanCollector.onActivation(null);
            ((Html5Presentation)beanCollector.getPresentationHelper()).build(session, beanCollector, "test", true, session.getNavigationStack());
            beanCollector.onDeactivation(null);

            //check bean and  attributes
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
                if (attr.getColumnDefinition() != null) {
                    Bean.getBean(attr.getColumnDefinition()).toValueMap(null, false, false, false, "value");
                }
            }
            ((Html5Presentation)bean.getPresentationHelper()).build(session, bean, "test", true, session.getNavigationStack());
    		if (!isDeepTest()) {
    			response = app.serve("/" + i+1, Method.POST, header, new HashMap<>(), new HashMap<>());
    		}
            html = ByteUtil.toString(response.getData(), "UTF-8");
            assertTrue(html.contains(DOMExtender.class.getName())); // see DOMExtender class
            bean.onDeactivation(null);

        }

        //smoke test on loading virtuals
        Charge charge = new Charge();
        for(BeanDefinition<?> beanDef: BeanDefinition.loadVirtualDefinitions()) {
        	assertTrue(beanDef.toString(), beanDef instanceof BeanCollector || beanDef instanceof CSheet);
        	if (beanDef instanceof Bean)
        		continue;
        	BeanCollector<List<Charge>, Charge> beanCollector = (BeanCollector) beanDef;
//        	beanDef.onActivation(new HashMap());
        	if (beanCollector.getSearchAction() != null)
        		beanCollector.getSearchAction().activate();
        	for (IPresentableColumn col : beanCollector.getColumnDefinitions()) {
            	beanCollector.getColumnText(charge, col.getIndex());
        	}
        }
        
        // check encoding (only if german!)
         assertTrue(!Locale.getDefault().equals(Locale.GERMANY) || html.contains("S&amp;chlie&szlig"));
         assertTrue("possible encoding problems found in html-output", !html.contains("ï»¿"));
         
         //create a new expected file (after new changes in the gui)
         String expFileName = "test-" + name + "-output" + (isDeepTest() ? "-deep" : "") + ".html";
         FileUtil.writeBytes(html.getBytes(), ENV.getConfigPath() + expFileName, false);
         
        //static check against last expteced state
       exptectedHtml = new String(FileUtil.getFileBytes(expFileName, null));
//       BaseTest.assertEquals(exptectedHtml, html, true, MapUtil.asMap("\\:[0-9]{5,5}", ":XXXXX",
//           "20\\d\\d(-\\d{2})*", BaseTest.XXX,
//           "[0-9]{1,6} Sec [0-9]{1,6} KB", "XXX Sec XXX KB", 
//           "statusinfo-[0-9]{13,13}\\.txt", "statusinfo-XXXXXXXXXXXXX.txt",
//           BaseTest.REGEX_DATE_US, BaseTest.XXX,
//           BaseTest.REGEX_DATE_DE, BaseTest.XXX,
//           BaseTest.REGEX_TIME_DE, BaseTest.XXX,
//           "startedAt", BaseTest.XXX,
//           "endedAt", BaseTest.XXX,
//           "Started At", BaseTest.XXX,
//           "Ended At", BaseTest.XXX,
//           "tsl2.nano.h5-\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?[\\-\\.0-9]*", "tsl2.nano.h5-X.X.X",
//           ".quicksearch", "?quicksearch", // the '?' does not match between the two sources!
//           "(\\w+[:])?((/|[\\\\])([.]?\\w+)+)+", BaseTest.XXX //absolute file pathes
//           ));
       
        //check xml failed files - these are written, if simple-xml has problems on deserializing from xml
        List<File> failed = FileUtil.getTreeFiles(DIR_TEST, ".*.xml.failed");
        assertTrue(failed.toString(), failed.size() == 0);
        
        //check workflow and specifications
        Workflow workflow = ENV.get(Workflow.class);
        if (workflow != null)
            assertTrue(!workflow.isEmpty());
        
        NanoH5.registereExpressionsAndPools();
        ENV.get(Pool.class).loadRunnables();

        app.stop();
        
        //create xsd from trang.jar
        final String PATH_TRANG_JAR = "../../../../tsl2.nano.common/lib-tools/trang.jar";
        assertTrue(FileUtil.userDirFile(DIR_TEST + "/" + PATH_TRANG_JAR).exists());
        int trangResult = SystemUtil.executeShell(FileUtil.userDirFile(DIR_TEST),
        		"java -jar " + PATH_TRANG_JAR + " presentation/*.xml presentation/beandef.xsd")
        		.exitValue();
        assertTrue(trangResult == 0);

        //extract language messages
        String basedir = FileUtil.userDirFile(DIR_TEST).getParent() + "/";
        String srcPath = "de/tsl2/nano/h5/timesheet/";
        String path = projectPath() + "src/test/" + srcPath;
        String initDB = "init-" + name + "-anyway.sql";
        final String BIN_DIR = targetPath() + "test-classes/";
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
        assertTrue(FileUtil.copy(projectPath() + "src/resources/run.bat", basedir + "run.bat"));
        assertTrue(FileUtil.copy(projectPath() + "src/resources/run.sh", basedir + "run.sh"));

        //create  run configuration
        FileUtil.writeBytes((projectPath() + "run.bat " + new File(DIR_TEST).getName()).getBytes(), basedir + name + ".cmd", false);
        FileUtil.writeBytes((projectPath() + "./run.sh " + new File(DIR_TEST).getName()).getBytes(), basedir + name + ".sh", false);
        
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
        // new File(DIR_TEST).deleteOnExit();
//        p.clear();
//        p.put("dir", DIR_TEST);
//        AntRunner.runTask(AntRunner.TASK_DELETE, p, (String)null);
    }

    private boolean isDeepTest() {
		return "true".equals((System.getProperty("nanoh5test.run.deep")));
	}

	private void initServices() {
        ENV.addService(BeanPresentationHelper.class, new Html5Presentation<>());
        String userName = Persistence.current().getConnectionUserName();
        Authorization auth = Authorization.create(userName, false);
        ENV.addService(IAuthorization.class, auth);
        ConcurrentUtil.setCurrent(auth);

        // BeanContainer.initEmtpyServiceActions();
        // BeanContainerUtil.initEmptyProxyServices();
        BeanContainerUtil.initProxyServiceFactory();
        GenericLocalBeanContainer.initLocalContainer();
        ServiceFactory.instance().setSubject(auth.getSubject());
        ENV.addService(IBeanContainer.class, BeanContainer.instance());
        ConcurrentUtil.setCurrent(BeanContainer.instance());
        ENV.addService(EntityManager.class, BeanProxy.createBeanImplementation(EntityManager.class));
    }

    //workaround for different base-paths on starting the tests (windows+maven <-> linux+maven)
    public static String projectPath() {
        String userDir = System.getProperty("user.dir");
        return userDir.contains(MVN_BUILD_PATH) ? "../" : "";
    }

    //workaround for different base-paths on starting the tests (windows+maven <-> linux+maven)
    public static String targetPath() {
        String userDir = System.getProperty("user.dir");
        return userDir.contains(MVN_BUILD_PATH) ? "" : MVN_BUILD_PATH + File.separatorChar;
    }
    /**
     * createENV
     * @param name
     * @return
     */
    public static String createENV(String name) {
        String DIR_TEST = targetPath() + "test/" + ENV.PREFIX_ENVNAME + name;
        DIR_TEST = new File(DIR_TEST).getAbsolutePath();//on different test starts (mvn, ide) you have diffeent user.dirs
        DIR_TEST = FileUtil.getRelativePath(DIR_TEST);
        
//        new File(DIR_TEST).delete();
//        Files.deleteIfExists(Paths.get(DIR_TEST));
        FileUtil.deleteRecursive(new File(DIR_TEST));

        ENV.create(DIR_TEST);
        Bean.clearCache(); //needs an existing ENV
        RuntimeClassloader cl = new RuntimeClassloader(new URL[0]);
        cl.addFile(ENV.getConfigPath());
        ENV.addService(ClassLoader.class, cl);
        return DIR_TEST;
    }

    @Test
    public void testTimesheet() throws Exception {
    	// deep scheint mit andreren Tests zu kollidieren....
        System.setProperty("app.session.anticsrf", "false");
        System.setProperty("nanoh5test.run.deep", "true");
        System.setProperty("app.update.interval.days", "-1");
        Properties mapper = new Properties();
        createAndTest(new Timesheet(getServiceURL(), null) {
            @Override
            public void start() {
                if (isDeepTest()) {
	            	createStartPage();
	                extractJarScripts();
	                extractDefaultResources();
	                try {
						enableSSL(true);
					} catch (IOException e) {
						ManagedException.forward(e);
					}
                }
                createBeanCollectors(null);
            }
        }, mapper, Charge.class);
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void testAttributeExpression() throws Exception {
        createENV("restful");
        new NanoH5();
        BeanConfigurator<Address> bconf = BeanConfigurator.create(Address.class).getInstance();
        bconf.actionAddAttribute("", "@http://www.openstreetmap.org/search?query={city}<<GET:text/html");
        bconf.actionAddAttribute("", "?select count(*) from Address");
        
        Bean.clearCache();
        ENV.reload();

        Address address = new Address();
        address.setCity("München");
        address.setStreet("Frankfurter Strasse 1");
        Bean<Address> bean = Bean.getBean(address);

        IValueDefinition attr = bean.getAttribute("www.openstreetmap.org");
        System.out.println(attr.getValue());

        //TODO: queries are defined in 'specification'.
        //TODO: name clash...
        attr = bean.getAttribute(FileUtil.getValidFileName("?select count(*) from Address"));
        System.out.println(attr.getValue());
    }
    
    @Test
    public void testCSheetRule() throws Exception {
        createENV("csheet-rule");
        /*
         * test csheet as logic table like excel
         */
        NanoH5.registereExpressionsAndPools();
        ENV.addService(BeanPresentationHelper.class, new Html5Presentation<>());
        new Pool().add(new Rule<>("test", "A1*B1", null));
        CSheet cSheet = new CSheet("test", 3, 3);
        cSheet.set(0, 1, 2, Rule.PREFIX + "test");
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
        Assert.assertEquals("2", cSheet.get(0, 2));
        cSheet.set(1, 0, new BigDecimal(5));
        Assert.assertTrue(cSheet.get(1, 2).equals(new BigDecimal(25)));
    }
    
    /**
     * testJarClassloader
     */
    @Test
//    @Ignore("the jar file will be created after test...")
    public void testJarClassloader() {
        ENV.create("target/test/classloader");
        ENV.assignENVClassloaderToCurrentThread();
        FileUtil.copy("../build.properties", ENV.getConfigPath() + "build-version.properties");
        String version = getCurrentVersion();
        assertTrue(version != null);
        String jarname = "target/tsl2.nano.h5-" + version + "-standalone.jar";
        if (new File(jarname).exists()) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            NestedJarClassLoader cl = new NestedJarClassLoader(contextClassLoader, "standalone") {
                @Override
                protected String getRootJarPath() {
                    return jarname;
                }
    //
    //            @Override
    //            protected ZipInputStream getJarInputStream(String jarName) {
    //                return getExternalJarInputStream(jarName);
    //            }
            };
    
            // filter the 'standalones'
            assertEquals(30, cl.getNestedJars().length);
        }
    }

    private String getCurrentVersion() {
        Properties props = FileUtil.loadProperties("build-version.properties");
        return props.getProperty("tsl2.nano.h5.version");
    }

    @Test
    public void testPersistenceMethodOrder() {
        Collection<java.lang.reflect.Method> elements = Arrays.asList(Persistence.class.getMethods());
        VerifyComparators.verifyTransitivity(new DeclaredMethodComparator(), elements );
        BeanDefinition<Persistence> bPers = BeanDefinition.getBeanDefinition(Persistence.class);
        assertEquals(19, bPers.getAttributeNames().length);
        
        Bean<Persistence> persistenceUI = PersistenceUI.createPersistenceUI(Persistence.current(), null);
        assertEquals("connectionUserName", persistenceUI.getAttributeNames()[0]);
    }

    @Test
    public void testCheckSecurity() {
        //ok
        NanoH5Session.checkSecurity(MapUtil.asProperties("myFieldName", "myValue"));

        //not ok
        try {
            NanoH5Session.checkSecurity(MapUtil.asProperties("myFieldName", "</script>"));
            fail();
        } catch (IllegalArgumentException ex) {
            //ok
        }

        try {
            NanoH5Session.checkSecurity(MapUtil.asProperties("myFieldName", "<script/>"));
            fail();
        } catch (IllegalArgumentException ex) {
            //ok
        }
    }
    @Test
    public void testCheckSecurityBlacklist() {
        String strBlackList = ENV.get("app.input.blacklist", "</,/>,notallowed");
        String allowedFields = ENV.get("app.input.blacklist.fieldnames.allowed.regex", "ZZZZZZ");

        NanoH5Session.checkSecurity(MapUtil.asProperties("myFieldName", "myValue"));
        //field is allowed
        NanoH5Session.checkSecurity(MapUtil.asProperties("ZZZZZZ", "notallowed"));

        //not ok
        try {
            NanoH5Session.checkSecurity(MapUtil.asProperties("myFieldName", "notallowed"));
            fail();
        } catch (IllegalArgumentException ex) {
            //ok
        }
    }
    
    @Test
    public void testGetServiceURL() {
    	assertEquals("http://localhost:8067", NanoH5.getServiceURL(null).toString());
    	
    	// offline it is only the localhost
    	ENV.setProperty("service.access.remote", true);
    	assertEquals("http://localhost:8067", NanoH5.getServiceURL(null).toString());
    	
    	// with null it does only a reset to the defaults!
    	ENV.setProperty("app.ssl.activate", true);
    	assertEquals("http://localhost:8067", NanoH5.getServiceURL(null).toString());
    	
    	ENV.setProperty("app.ssl.activate", true);
    	assertEquals("https://localhost:8067", NanoH5.getServiceURL("http://localhost:8067").toString());
    }
}
//TODO: wieder rausschmeissen...
class VerifyComparators
{
    /**
     * only for testing comparators
     */
    public static <T> void verifyTransitivity(Comparator<T> comparator, Collection<T> elements)
    {
        for (T first: elements)
        {
            for (T second: elements)
            {
                int result1 = comparator.compare(first, second);
                int result2 = comparator.compare(second, first);
                if (result1 != -result2)
                {
                    // Uncomment the following line to step through the failed case
                    //comparator.compare(first, second);
                    throw new AssertionError("compare(" + first + ", " + second + ") == " + result1 +
                        " but swapping the parameters returns " + result2);
                }
            }
        }
        for (T first: elements)
        {
            for (T second: elements)
            {
                int firstGreaterThanSecond = comparator.compare(first, second);
                if (firstGreaterThanSecond <= 0)
                    continue;
                for (T third: elements)
                {
                    int secondGreaterThanThird = comparator.compare(second, third);
                    if (secondGreaterThanThird <= 0)
                        continue;
                    int firstGreaterThanThird = comparator.compare(first, third);
                    if (firstGreaterThanThird <= 0)
                    {
                        // Uncomment the following line to step through the failed case
                        //comparator.compare(first, third);
                        throw new AssertionError("compare(" + first + ", " + second + ") > 0, " +
                            "compare(" + second + ", " + third + ") > 0, but compare(" + first + ", " + third + ") == " +
                            firstGreaterThanThird);
                    }
                }
            }
        }
    }
}