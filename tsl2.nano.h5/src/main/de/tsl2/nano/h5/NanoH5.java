package de.tsl2.nano.h5;

import static de.tsl2.nano.bean.def.IBeanCollector.MODE_CREATABLE;
import static de.tsl2.nano.bean.def.IBeanCollector.MODE_EDITABLE;
import static de.tsl2.nano.bean.def.IBeanCollector.MODE_MULTISELECTION;
import static de.tsl2.nano.bean.def.IBeanCollector.MODE_SEARCHABLE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.IBeanContainer;
import de.tsl2.nano.bean.def.AbstractExpression;
import de.tsl2.nano.bean.def.Attachment;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.PathExpression;
import de.tsl2.nano.collection.ExpiringMap;
import de.tsl2.nano.core.AppLoader;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.classloader.NetworkClassLoader;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.execution.CompatibilityLayer;
import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.AntRunner;
import de.tsl2.nano.h5.NanoHTTPD.Response.Status;
import de.tsl2.nano.h5.expression.QueryPool;
import de.tsl2.nano.h5.expression.URLExpression;
import de.tsl2.nano.h5.expression.RuleExpression;
import de.tsl2.nano.h5.expression.SQLExpression;
import de.tsl2.nano.h5.expression.SimpleExpression;
import de.tsl2.nano.h5.navigation.EntityBrowser;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.h5.navigation.Workflow;
import de.tsl2.nano.incubation.specification.actions.ActionPool;
import de.tsl2.nano.incubation.specification.rules.RulePool;
import de.tsl2.nano.core.messaging.EventController;
import de.tsl2.nano.persistence.GenericLocalBeanContainer;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.persistence.PersistenceClassLoader;
import de.tsl2.nano.persistence.provider.NanoEntityManagerFactory;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.util.SchedulerUtil;
import de.tsl2.nano.util.Translator;

/**
 * An Application of subclassing NanoHTTPD to make a custom HTTP server.
 * 
 * <pre>
 * TODO: 
 * - Bean-->
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
    public static final String JAR_SAMPLE = "tsl2.nano.h5.sample.jar";
    public static final String JAR_RESOURCES = "tsl2.nano.h5.default-resources.jar";
    public static final String JAR_SIMPLEXML = "tsl2.nano.simple-xml.jar";

    public static final String ZIP_STANDALONE = "standalone.zip";

    /** ant script to start the hibernatetool 'hbm2java' */
    static final String REVERSE_ENG_SCRIPT = "reverse-eng.xml";
    /** hibernate reverse engeneer configuration */
    static final String HIBREVNAME = "hibernate.reveng.xml";
    static final String HIBREVNAME_TEMPLATE = "hibernate.reveng.tml";

    /**
     * if nano was packaged as standalone jar, the o/r-mapper, hsqldb and mysqlconnector are stored inside this
     * directory to be extraced into the environments directory. the standalone path inside the jar is not part of the
     * classpath!
     */
    static final String LIBS_STANDALONE = "standalone/";

    Map<InetAddress, NanoH5Session> sessions;
    long requests = 0;
    IPageBuilder<?, String> builder;
    URL serviceURL;
    ClassLoader appstartClassloader;

    private EventController eventController;

