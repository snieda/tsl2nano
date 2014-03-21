package de.tsl2.nano.h5;

import static de.tsl2.nano.bean.def.IBeanCollector.MODE_CREATABLE;
import static de.tsl2.nano.bean.def.IBeanCollector.MODE_EDITABLE;
import static de.tsl2.nano.bean.def.IBeanCollector.MODE_MULTISELECTION;
import static de.tsl2.nano.bean.def.IBeanCollector.MODE_SEARCHABLE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import org.apache.commons.logging.Log;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.action.IActivable;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.IBeanContainer;
import de.tsl2.nano.bean.def.AbstractExpression;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.execution.CompatibilityLayer;
import de.tsl2.nano.execution.SystemUtil;
import de.tsl2.nano.h5.configuration.BeanConfigurator;
import de.tsl2.nano.h5.expression.Query;
import de.tsl2.nano.h5.expression.QueryPool;
import de.tsl2.nano.h5.expression.RuleExpression;
import de.tsl2.nano.h5.expression.SQLExpression;
import de.tsl2.nano.h5.navigation.EntityBrowser;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.h5.navigation.Workflow;
import de.tsl2.nano.persistence.GenericLocalBeanContainer;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.persistence.PersistenceClassLoader;
import de.tsl2.nano.script.ScriptTool;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.util.NumberUtil;

