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

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.IBeanContainer;
import de.tsl2.nano.bean.def.AbstractExpression;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.core.AppLoader;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.execution.CompatibilityLayer;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.SystemUtil;
import de.tsl2.nano.h5.expression.RuleExpression;
import de.tsl2.nano.h5.expression.SQLExpression;
import de.tsl2.nano.h5.navigation.EntityBrowser;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.h5.navigation.Workflow;
import de.tsl2.nano.h5.websocket.NanoWebSocketServer;
import de.tsl2.nano.persistence.GenericLocalBeanContainer;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.persistence.PersistenceClassLoader;
import de.tsl2.nano.persistence.provider.NanoEntityManagerFactory;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.util.NumberUtil;

/**
 * An Application of subclassing NanoHTTPD to make a custom HTTP server.
 * 
 * <pre * TODO: * - Bean-->
 * BeanValue-->getColumnDefinition() --> Table(columns)
 * - PageBuilder --> Bean.Presentable
 * - Navigation
 * - Verbindung/Abgrenzung BeanContainer
 * - evtl mini-browser wie lynx einbinden
 * - BeanContainer mit cache fuer tests
 * </pre>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class NanoH5 extends NanoHTTPD implements ISystemConnector<Persistence> {
    private static final Log LOG = LogFactory.getLog(NanoH5.class);

    public static final String JAR_COMMON = "tsl2.nano.common.jar";
    public static final String JAR_SERVICEACCESS = "tsl2.nano.serviceaccess.jar";
    public static final String JAR_DIRECTACCESS = "tsl2.nano.directaccess.jar";
    public static final String JAR_INCUBATION = "tsl2.nano.incubation.jar";
    public static final String JAR_SAMPLE = "tsl2.nano.sample.jar";
    public static final String JAR_RESOURCES = "tsl2.nano.resources.jar";
    public static final String JAR_SIMPLEXML = "tsl2.nano.simple-xml.jar";

    Map<InetAddress, NanoH5Session> sessions;

    IPageBuilder<?, String> builder;
    URL serviceURL;
    ClassLoader appstartClassloader;

    /** workaround to avoid re-serving a cached request. */
    private Properties lastHeader;

    private static final String DEBUG_HTML_FILE = AppLoader.getFileSystemPrefix() + "application.html";
    static final String START_PAGE = "Start";
    static final int OFFSET_FILTERLINES = 2;

    private static final String BEAN_GENERATION_PACKAGENAME = "bean.generation.packagename";

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

            Environment.extractResourceToDir("run.bat", "../");
            Environment.extractResource("shell.xml");
            Environment.extractResource("mda.bat");
            Environment.extractResource("mda.xml");
            Environment.extractResource("beandef.xsd");
            Environment.extractResource("tsl2nano-appcache.mf");
            Environment.extractResourceToDir("favicon.ico", "../");
            String dir = Environment.getConfigPath();
            File icons = new File(dir + "icons");
            if (!icons.exists()) {
                try {
                    FileUtil.extractNestedZip("tsl2.nano.h5.default-resources.jar", dir, null);
                } catch (Exception ex) {
                    //this shouldn't influence the application start!
                    LOG.warn("couldn't extract resources from internal file " + "tsl2.nano.h5.default-resources.jar",
                        ex);
                }
                /*
                 * on first start, extract the sample files
                 */
                ((Html5Presentation) Environment.get(BeanPresentationHelper.class)).createSampleEnvironment();
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
        return createStartPage(DEBUG_HTML_FILE);
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
        String page =
            Html5Presentation.createMessagePage("start.template", Environment.translate("tsl2nano.start", true) + " "
                + Environment.getName(), serviceURL);
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

        long startTime = System.currentTimeMillis();
        InetAddress requestor = ((Socket) header.get("socket")).getInetAddress();
        NanoH5Session session = sessions.get(requestor);
        if (session == null) {
            //on a new session, no parameter should be set
            session = createSession(requestor);
        } else {//perhaps session was interrupted/closed but not removed
            //WORKAROUND: may occur on cached pages
            if (method.equals("GET") && parms.size() == 0 && (uri.length() < 2 || header.get("referer") == null)) {
                LOG.debug("reloading cached page...");
                return session.response;
            } else if (session.nav == null || session.nav.isEmpty()) {
                session.close();
                sessions.remove(session.inetAddress);
                session = createSession(requestor);
            }
        }
        session.startTime = startTime;
        //workaround to avoid doing a cached request twice
        lastHeader = header;

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
        try {
            NanoH5Session session = new NanoH5Session(this,
                inetAddress,
                createGenericNavigationModel(),
                Environment.get(ClassLoader.class), null, createSesionContext());
            sessions.put(inetAddress, session);
            return session;
        } catch (Throwable e) {
            //to avoid an application halt without error message, we catch all to re-throw and log.
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * createSesionContext
     * 
     * @return data specific context name
     */
    private String createSesionContext() {
        Persistence p = Persistence.current();
        return p.getDatabase() + "." + p.getDefaultSchema() + "." + p.getJarFile();
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
        ISystemConnector conn = Environment.get(ISystemConnector.class);
        if (conn == null)
            conn = Environment.addService(ISystemConnector.class, this);

        Bean login = Bean.getBean(conn.createConnectionInfo());

        Workflow workflow = Environment.get(Workflow.class);

        if (workflow == null || workflow.isEmpty()) {
            LOG.debug("creating navigation stack");
            Stack<BeanDefinition<?>> navigationModel = new Stack<BeanDefinition<?>>();
            Bean<String> startPage = Bean.getBean(START_PAGE);
            navigationModel.push(startPage);

            //perhaps, use META-INF/persistence.xml directly without user input
            if (Environment.get("use.gui.login", true)) {
                navigationModel.push(login);
            } else {
                navigationModel.push(connect((Persistence) login.getInstance()));
            }
            return new EntityBrowser(navigationModel);
        } else {
            //create a copy for the new session
            try {
                workflow = workflow.clone();
            } catch (CloneNotSupportedException e) {
                ManagedException.forward(e);
            }
            if (Environment.get("use.gui.login", true)) {
                workflow.setLogin(login);
            } else {
                workflow.add(connect((Persistence) login.getInstance()));
            }
            return workflow;
        }
    }

    @Override
    public Persistence createConnectionInfo() {
        return createPersistenceUnit().getInstance();
    }

    private Bean<Persistence> createPersistenceUnit() {
        final Persistence persistence = Persistence.current();
        final Bean<Persistence> login = PersistenceUI.createPersistenceUI(persistence, this);
        return login;
    }

    @Override
    public BeanDefinition<?> connect(Persistence persistence) {
        //define a new classloader to access all beans of given jar-file
        PersistenceClassLoader runtimeClassloader = new PersistenceClassLoader(new URL[0],
            rootClassloader());
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
    public void disconnect(Persistence connectionEnd) {
        //nothing to clean
    }

    protected ClassLoader rootClassloader() {
        return appstartClassloader;
//        try {
        //a cast to NestedJarClassloader is not possible - so we do it with reflection
//            return (ClassLoader) ((NestedJarClassLoader) appstartClassloader).clone();
//            return (ClassLoader) BeanClass.call(appstartClassloader, "clone");
//        } catch (CloneNotSupportedException e) {
//            ManagedException.forward(e);
//            return null;
//        }
    }

    /**
     * createSubject
     * 
     * @param persistence
     * @return
     */
    private void createAuthorization(final Persistence persistence) {
        String userName = persistence.getConnectionUserName();
        Message.send("creating authorization for " + userName);
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
        Message.send("loading bean collectors for " + beanClasses.size() + " types");
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

        BeanCollector root = new BeanCollector(BeanCollector.class, types, MODE_EDITABLE | MODE_SEARCHABLE, null);
        root.setName(StringUtil.toFirstUpper(StringUtil
            .substring(Persistence.current().getJarFile().replace("\\", "/"), "/", ".jar", true)));
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
        Environment.setAutopersist(true);
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
        Message.send("creating bean-container for " + persistence.getJarFile());
        /*
         * If a external jar-file was selected (-->absolute path), it will be copied
         * If a relative jar-file-path is given, but the file doesn't exist, it will be generated
         * On any circumstances: the jar-file has to be in the environments directory,
         *    the persistence-units jar-file is always environment-dir + jar-filename --> found always through current classpath
         */
        /*
         * we have to check, if it is an URL, an absolute file path or a relative file path.
         * 1. URL: extract file path from URL
         * 2. absolute file path: copy the file to environment location
         * 3. relative: perfect ;-)
         */
        String jarName = persistence.getJarFile();
        File selectedFile = new File(jarName);
        boolean isAbsolutePath = selectedFile.isAbsolute();
        /* 
         * new: absolute is not possible if sent as attachment from browser-client.
         * first we look for an attachment inside the temp dir, then look at the
         * standard environment path.
         */
        if (!isAbsolutePath) {
            String attachmentJarName = NanoWebSocketServer.getAttachmentFilename(persistence, "jarFile", jarName);
            File attFile = new File(attachmentJarName);
            if (attFile.canRead()) {
                FileUtil.copy(attFile.getPath(), persistence.jarFileInEnvironment());
            }
            selectedFile =
                new File(Environment.getConfigPath()
                    + FileUtil.getURIFile(jarName).getPath());
        }
        jarName = selectedFile.getPath();
        if (!selectedFile.exists() && !isAbsolutePath) {
            //ant-scripts can't use the nested jars. but normal beans shouldn't have dependencies to simple-xml.
            Environment.extractResource(JAR_SIMPLEXML);
            Environment.extractResource(JAR_COMMON);
            Environment.extractResource(JAR_DIRECTACCESS);
            Environment.extractResource(JAR_SERVICEACCESS);

            String generatorTask;
            if (persistence.getGenerator().equals(Persistence.GEN_HIBERNATE)) {
                generatorTask = "org.hibernate.tool.ant.HibernateToolTask";
            } else {
                generatorTask = "org.apache.openjpa.jdbc.ant.ReverseMappingToolTask";
            }
            Environment.loadClassDependencies("org.apache.tools.ant.taskdefs.Taskdef",
                generatorTask, persistence.getConnectionDriverClass());

            if (isNewDatabase(persistence)) {
                generateDatabase(persistence);
            }
            //TODO: show generation message before - get script exception from exception handler
            generateJarFile(jarName, persistence.getGenerator(), persistence.getDefaultSchema());

            if (!new File(jarName).exists()) {
                throw new ManagedException(
                    "Couldn't generate bean jar file '"
                        + jarName
                        + "' through ant-script 'reverse-eng.xml'! Please see log file for exceptions. Possible errors are:\n"
                        + "\t- no ant jar files (ant.jar, ant-launcher.jar, ant-nodeps.jar) in your environment directory\n"
                        + "\t- no hibernate or open-jpa jar files in your environment directory\n"
                        + "\t- no jdbc-driver-jar file for the given 'ConnectionDriverClass' in your environment directory\n"
                        + "\t- your java is an JRE instead of a full JDK (needed to compile the generated classes!)\n"
                        + "\nAs alternative you may select an existing bean-jar file (-->no generation needed!) in field \"JarFile\"");
            }
        } else if (isAbsolutePath) {//copy it into the own classpath (to don't lock the file)
            if (!selectedFile.exists())
                throw new IllegalArgumentException(
                    "If an absolute file-path is given, the file has to exist! If the file-path is relative and doesn't exist, it will be created/generated");
//            if (!new File(envFile).exists())
            FileUtil.copy(selectedFile.getPath(), persistence.jarFileInEnvironment());
        }

        boolean useJPAPersistenceProvider = true;
        /* 
         * check, whether a real/existing jpa-persistence-provider was selected.
         * perhaps provide directly an EntityManager
         */
        if (!Environment.get("use.applicationserver", false)) {
//            Environment.loadDependencies(persistence.getProvider());
            Class[] provider = Environment.get(CompatibilityLayer.class).load(persistence.getProvider());
            if (NanoEntityManagerFactory.AbstractEntityManager.class.isAssignableFrom(provider[0])) {
                useJPAPersistenceProvider = false;
                //ok, here we use again the class as string ;-(
                Environment.addService(
                    EntityManager.class,
                    NanoEntityManagerFactory.instance().createEntityManager(persistence.getProvider(),
                        persistence.getJdbcProperties()));
            }
        }

        if (Environment.get("use.applicationserver", false)) {
            ServiceFactory.createInstance(runtimeClassloader);
            //a service has to load user object, roles and features project specific!
            BeanClass authentication =
                BeanClass.createBeanClass(Environment.get("applicationserver.authentication.service",
                    Environment.getApplicationMainPackage() + ".service.remote.IUserService"));
            Object authService = ServiceFactory.instance().getService(authentication.getClazz());
            BeanClass.call(authService, Environment.get("applicationserver.authentication.method", "login"),
                new Class[] {
                    String.class, String.class }, persistence.getConnectionUserName(),
                persistence.getConnectionPassword());
//            ServiceFactory.instance().createSession(userObject, mandatorObject, subject, userRoles, features, featureInterfacePrefix)
            BeanContainerUtil.initGenericServices(runtimeClassloader);
        } else {
            Environment.loadClassDependencies(persistence.getConnectionDriverClass(),
                persistence.getDatasourceClass(), persistence.getProvider());
            GenericLocalBeanContainer.initLocalContainer(runtimeClassloader,
                useJPAPersistenceProvider && Environment.get("connection.check.on.login", false));
        }
        Environment.addService(IBeanContainer.class, BeanContainer.instance());

        List<Class> beanClasses =
            runtimeClassloader.loadBeanClasses(jarName,
                Environment.get("bean.class.presentation.regexp", ".*"), null);
        Environment.setProperty("loadedBeanTypes", beanClasses);

        return beanClasses;
    }

    private boolean isNewDatabase(Persistence persistence) {
        if (!Util.isEmpty(persistence.getPort())) {
            int p = Integer.valueOf(persistence.getPort());
            //TODO: eval if url on localhost
            String url = persistence.getConnectionUrl();
            return persistence.getConnectionDriverClass().equals(Persistence.STD_LOCAL_DATABASE_DRIVER)
                && (url.contains("localhost") || url.contains("127.0.0.1"))
                && !NetUtil.isOpen(NetUtil.getInetAddress(), p);
        }
        return false;
    }

    private void generateDatabase(Persistence persistence) {
        Message.send("creating new database " + persistence.getDatabase() + " for url "
            + persistence.getConnectionUrl());
        Environment.get(CompatibilityLayer.class).runRegistered("ant",
            Environment.getConfigPath() + "mda.xml",
            "do.all",
            new Properties(), null);
    }

    protected static void generateJarFile(String jarFile, String generator, String schema) {
        /** ant script to start the hibernatetool 'hbm2java' */
        final String REVERSE_ENG_SCRIPT = "reverse-eng.xml";
        /** hibernate reverse engeneer configuration */
        final String HIBREVNAME = "hibernate.reveng.xml";
        Environment.extractResource(REVERSE_ENG_SCRIPT);
        Environment.extractResource(HIBREVNAME);
        Properties properties = new Properties();
        properties.setProperty(HIBREVNAME, Environment.getConfigPath() + HIBREVNAME);
//    properties.setProperty("hbm.conf.xml", "hibernate.conf.xml");
        properties.setProperty("server.db-config.file", Persistence.FILE_JDBC_PROP_FILE);
        properties.setProperty("dest.file", jarFile);
        properties.setProperty("generator", generator);
        properties.setProperty("schema", schema);
        properties.setProperty(BEAN_GENERATION_PACKAGENAME,
            Environment.get(BEAN_GENERATION_PACKAGENAME, "org.anonymous.project"));
        String plugin_dir = System.getProperty("user.dir");
        properties.setProperty("plugin.dir", new File(plugin_dir).getAbsolutePath());
        if (plugin_dir.endsWith(".jar/")) {
            properties.setProperty("plugin_isjar", Boolean.toString(true));
        }
        Message.send("starting generation of '" + jarFile + "' through script " + REVERSE_ENG_SCRIPT);
        //If no environment was saved before, we should do it now!
        Environment.persist();

        Environment.get(CompatibilityLayer.class).runRegistered("ant",
            Environment.getConfigPath() + REVERSE_ENG_SCRIPT,
            "create.bean.jar",
            properties, null);
    }

    protected void reset() {
        String configPath = Environment.get(Environment.KEY_CONFIG_PATH, "config");

        sessions.clear();
        Environment.reset();
        Environment.setProperty(Environment.KEY_CONFIG_PATH, configPath);
        Environment.setProperty("http.serviceURL", serviceURL.toString());
        Bean.clearCache();
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