//    /** workaround to avoid re-serving a cached request. */
//    private Properties lastHeader;

    private static final String START_HTML_FILE = /*AppLoader.getFileSystemPrefix() +*/"application.html";
    static final String START_PAGE = "Start";
    static final int OFFSET_FILTERLINES = 2;

    private static final String BEAN_GENERATION_PACKAGENAME = "bean.generation.packagename";

    public NanoH5() throws IOException {
        this(ENV.get("service.url", "http://localhost:8067"), ENV.get(IPageBuilder.class));
    }

    public NanoH5(String serviceURL, IPageBuilder<?, String> builder) throws IOException {
//        super(null, getPort(serviceURL), new File(ENV.getConfigPath()), !LOG.isDebugEnabled());
        super(getPort(serviceURL), new File(ENV.getConfigPath()));
        this.serviceURL = getServiceURL(serviceURL);
        this.builder = builder != null ? builder : createPageBuilder();
        ENV.registerBundle(NanoH5.class.getPackage().getName() + ".messages", true);
        appstartClassloader = Thread.currentThread().getContextClassLoader();
        ENV.addService(ClassLoader.class, appstartClassloader);
        eventController = new EventController();
        sessions = Collections.synchronizedMap(
            new ExpiringMap<InetAddress, NanoH5Session>(ENV.get("session.timeout.millis", 12 * DateUtil.T_HOUR)));
        //thought, the expression extensions would register themself - but it's not working
        AbstractExpression.registerExpression(PathExpression.class);
        AbstractExpression.registerExpression(RuleExpression.class);
        AbstractExpression.registerExpression(SQLExpression.class);
        AbstractExpression.registerExpression(URLExpression.class);
        AbstractExpression.registerExpression(SimpleExpression.class);
    }

    /**
     * main
     * 
     * @param args
     */
    public static void main(String[] args) {
        startApplication(NanoH5.class, MapUtil.asMap(0, "service.port"), args);
    }

    /**
     * starts application and shows initial html page
     */
    @Override
    public void start() {
        //print errors directly
        Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
            }
        });
        try {
//            LogFactory.setLogLevel(LogFactory.LOG_ALL);
                
            LOG.debug(System.getProperties());
            createStartPage();

            try {
                if (AppLoader.isUnixFS()) {
                    ENV.extractResourceToDir("run.sh", "../", false, true, true);
                    ENV.extractResourceToDir("runasservice.sh", "../", false, true, true);
                    ENV.extractResource("mda.sh", true, true);
                } else {
                    ENV.extractResourceToDir("run.bat", "../", false, false, true);
                    ENV.extractResource("mda.bat");
                }
                ENV.extractResource("readme.txt");
                ENV.extractResource("shell.xml");
                ENV.extractResource("mda.xml");
                ENV.extractResource("tsl2nano-appcache.mf");
                ENV.extractResource("favicon.ico");
                
                ENV.extractResource("doc/beanconfigurator.help.html");
                ENV.extractResource("doc/attributeconfigurator.help.html");
                ENV.extractResource("doc/entry.help.html");
                ENV.extractResource("doc/persistence.help.html");
                onStandaloneExtractJars();
            } catch (Exception ex) {
                LOG.error("couldn't extract ant or shell script", ex);
                ENV.get(UncaughtExceptionHandler.class).uncaughtException(null, ex);
            }
            String dir = ENV.getConfigPath();
            File icons = new File(dir + "icons");
            if (!icons.exists()) {
                try {
                    FileUtil.extractNestedZip(JAR_RESOURCES, dir, null);
                } catch (Exception ex) {
                    //this shouldn't influence the application start!
                    LOG.warn("couldn't extract resources from internal file " + JAR_RESOURCES,
                        ex);
                    try {
                        ENV.extractResource("icons/**");
                    } catch (Exception ex1) {
                        LOG.warn("couldn't extract resources from icons directory",
                            ex1);
                        ENV.get(UncaughtExceptionHandler.class).uncaughtException(null, ex);
                    }
                }
//                if (AppLoader.isDalvik()) {//on android, we cannot yet create the beans-jar-file
//                    try {
//                        ENV.extractResource("anyway.jar");
//                    } catch (Exception e1) {
//                        LOG.warn("couldn't extract resources from internal file " + "anyway.jar",
//                            e1);
//                    }
//                }
                /*
                 * DEPRECATED: we integrate the 'anyway' database as sample
                 * on first start, extract the sample files
                 */
                if (ENV.get("app.create.sample.files.on.first.start", false))
                    ((Html5Presentation) ENV.get(BeanPresentationHelper.class)).createSampleEnvironment();
            }
            //perhaps activate secure transport layer TLS
            if (ENV.get("app.ssl.activate", false)) {
                String keyStore = ENV.get("app.ssl.keystore.file", "nanoh5.jks");
                LOG.info("activating ssl using keystore " + keyStore);
                makeSecure(NanoHTTPD.makeSSLSocketFactory(keyStore,
                    ENV.get("app.ssl.keystore.password", "nanoh5").toCharArray()), null);
            } else {
                ENV.setProperty("app.ssl.shortcut", "");
            }
            super.start();

            if (System.getProperty("os.name").startsWith("Windows")) {
                SystemUtil.executeRegisteredWindowsPrg(applicationHtmlFile());
            } else {
                LOG.info("Please open the URL '" + serviceURL.toString() + "' in your browser");
            }

//            myOut = LogFactory.getOut();
//            myErr = LogFactory.getErr();

            try {
                LOG.info("Listening on port " + serviceURL.getPort() + ". Hit Enter or Strg+C to stop.\n");
                LOG.debug("waiting for input on " + System.in);
                System.in.read();
            } catch (Exception ex) {
                LOG.debug("server mode without input console available (message: " + ex.toString() + ")");
                while (true) {
                    ConcurrentUtil.sleep(3000);
                }
            }
        } catch (Exception ioe) {
            LOG.error("Couldn't start server: ", ioe);
            ConcurrentUtil.sleep(3000);
            System.exit(-1);
        }
    }

    /**
     * if this jar is a standalone (containing o/r-mapper and a database), extract that jars to be available for ant
     * scripts. for more informations, see {@link #LIBS_STANDALONE}.
     */
    private void onStandaloneExtractJars() {
        final String JAR_JPA_API = "hibernate-jpa-2.1-api-1.0.0.Final.jar";
        if (FileUtil.hasResource(ZIP_STANDALONE) && !new File(ENV.getConfigPath() + JAR_JPA_API).exists()) {
            LOG.info("extracting " + ZIP_STANDALONE + " and " + JAR_JPA_API);
            ENV.extractResource(JAR_JPA_API, true, false);
            FileUtil.extractNestedZip(ZIP_STANDALONE, ENV.getConfigPath(), null);
            //wait, until the ant jars are stored and loaded
            ConcurrentUtil.startDaemon("regex-replace-run-scripts", new Runnable() {
                @Override
                public void run() {
                    ConcurrentUtil.sleep(3000);
                    try {
                        AntRunner.runRegexReplace("rem (set STANDALONE)", "\\1", System.getProperty("user.dir"),
                            "run.bat");
                        AntRunner.runRegexReplace("[#](STANDALONE)", "\\1", System.getProperty("user.dir"), "run.sh");
                    } catch (Exception e) {
                        //ok, no real problem, but log it...
                        LOG.error("", e);
                    }
                }
            });
        }
    }

    public static URL getServiceURL(String serviceURLString) {
        if (serviceURLString == null) {
            serviceURLString = ENV.get("service.url", "http://localhost:8067");
            return NetUtil.url(serviceURLString);
        }
        //perhaps activate secure transport layer TLS
        if (serviceURLString.startsWith("https"))
            ENV.setProperty("app.ssl.activate", true);
        if (ENV.get("app.ssl.activate", false)) {
            ENV.setProperty("app.ssl.shortcut", "s");
            serviceURLString.replace("http://", "https://");
        } else {
            ENV.setProperty("app.ssl.shortcut", "");
            serviceURLString.replace("https://", "http://");
        }
        if (!serviceURLString.matches(".*[:][0-9]{3,5}")) {
            serviceURLString = "localhost:" + serviceURLString;
        }
        if (!serviceURLString.contains("://")) {
            serviceURLString = "http" + ENV.get("app.ssl.shortcut", "") + "://" + serviceURLString;
        }
        URL serviceURL = null;
        try {
            serviceURL = new URL(serviceURLString);
            ENV.setProperty("service.url", serviceURL.toString());
        } catch (MalformedURLException e) {
            ManagedException.forward(e);
        }
        return serviceURL;
    }

    public static int getPort(String serviceURL) {
        return Integer.valueOf(StringUtil.substring(serviceURL, ":", null, true));
    }

    protected String createStartPage() {
        return createStartPage(applicationHtmlFile());
    }

    /**
     * createStartPage
     * 
     * @param resultHtmlFile
     */
    protected String createStartPage(String resultHtmlFile) {
        InputStream stream = ENV.getResource("start.template");
//        String startPage = String.valueOf(FileUtil.getFileData(stream, null));
//        startPage = StringUtil.insertProperties(startPage,
//            MapUtil.asMap("url", serviceURL, "text", ENV.getName()));
        String page =
            Html5Presentation.createMessagePage("start.template", ENV.translate("tsl2nano.start", true) + " "
                + ENV.translate(ENV.getName(), true), serviceURL);
        FileUtil.writeBytes(page.getBytes(), resultHtmlFile, false);
        return page;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response serve(String uri,
            Method m,
            Map<String, String> header,
            Map<String, String> parms,
            Map<String, String> files) {
        String method = m.name();
        if (method.equals("GET")) {
            // serve files
            if (!NumberUtil.isNumber(uri.substring(1)) && HtmlUtil.isURI(uri)
                && !uri.contains(Html5Presentation.PREFIX_BEANREQUEST)) {
                return super.serve(uri, method, header, parms, files);
            }
        }

        long startTime = System.currentTimeMillis();
        InetAddress requestor = ((Socket) ((Map) header).get("socket")).getInetAddress();
        NanoH5Session session = sessions.get(requestor);
        //if a user reconnects on the same machine, we remove his old session
        if (session != null && session.getUserAuthorization() != null && parms.containsKey("connectionUserName")
            && !parms.get("connectionUserName").equals(session.getUserAuthorization().getUser().toString())) {
            session.close();
            sessions.remove(session.inetAddress);
            session = null;
        }
        if (session == null) {
            //on a new session, no parameter should be set
            session = createSession(requestor);
        } else {//perhaps session was interrupted/closed but not removed
            boolean done = session.nav != null && session.nav.done();
            if (!session.check(ENV.get("session.timeout.millis", 30 * DateUtil.T_MINUTE),
                ENV.get("session.timeout.throwexception", false))) {
                //TODO: show page with 'session expired'
                // session expired!
                if (ENV.get("session.workflow.close", false)) {
                    session.close();
                    sessions.remove(session.inetAddress);
                    session = createSession(requestor);
                }
                if (done) {
                    // the workflow was done, now create the entity browser
                    LOG.info("session-workflow of " + session + " was done. creating an entity-browser now...");
                    session.nav = createGenericNavigationModel(true);
                    //don't lose the connection - the first item is the login
//                    session.nav.next(session.nav.toArray()[1]);
                }
            } else if (session.response != null && method.equals("GET") && parms.size() < 2 /* contains 'QUERY_STRING = null' */
                && (uri.length() < 2 || header.get("referer") == null) || isDoubleClickDelay(session)) {
                LOG.debug("reloading cached page...");
                try {
                    session.response.getData().reset();
                    return new Response(Status.OK, "text/html", session.response.getData(), -1);
                } catch (IOException e) {
                    LOG.error(e);
                }
            }
        }
        requests++;
        session.startTime = startTime;
//        //workaround to avoid doing a cached request twice
//        lastHeader = header;

        return session.serve(uri, method, header, parms, files);
    }

    private boolean isDoubleClickDelay(NanoH5Session session) {
        return System.currentTimeMillis() - ENV.get("app.event.dblclick.delay", 100) < session.getLastAccess();
    }

    public Response createResponse(String msg) {
        return createResponse(Response.Status.OK, MIME_HTML, msg);
    }

    public Response createResponse(Status status, String type, String msg) {
        byte[] bytes = msg.getBytes();
        return new NanoHTTPD.Response(status, type, new ByteArrayInputStream(bytes), bytes.length);
    }

    /**
     * adds services for presentation and page-builder
     */
    protected IPageBuilder<?, String> createPageBuilder() {
        Html5Presentation pageBuilder = new Html5Presentation();
        ENV.addService(BeanPresentationHelper.class, pageBuilder);
        ENV.addService(IPageBuilder.class, pageBuilder);
        return pageBuilder;
    }

    /**
     * initialize navigation model and perhaps bean-definitions. overwrite this method if you create your extending
     * application. this method will be called, before any bean types are loaded!
     */
    protected NanoH5Session createSession(InetAddress inetAddress) {
        LOG.info("creating new session on socket: " + inetAddress);
        try {
            NanoH5Session session = NanoH5Session.createSession(this,
                inetAddress,
                createGenericNavigationModel(),
                Thread.currentThread().getContextClassLoader(), ConcurrentUtil.getCurrent(Authorization.class),
                createSessionContext());
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
    private Map createSessionContext() {
//        Persistence p = Persistence.current();
        Map context = new HashMap();
//        context.put("session.name", p.getDatabase() + "." + p.getDefaultSchema() + "." + p.getJarFile());
        return context;
    }

    protected IBeanNavigator createGenericNavigationModel() {
        return createGenericNavigationModel(false);
    }

    /**
     * reads all classes of 'beanjar', creates a root beancollector holding a collection of this classes.
     * 
     * @param beanjar jar to resolve the desired entities from
     * @return navigation stack holding a beancollector for all entity classes inside beanjar
     */
    protected IBeanNavigator createGenericNavigationModel(boolean workflowDone) {

        BeanContainer.initEmtpyServiceActions();
        /*
         * create the presentable navigation stack
         */
        ISystemConnector conn = ENV.get(ISystemConnector.class);
        if (conn == null) {
            conn = ENV.addService(ISystemConnector.class, this);
        }

        Bean login = Bean.getBean(conn.createConnectionInfo());

        //on webstart, the incubation jar (Workflow-->Parameter-->ComparableMap) is not present
        if (!ENV.get(CompatibilityLayer.class).isAvailable("de.tsl2.nano.incubation.vnet.workflow.ComparableMap")) {
            ENV.extractResource(JAR_INCUBATION);
            //ENV holds an old class reference, so we use the threads context classloader
//            ((NetworkClassLoader)Thread.currentThread().getContextClassLoader()).addFile(ENV.getConfigPath() + System.getProperty(JAR_INCUBATION));
            //wait until the classloader found the new jar file
            ConcurrentUtil.sleep(2000);
        }

        Workflow workflow = ENV.get(Workflow.class);

        if (workflow == null || workflow.isEmpty() || workflowDone) {
            LOG.debug("creating navigation stack");
            Stack<BeanDefinition<?>> navigationModel = new Stack<BeanDefinition<?>>();
            Bean<String> startPage = Bean.getBean(START_PAGE + " (" + StringUtil.substring(ENV.getName(), ".", null, true) + ")");
            navigationModel.push(startPage);

            //perhaps, use META-INF/persistence.xml directly without user input
            if (ENV.get("app.login.use.gui", true)) {
                navigationModel.push(login);
            } else {
                navigationModel.push(connect((Persistence) login.getInstance()));
            }
            return new EntityBrowser("entity-browser", navigationModel);
        } else {
            //create a copy for the new session
            try {
                workflow = workflow.clone();
            } catch (CloneNotSupportedException e) {
                ManagedException.forward(e);
            }
            if (ENV.get("app.login.use.gui", true)) {
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

    /**
     * {@inheritDoc}. thread-safe. will be called from session-thread
     */
    @Override
    public synchronized BeanDefinition<?> connect(Persistence persistence) {
        //define a new classloader to access all beans of given jar-file
        PersistenceClassLoader runtimeClassloader = new PersistenceClassLoader(new URL[0],
            rootClassloader());
        runtimeClassloader.addLibraryPath(ENV.getConfigPath());
        //TODO: the environment and current thread shouldn't use the new sessions classloader! 
        Thread.currentThread().setContextClassLoader(runtimeClassloader);
//        ENV.addService(ClassLoader.class, runtimeClassloader);

        createAuthorization(persistence.getAuth());

        //load all beans from selected jar-file and provide them in a beancontainer
        List<Class> beanClasses = null;
        try {
            beanClasses = createBeanContainer(persistence.clone(), runtimeClassloader);
        } catch (CloneNotSupportedException e) {
            ManagedException.forward(e);
        }

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
    private void createAuthorization(final String userName) {
        Message.send("creating authorization for " + userName);
        ConcurrentUtil.setCurrent(Authorization.create(userName, ENV.get("app.login.secure", false)));
    }

    /**
     * after the creation of a new classloader and a beancontainer providing access to the new loaded bean types, this
     * method creates entries for all bean-types to the navigation stack. overwrite this method to define own
     * bean-collector handling.
     * 
     * @param beanClasses new loaded bean types
     * @return a root bean-collector holding all bean-type collectors.
     */
    protected BeanDefinition<?> createBeanCollectors(List<Class> beanClasses) {
        Message.send("loading bean collectors for " + beanClasses.size() + " types");
        LOG.debug("creating collector for: ");
        List types = new ArrayList(beanClasses.size());
        for (Class cls : beanClasses) {
            LOG.debug("creating collector for: " + cls);
            BeanCollector collector = BeanCollector.getBeanCollector(cls, null,
                ENV.get("collector.mode.default", MODE_EDITABLE | MODE_CREATABLE
                    | MODE_MULTISELECTION
                    | MODE_SEARCHABLE),
                null);
            if (!BeanContainer.isConnected()
                || BeanContainer.instance().hasPermission(collector.getName().toLowerCase() + ".view", null))
                if (collector.getPresentable().isVisible())
                    types.add(collector);
        }
        /*
         * Load virtual BeanCollectors like QueryResult from directory.
         * name-convention: beandef/virtual/*.xml
         */
        types.addAll(BeanDefinition.loadVirtualDefinitions());

        /*
         * perhaps, do auto-translation if no resourcebundle present for current locale
         */
        if (NetUtil.isOnline() && !Messages.exists("messages")) {
            if (FileUtil.hasResource("messages.properties")) {
                ENV.registerBundle("messages", true);

                if (ENV.get("app.translate.bundle.project", true)) {
                    Message.send("doing machine translation for locale " + Locale.getDefault());
                    Translator.translateBundle(ENV.getConfigPath() + "messages", Messages.keySet(), Locale.ENGLISH,
                        Locale.getDefault());
                }
            } else {
                LOG.warn("no resource bundle 'messages.properties' found in environment directory");
            }
        }

        BeanCollector root = new BeanCollector(BeanCollector.class, types,
            ENV.get("collector.root.mode", MODE_EDITABLE | MODE_SEARCHABLE), null);
        root.setName(StringUtil.toFirstUpper(StringUtil
            .substring(Persistence.current().getJarFile().replace("\\", "/"), "/", ".jar", true)));
        root.setAttributeFilter("name");
        root.getAttribute("name").setFormat(new Format() {
            /** serialVersionUID */
            private static final long serialVersionUID = 1725704131355509738L;

            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                String name = StringUtil.substring((String) obj, null, BeanCollector.POSTFIX_COLLECTOR);
                toAppendTo.append(ENV.translate(name, true));
                pos.setEndIndex(1);
                return toAppendTo;
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });
        //perhaps, create the first environment.xml
        if (!ENV.isPersisted()) {
            ENV.persist();
            BeanDefinition.dump();
        }
        ENV.setAutopersist(true);
        return root;
    }

    /**
     * createBeanContainer
     * 
     * @param persistence
     * @param runtimeClassloader
     * @return
     */
    protected List<Class> createBeanContainer(final Persistence persistence,
            PersistenceClassLoader runtimeClassloader) {
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
            String attachmentJarName = Attachment.getFilename(persistence, "jarFile", jarName);
            File attFile = new File(attachmentJarName);
            if (attFile.canRead()) {
                FileUtil.copy(attFile.getPath(), persistence.jarFileInEnvironment());
            }
            selectedFile =
                new File(ENV.getConfigPath()
                    + FileUtil.getURIFile(jarName).getPath());
        }
        jarName = selectedFile.getPath();
        if (!selectedFile.exists() && !isAbsolutePath) {
            //ant-scripts can't use the nested jars. but normal beans shouldn't have dependencies to simple-xml.
            ENV.extractResource(JAR_SIMPLEXML);
            ENV.extractResource(JAR_COMMON);
            ENV.extractResource(JAR_DIRECTACCESS);
            ENV.extractResource(JAR_SERVICEACCESS);

            provideScripts(persistence);

            String generatorTask;
            if (persistence.getGenerator().equals(Persistence.GEN_HIBERNATE)) {
                generatorTask = "org.hibernate.tool.ant.HibernateToolTask";
            } else {
                generatorTask = "org.apache.openjpa.jdbc.ant.ReverseMappingToolTask";
            }
            ENV.loadClassDependencies("org.apache.tools.ant.taskdefs.Taskdef",
                generatorTask, persistence.getConnectionDriverClass(), persistence.getProvider());

            if ((isLocalDatabase(persistence) && !canConnectToLocalDatabase(persistence))
                || persistence.autoDllIsCreateDrop()) {
                generateDatabase(persistence);
            }
            Boolean generationComplete =
                generateJarFile(jarName, persistence.getGenerator(), persistence.getDefaultSchema());
            //return value may be null or false
            if (generationComplete == null || !Boolean.TRUE.equals(generationComplete) || !new File(jarName).exists()) {
                throw new ManagedException(
                    "Couldn't generate bean jar file '"
                        + jarName
                        + "' through ant-script 'reverse-eng.xml'! Please see log file for exceptions.\n"
                        + (!ENV.get(CompatibilityLayer.class).isAvailable("java.lang.Compiler")
                            ? "\tYOUR JAVA IS ONLY A JRE! We need the full JDK to compile the generated classes.\n"
                            : "")
                        + "\nAs alternative you may select an existing bean-jar file (-->no generation needed!) in field \"JarFile\"\n\n"
                        + ENV.get(UncaughtExceptionHandler.class).toString());
            }
        } else if (isAbsolutePath) {//copy it into the own classpath (to don't lock the file)
            if (!selectedFile.exists()) {
                throw new IllegalArgumentException(
                    selectedFile
                        + " (-- If an absolute file-path is given, the file has to exist! If the file-path is relative and doesn't exist, it will be created/generated.");
            }
//            if (!new File(envFile).exists())
            FileUtil.copy(selectedFile.getPath(), persistence.jarFileInEnvironment());
        }

        //may be on second start after having already generated the jar file
        if (isLocalDatabase(persistence) && !canConnectToLocalDatabase(persistence)) {
            String[] cmd =
                AppLoader.isUnix() ? new String[] { "sh", "runServer.cmd" } : new String[] { "cmd", "/C", "start",
                    "runServer.cmd" };
            SystemUtil.execute(new File(ENV.getConfigPathRel()), cmd);
            //prepare shutdown and backup
            Runtime.getRuntime().addShutdownHook(Executors.defaultThreadFactory().newThread(new Runnable() {
                @Override
                public void run() {
                    if (BeanContainer.isInitialized()) {
                        Message.broadcast(this, "APPLICATION SHUTDOWN INITIALIZED...", "*");
                        LOG.info("preparing shutdown of local database " + persistence.getConnectionUrl());
                        try {
                            BeanContainer.instance().executeStmt(ENV.get("app.shutdown.statement", "shutdown"), true,
                                null);
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            LOG.error(e.toString());
                        }
                        String hsqldbScript = isH2(persistence.getConnectionUrl())
                            ? persistence.getDefaultSchema() + ".mv.db" : persistence.getDatabase() + ".script";
                        String backupFile =
                            ENV.getTempPathRel() + FileUtil.getUniqueFileName(ENV.get("app.database.backup.file",
                                persistence.getDatabase()) + ".zip");
                        LOG.info("creating database backup to file " + backupFile);
                        FileUtil.writeToZip(backupFile, hsqldbScript, FileUtil.getFileBytes(hsqldbScript, null));
                    }
                }
            }));
            //do a periodical backup
            SchedulerUtil.runAt(0, -1, TimeUnit.DAYS, new Runnable() {
                @Override
                public void run() {
                    LOG.info("preparing backup of local database " + persistence.getConnectionUrl());
                    try {
                        BeanContainer.instance().executeStmt(
                            ENV.get("app.backup.statement", "backup to temp/database-daily-backup.zip"), true, null);
                    } catch (Exception e) {
                        LOG.error(e.toString());
                    }
                }
            });
        }

        boolean useJPAPersistenceProvider = true;
        /* 
         * check, whether a real/existing jpa-persistence-provider was selected.
         * perhaps provide directly an EntityManager
         */
        if (!ENV.get("app.use.applicationserver", false)) {
//            Environment.loadDependencies(persistence.getProvider());
            Class[] provider = ENV.get(CompatibilityLayer.class).load(persistence.getProvider());
            if (NanoEntityManagerFactory.AbstractEntityManager.class.isAssignableFrom(provider[0])) {
                useJPAPersistenceProvider = false;
                //ok, here we use again the class as string ;-(
                ENV.addService(
                    EntityManager.class,
                    NanoEntityManagerFactory.instance().createEntityManager(persistence.getProvider(),
                        persistence.getJdbcProperties()));
            }
        }

        if (ENV.get("app.use.applicationserver", false)) {
            ServiceFactory.createInstance(runtimeClassloader);
            //a service has to load user object, roles and features project specific!
            BeanClass authentication =
                BeanClass.createBeanClass(ENV.get("app.applicationserver.authentication.service",
                    ENV.getApplicationMainPackage() + ".service.remote.IUserService"));
            Object authService = ServiceFactory.instance().getService(authentication.getClazz());
            BeanClass.call(authService, ENV.get("app.applicationserver.authentication.method", "login"),
                new Class[] {
                    String.class, String.class },
                persistence.getConnectionUserName(),
                persistence.getConnectionPassword());
//            ServiceFactory.instance().createSession(userObject, mandatorObject, subject, userRoles, features, featureInterfacePrefix)
            BeanContainerUtil.initGenericServices(runtimeClassloader);
        } else {
            ENV.loadClassDependencies(persistence.getConnectionDriverClass(),
                persistence.getDatasourceClass(), persistence.getProvider());
            GenericLocalBeanContainer.initLocalContainer(runtimeClassloader,
                useJPAPersistenceProvider && ENV.get("app.login.service.connection.check", false));
        }
        ENV.addService(IBeanContainer.class, BeanContainer.instance());
        ConcurrentUtil.setCurrent(BeanContainer.instance());

        List<Class> beanClasses =
            runtimeClassloader.loadBeanClasses(jarName,
                ENV.get("bean.class.presentation.regexp", ".*"), null);
        ENV.setProperty("service.loadedBeanTypes", beanClasses);

        return beanClasses;
    }

    private void provideScripts(Persistence persistence) {
        System.setProperty(HIBREVNAME_TEMPLATE + ".destination", HIBREVNAME);

        //check if an equal named ddl-script is inside our jar file. should be done on 'anyway' or 'timedb'.
        try {
            ENV.extractResource(persistence.getDatabase() + ".sql", false, false, false);
            ENV.extractResource("drop-" + persistence.getDatabase() + ".sql", false, false, false);
            ENV.extractResource("init-" + persistence.getDatabase() + ".sql", false, false, false);
        } catch (Exception e) {
            LOG.warn(e);
            //ok, it was only a try ;-)
        }
        ENV.extractResource(HIBREVNAME_TEMPLATE);

    }

    private boolean isLocalDatabase(Persistence persistence) {
        String url = persistence.getConnectionUrl();
        if (!Util.isEmpty(persistence.getPort()) || isH2(url)) {
            return Arrays.asList(persistence.STD_LOCAL_DATABASE_DRIVERS).contains(
                persistence.getConnectionDriverClass())
                && (url.contains("localhost") || url.contains("127.0.0.1") || isH2(url));
        }
        return false;
    }

    private boolean isH2(String url) {
        return url.matches("jdbc[:]h2[:].*");
    }

    private boolean canConnectToLocalDatabase(Persistence persistence) {
        if (!Util.isEmpty(persistence.getPort())) {
            int p = Integer.valueOf(persistence.getPort());
            return NetUtil.isOpen(p);
        }
        return false;
    }

    private void generateDatabase(Persistence persistence) {
        Message.send("creating new database " + persistence.getDatabase() + " for url "
            + persistence.getConnectionUrl());
        Properties p = new Properties();
        ENV.setProperty("app.doc.name", persistence.getDatabase());
        
        //give mda.xml the information to don't start nano.h5
        p.put("nano.h5.running", "true");

        ENV.get(CompatibilityLayer.class).runRegistered("ant",
            ENV.getConfigPath() + "mda.xml",
            "do.all",
            p);
    }

    protected static Boolean generateJarFile(String jarFile, String generator, String schema) {
        ENV.extractResource(REVERSE_ENG_SCRIPT);
        ENV.extractResource(HIBREVNAME_TEMPLATE);
        Properties properties = new Properties();
        properties.setProperty(HIBREVNAME, ENV.getConfigPath() + HIBREVNAME);
//    properties.setProperty("hbm.conf.xml", "hibernate.conf.xml");
        properties.setProperty("server.db-config.file", Persistence.FILE_JDBC_PROP_FILE);
        properties.setProperty("dest.file", jarFile);
        properties.setProperty("generator", generator);
        properties.setProperty("schema", schema);
        properties.setProperty(BEAN_GENERATION_PACKAGENAME,
            ENV.get(BEAN_GENERATION_PACKAGENAME, "org.anonymous.project"));
        String plugin_dir = System.getProperty("user.dir");
        properties.setProperty("plugin.dir", new File(plugin_dir).getAbsolutePath());
        if (plugin_dir.endsWith(".jar/")) {
            properties.setProperty("plugin_isjar", Boolean.toString(true));
        }
        Message.send("starting generation of '" + jarFile + "' through script " + REVERSE_ENG_SCRIPT);
        //If no environment was saved before, we should do it now!
        ENV.persist();

        return (Boolean) ENV.get(CompatibilityLayer.class).runRegistered("ant",
            ENV.getConfigPath() + REVERSE_ENG_SCRIPT,
            "create.bean.jar",
            properties);
    }

    private String applicationHtmlFile() {
        return ENV.getTempPath() + START_HTML_FILE;
    }

    @Override
    public void reset() {
        String configPath = ENV.get(ENV.KEY_CONFIG_PATH, "config");

        ConcurrentUtil.removeAllCurrent(NanoH5Session.getThreadLocalTypes());

        ENV.get(RulePool.class).reset();
        ENV.get(QueryPool.class).reset();
        ENV.get(ActionPool.class).reset();
        BeanContainer.reset();
        Bean.clearCache();

        sessions.clear();
        ENV.reload();
        ENV.setProperty(ENV.KEY_CONFIG_PATH, configPath);
        ENV.setProperty("service.url", serviceURL.toString());
        NetworkClassLoader.resetUnresolvedClasses(ENV.getConfigPath());
        Thread.currentThread().setContextClassLoader(appstartClassloader);

        HtmlUtil.tableDivStyle = null;
        createPageBuilder();
        builder = ENV.get(IPageBuilder.class);
    }

    @Override
    public void persist() {
        super.persist();
        ENV.persist();
    }
    
    @Override
    public EventController getEventController() {
        return eventController;
    }

    @Override
    public String toString() {
        return Util.toString(NanoH5.class, "serviceURL: " + serviceURL,
            "sessions: " + (sessions != null ? sessions.size() : 0), "requests: " + requests);
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