/**
 * An Application of subclassing NanoHTTPD to make a custom HTTP server.
 * 
 * <pre>
 * TODO:
 * - Bean-->BeanValue-->getColumnDefinition() --> Table(columns)
 * - PageBuilder --> Bean.Presentable
 * - Navigation
 * - Verbindung/Abgrenzung BeanContainer
 * - evtl mini-browser wie lynx einbinden
 * - BeanContainer mit cache fuer tests
 * </pre>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class NanoH5 extends NanoHTTPD {
    private static final Log LOG = LogFactory.getLog(NanoH5.class);

    Map<InetAddress, NanoH5Session> sessions;

    IPageBuilder<?, String> builder;
    URL serviceURL;
    ClassLoader appstartClassloader;

    private static final String DEGBUG_HTML_FILE = "application.html";
    static final String START_PAGE = "Start";
    static final int OFFSET_FILTERLINES = 2;

    public NanoH5() throws IOException {
        this(Environment.get("http.connection", "localhost:8067"), Environment.get(IPageBuilder.class));
    }

    public NanoH5(String serviceURL, IPageBuilder<?, String> builder) throws IOException {
        super(getPort(serviceURL), new File(Environment.getConfigPath()));
        this.serviceURL = getServiceURL(serviceURL);
        this.builder = builder != null ? builder : createPageBuilder();
        Environment.registerBundle(NanoH5.class.getPackage().getName() + ".messages", true);
        appstartClassloader = Thread.currentThread().getContextClassLoader();
        Environment.addService(ClassLoader.class, appstartClassloader);
        sessions = new LinkedHashMap<InetAddress, NanoH5Session>();
        AbstractExpression.registerExpression(new RuleExpression().getExpressionPattern(), RuleExpression.class);
        AbstractExpression.registerExpression(new SQLExpression().getExpressionPattern(), SQLExpression.class);
    }

    /**
     * main
     * 
     * @param args
     */
    public static void main(String[] args) {
        startApplication(NanoH5.class, MapUtil.asMap(0, "http.connection"), args);
    }

    /**
     * starts application and shows initial html page
     */
    @Override
    public void start() {
        try {
//            LogFactory.setLogLevel(LogFactory.LOG_ALL);
            LOG.info(System.getProperties());
            createStartPage();
            Environment.saveResourceToFileSystem("run.bat", "../run.bat");
            Environment.saveResourceToFileSystem("shell.xml");
            Environment.saveResourceToFileSystem("mda.bat");
            Environment.saveResourceToFileSystem("mda.xml");
            Environment.saveResourceToFileSystem("mda.properties");
            Environment.saveResourceToFileSystem("beandef.xsd");
            Environment.saveResourceToFileSystem("favicon.ico", "../favicon.ico");
            String dir = Environment.getConfigPath();
            File icons = new File(dir + "icons");
            if (!icons.exists()) {
                FileUtil.extract("tsl2.nano.h5.default-resources.jar", dir, null);
            }

            LOG.info("Listening on port " + serviceURL.getPort() + ". Hit Enter to stop.\n");
            if (System.getProperty("os.name").startsWith("Windows"))
                SystemUtil.executeRegisteredWindowsPrg("application.html");

            myOut = LogFactory.getOut();
            myErr = LogFactory.getErr();
            System.in.read();
        } catch (Exception ioe) {
            LOG.error("Couldn't start server:", ioe);
            System.exit(-1);
        }
    }

    public static URL getServiceURL(String serviceURLString) {
        if (serviceURLString == null)
            serviceURLString = Environment.get("service.url", "http://localhost:8067");
        if (!serviceURLString.matches(".*[:][0-9]{4,4}"))
            serviceURLString = "localhost:" + serviceURLString;
        if (!serviceURLString.contains("://"))
            serviceURLString = "http://" + serviceURLString;
        URL serviceURL = null;
        try {
            serviceURL = new URL(serviceURLString);
        } catch (MalformedURLException e) {
            ManagedException.forward(e);
        }
        return serviceURL;
    }

    public static int getPort(String serviceURL) {
        return Integer.valueOf(StringUtil.substring(serviceURL, ":", null));
    }

    protected String createStartPage() {
        return createStartPage(DEGBUG_HTML_FILE);
    }
    
    /**
     * createStartPage
     * 
     * @param resultHtmlFile
     */
    protected String createStartPage(String resultHtmlFile) {
        InputStream stream = Environment.getResource("start.template");
        String startPage = String.valueOf(FileUtil.getFileData(stream, null));
        startPage = StringUtil.insertProperties(startPage,
            MapUtil.asMap("url", serviceURL, "text", Environment.getName()));
        String page = Html5Presentation.createMessagePage("start.template", "Start " + Environment.getName() + "App", serviceURL);
        FileUtil.writeBytes(page.getBytes(), resultHtmlFile, false);
        return page;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
        if (method.equals("GET") && !NumberUtil.isNumber(uri.substring(1)) && HtmlUtil.isURL(uri)
            && !uri.contains(Html5Presentation.PREFIX_BEANREQUEST))
            return super.serve(uri, method, header, parms, files);
        InetAddress requestor = ((Socket) header.get("socket")).getInetAddress();
        NanoH5Session session = sessions.get(requestor);
        if (session == null) {
            //on a new session, no parameter should be set
            session = createSession(requestor);
        } else {//perhaps session was interrupted/closed but not removed
            if (session.nav == null || session.nav.isEmpty()) {
                sessions.remove(session.inetAddress);
                session = createSession(requestor);
            }
        }
        return session.serve(uri, method, header, parms, files);
    }

    public Response createResponse(String msg) {
        return createResponse(HTTP_OK, MIME_HTML, msg);
    }

    public Response createResponse(String status, String type, String msg) {
        return new NanoHTTPD.Response(status, type, msg);
    }

    /**
     * adds services for presentation and page-builder
     */
    protected IPageBuilder<?, String> createPageBuilder() {
        Html5Presentation pageBuilder = new Html5Presentation();
        Environment.addService(BeanPresentationHelper.class, pageBuilder);
        Environment.addService(IPageBuilder.class, pageBuilder);
        return pageBuilder;
    }

    /**
     * initialize navigation model and perhaps bean-definitions. overwrite this method if you create your extending
     * application. this method will be called, before any bean types are loaded!
     */
    protected NanoH5Session createSession(InetAddress inetAddress) {
        LOG.info("creating new session on socket: " + inetAddress);
        NanoH5Session session = new NanoH5Session(this,
            inetAddress,
            createGenericNavigationModel(),
            Environment.get(ClassLoader.class));
        sessions.put(inetAddress, session);
        return session;
    }

    /**
     * reads all classes of 'beanjar', creates a root beancollector holding a collection of this classes.
     * 
     * @param beanjar jar to resolve the desired entities from
     * @return navigation stack holding a beancollector for all entity classes inside beanjar
     */
    protected IBeanNavigator createGenericNavigationModel() {

        BeanContainer.initEmtpyServiceActions();
        /*
         * create the presentable navigation stack
         */
        Bean<?> login = createPersistenceUnit();

        Workflow workflow = Environment.get(Workflow.class);

        if (workflow == null || workflow.isEmpty()) {
            LOG.debug("creating navigation stack");
            Stack<BeanDefinition<?>> navigationModel = new Stack<BeanDefinition<?>>();
            Bean<String> startPage = Bean.getBean(START_PAGE);
            navigationModel.push(startPage);
            navigationModel.push(login);
            return new EntityBrowser(navigationModel);
        } else {
            workflow.setLogin(login);
            return workflow;
        }
    }

    @SuppressWarnings({ "serial" })
    private Bean<?> createPersistenceUnit() {
        final Persistence persistence = Persistence.current();
        Bean<?> login = Bean.getBean(persistence);
        if (login.toString().matches(Environment.get("default.present.attribute.multivalue", ".*")))
            login.removeAttributes("jdbcProperties");
        login.getAttribute("jarFile").getPresentation().setType(IPresentable.TYPE_ATTACHMENT);
        ((Html5Presentable) login.getAttribute("jarFile").getPresentation()).getLayoutConstraints().put("accept",
            ".jar");
//        login.getPresentationHelper().change(BeanPresentationHelper.PROP_DESCRIPTION,
//            Environment.translate("jarFile.tooltip", true),
//            "jarFile");
        login.getPresentationHelper().change(BeanPresentationHelper.PROP_NULLABLE, false);
        login.getPresentationHelper().change(BeanPresentationHelper.PROP_NULLABLE, true, "connectionPassword");
        login.getPresentationHelper().change(BeanPresentationHelper.PROP_NULLABLE, true, "replication");
        login.getPresentationHelper().chg("replication", BeanPresentationHelper.PROP_ENABLER, new IActivable() {
            @Override
            public boolean isActive() {
                return Environment.get("use.database.replication", false);
            }
        });

//        ((Map)login.getPresentable().getLayoutConstraints()).put("style", "opacity: 0.9;");
        login.addAction(new SecureAction<Object>("tsl2nano.login.ok") {
            //TODO: ref. to persistence class
            @Override
            public Object action() throws Exception {
                persistence.save();

                //define a new classloader to access all beans of given jar-file
                PersistenceClassLoader runtimeClassloader = new PersistenceClassLoader(new URL[0],
                    Thread.currentThread().getContextClassLoader());
                runtimeClassloader.addLibraryPath(Environment.getConfigPath());
                Thread.currentThread().setContextClassLoader(runtimeClassloader);
                Environment.addService(ClassLoader.class, runtimeClassloader);

                createAuthorization(persistence);

                //load all beans from selected jar-file and provide them in a beancontainer
                List<Class> beanClasses = createBeanContainer(persistence, runtimeClassloader);

                //create navigation model holding all bean types on first page after login
                return createBeanCollectors(beanClasses);
            }

            @Override
            public String getImagePath() {
                return "icons/open.png";
            }

            @Override
            public boolean isDefault() {
                return true;
            }
        });
        return login;
    }

    /**
     * createSubject
     * 
     * @param persistence
     * @return
     */
    private void createAuthorization(final Persistence persistence) {
        String userName = persistence.getConnectionUserName();
        Environment.addService(IAuthorization.class, Authorization.create(userName, false));
    }

    /**
     * after the creation of a new classloader and a beancontainer providing access to the new loaded bean types, this
     * method creates entries for all bean-types to the navigation stack. overwrite this method to define own
     * bean-collector handling.
     * 
     * @param beanClasses new loaded bean types
     * @return a root bean-collector holding all bean-type collectors.
     */
    @SuppressWarnings("serial")
    protected BeanDefinition<?> createBeanCollectors(List<Class> beanClasses) {
        LOG.debug("creating collector for: ");
        List types = new ArrayList(beanClasses.size());
        for (Class cls : beanClasses) {
            LOG.debug("creating collector for: " + cls);
            BeanCollector collector = BeanCollector.getBeanCollector(cls, null, MODE_EDITABLE | MODE_CREATABLE
                | MODE_MULTISELECTION
                | MODE_SEARCHABLE, null);
//            collector.setPresentationHelper(new Html5Presentation(collector));
            types.add(collector);
        }
        /*
         * Load virtual BeanCollectors like QueryResult from directory.
         * name-convention: beandef/virtual/*.xml
         */
        types.addAll(BeanDefinition.loadVirtualDefinitions());

        /*
         * Perhaps show the script tool to do direct sql or ant
         */
        if (Environment.get("application.show.scripttool", false)) {
            BeanConfigurator.defineAction(null);
            final ScriptTool tool = new ScriptTool();
            Bean beanTool = Bean.getBean(tool);
            beanTool.setAttributeFilter("sourceFile", "selectedAction", "text"/*, "result"*/);
            beanTool.getAttribute("text").getPresentation().setType(IPresentable.TYPE_INPUT_MULTILINE);
            beanTool.getAttribute("text").getConstraint().setLength(100000);
            beanTool.getAttribute("text").getConstraint().setFormat(null/*RegExpFormat.createLengthRegExp(0, 100000, 0)*/);
//            beanTool.getAttribute("result").getPresentation().setType(IPresentable.TYPE_TABLE);
            beanTool.getAttribute("sourceFile").getPresentation().setType(IPresentable.TYPE_ATTACHMENT);
            beanTool.getAttribute("selectedAction").setRange(tool.availableActions());
            beanTool.addAction(tool.runner());
            
            String id = "scripttool.define.query";
            String lbl = Environment.translate(id, true);
            IAction queryDefiner = new CommonAction(id, lbl, lbl) {
                @Override
                public Object action() throws Exception {
                    String name = tool.getSourceFile().toLowerCase();
                    Query query = new Query(name, tool.getText(), tool.getSelectedAction().getId().equals("scripttool.sql.id"), null);
                    Environment.get(QueryPool.class).add(query.getName(), query);
                    QueryResult qr = new QueryResult(query.getName());
                    qr.setName(BeanDefinition.PREFIX_VIRTUAL + query.getName());
                    qr.saveDefinition();
                    return "New created specification-query: " + name;
                }
                @Override
                public String getImagePath() {
                    return "icons/save.png";
                }
            };
            beanTool.addAction(queryDefiner);
            types.add(beanTool);
        }
        BeanCollector root = new BeanCollector(BeanCollector.class, types, MODE_EDITABLE | MODE_SEARCHABLE, null);
        root.setName(StringUtil.toFirstUpper(StringUtil
            .substring(Persistence.current().getJarFile(), "/", ".jar", true)));
        root.setAttributeFilter("name");
        root.getAttribute("name").setFormat(new Format() {
            /** serialVersionUID */
            private static final long serialVersionUID = 1725704131355509738L;

            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                String name = StringUtil.substring((String) obj, null, BeanCollector.POSTFIX_COLLECTOR);
                toAppendTo.append(Environment.translate(name, true));
                pos.setEndIndex(1);
                return toAppendTo;
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });
        //perhaps, create the first environment.xml
        if (!Environment.isPersisted()) {
            Environment.persist();
            BeanDefinition.dump();
        }
        return root;
    }

    /**
     * createBeanContainer
     * 
     * @param persistence
     * @param runtimeClassloader
     * @return
     */
    protected List<Class> createBeanContainer(final Persistence persistence, PersistenceClassLoader runtimeClassloader) {
        /*
         * If a external jar-file was selected (-->absolute path), it will be copied
         * If a relative jar-file-path is given, but the file doesn't exist, it will be generated
         * On any circumstances: the jar-file has to be in the environments directory,
         *    the persistence-units jar-file is always environment-dir + jar-filename --> found always through current classpath
         */
        File selectedFile = new File(persistence.getJarFile());
        String jarFile =
            !selectedFile.isAbsolute() ? Environment.getConfigPath() + persistence.getJarFile()
                : persistence.getJarFile();
        if (!new File(jarFile).exists() && !selectedFile.isAbsolute()) {
            //TODO: show generation message before - get script exception from exception handler
            generateJarFile(jarFile);
            if (!new File(jarFile).exists()) {
                throw new ManagedException("Couldn't generate bean jar file '" + jarFile
                    + "' through script hibtools.xml! Please see log file for exceptions.");
            }
        } else if (selectedFile.isAbsolute()) {//copy it into the own classpath (to don't lock the file)
            if (!selectedFile.exists())
                throw new IllegalArgumentException(
                    "If an absolute file-path is given, the file has to exist! If the file-path is relative and doesn't exist, it will be created/generated");
//            if (!new File(envFile).exists())
            FileUtil.copy(selectedFile.getPath(), persistence.jarFileInEnvironment());
        }

        if (Environment.get("use.applicationserver", false)) {
            ServiceFactory.createInstance(runtimeClassloader);
            //a service has to load user object, roles and features project specific!
            BeanClass authentication =
                BeanClass.createBeanClass(Environment.get("applicationserver.authentification.service",
                    Environment.getApplicationMainPackage() + ".service.remote.IUserService"));
            Object authService = ServiceFactory.instance().getService(authentication.getClazz());
            BeanClass.call(authService, Environment.get("applicationserver.authentification.method", "login"),
                new Class[] {
                    String.class, String.class }, persistence.getConnectionUserName(),
                persistence.getConnectionPassword());
//            ServiceFactory.instance().createSession(userObject, mandatorObject, subject, userRoles, features, featureInterfacePrefix)
            BeanContainerUtil.initGenericServices(runtimeClassloader);
        } else {
            GenericLocalBeanContainer.initLocalContainer(runtimeClassloader,
                Environment.get("check.connection.on.login", true));
        }
        Environment.addService(IBeanContainer.class, BeanContainer.instance());

        List<Class> beanClasses =
            runtimeClassloader.loadBeanClasses(jarFile,
                Environment.get("bean.class.presentation.regexp", ".*"), null);
        Environment.setProperty("loadedBeanTypes", beanClasses);

        return beanClasses;
    }

    protected static void generateJarFile(String jarFile) {
        /** ant script to start the hibernatetool 'hbm2java' */
        final String HIBTOOLNAME = "hibtool.xml";
        /** hibernate reverse engeneer configuration */
        final String HIBREVNAME = "hibernate.reveng.xml";
        Environment.saveResourceToFileSystem(HIBTOOLNAME);
        Environment.saveResourceToFileSystem(HIBREVNAME);
        Properties properties = new Properties();
        properties.setProperty(HIBREVNAME, Environment.getConfigPath() + HIBREVNAME);
//    properties.setProperty("hbm.conf.xml", "hibernate.conf.xml");
        properties.setProperty("server.db-config.file", Persistence.FILE_JDBC_PROP_FILE);
        properties.setProperty("dest.file", jarFile);

        String plugin_dir = System.getProperty("user.dir");
        properties.setProperty("plugin.dir", new File(plugin_dir).getAbsolutePath());
        if (plugin_dir.endsWith(".jar/")) {
            properties.setProperty("plugin_isjar", Boolean.toString(true));
        }
        Environment.get(CompatibilityLayer.class).runRegistered("ant",
            Environment.getConfigPath() + HIBTOOLNAME,
            "create.bean.jar",
            properties);
    }

    protected void reset() {
        String configPath = Environment.get(Environment.KEY_CONFIG_PATH, "config");

        sessions.clear();
        Environment.reset();
        Environment.setProperty(Environment.KEY_CONFIG_PATH, configPath);
        Environment.setProperty("http.serviceURL", serviceURL.toString());
        BeanDefinition.clearCache();
        BeanValue.clearCache();
        Thread.currentThread().setContextClassLoader(appstartClassloader);
        createPageBuilder();
        builder = Environment.get(IPageBuilder.class);
    }

//    /**
//     * createTestNavigationModel
//     * 
//     * @return navigation model for testing purpose
//     */
//    static Stack<BeanDefinition<?>> createTestNavigationModel() {
//
//        BeanContainer.initEmtpyServiceActions();
//
//        TypeBean b = new TypeBean();
//        b.setDate(new Date());
//        b.setString("test");
//        b.setBigDecimal(new BigDecimal(10));
//        Bean<TypeBean> bean = new Bean<TypeBean>();
//        final Collection<TypeBean> rootBeanList = new ArrayList(Arrays.asList(b));
//        BeanCollector<Collection<TypeBean>, TypeBean> root = new BeanCollector<Collection<TypeBean>, TypeBean>(TypeBean.class,
//            true,
//            true,
//            false);
//        BeanFinder<TypeBean, Object> beanFinder = new BeanFinder<TypeBean, Object>(TypeBean.class) {
//            @Override
//            public Collection<TypeBean> getData(Object fromFilter, Object toFilter) {
//                return rootBeanList;
//            }
//        };
//        beanFinder.setDetailBean(bean);
//        root.setBeanFinder(beanFinder);
//        final Bean<Object> model = new Bean<Object>();
//        model.addAttribute("Name", "Stefan", RegularExpressionFormat.createAlphaNumRegExp(15, true), null, null);
//        model.addAttribute("Kategorie", 1, null, null, null)
//            .setRange(Arrays.asList(1, 2, 3))
//            .setBasicDef(3, false, null, null, "Kategorie");
//        model.addAction(new CommonAction<Object>("testid", "+xxx", null) {
//            @Override
//            public Object action() throws Exception {
//                String newValue = model.getValue("Name") + "xxx";
//                model.setValue("Name", newValue);
//                return model;
//            }
//        });
////        model.addDefaultSaveAction();
//
//        Bean appStart = new Bean();
//        appStart.setName(START_PAGE);
//
//        Stack<BeanDefinition<?>> navigationModel = new Stack<BeanDefinition<?>>();
//        navigationModel.push(appStart);
//        navigationModel.push(root);
//        navigationModel.push(model);
//        return navigationModel;
//    }
}
