package de.tsl2.nano.h5;

import static de.tsl2.nano.bean.def.IBeanCollector.MODE_CREATABLE;
import static de.tsl2.nano.bean.def.IBeanCollector.MODE_EDITABLE;
import static de.tsl2.nano.bean.def.IBeanCollector.MODE_MULTISELECTION;
import static de.tsl2.nano.bean.def.IBeanCollector.MODE_SEARCHABLE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.Executors;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.IBeanContainer;
import de.tsl2.nano.bean.def.AbstractExpression;
import de.tsl2.nano.bean.def.Attachment;
import de.tsl2.nano.bean.def.AttributeCover;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.IBeanDefinitionSaver;
import de.tsl2.nano.bean.def.IIPresentable;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.PathExpression;
import de.tsl2.nano.collection.ExpiringMap;
import de.tsl2.nano.core.AppLoader;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.IEnvChangeListener;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.Main;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.UnboundAccessor;
import de.tsl2.nano.core.exception.ExceptionHandler;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.execution.CompatibilityLayer;
import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.messaging.ChangeEvent;
import de.tsl2.nano.core.messaging.EventController;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.NanoHTTPD.Response.Status;
import de.tsl2.nano.h5.collector.QueryResult;
import de.tsl2.nano.h5.configuration.BeanConfigurator;
import de.tsl2.nano.h5.expression.Query;
import de.tsl2.nano.h5.expression.RuleExpression;
import de.tsl2.nano.h5.expression.SQLExpression;
import de.tsl2.nano.h5.expression.SimpleExpression;
import de.tsl2.nano.h5.expression.URLExpression;
import de.tsl2.nano.h5.expression.WebClient;
import de.tsl2.nano.h5.navigation.EntityBrowser;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.h5.navigation.Workflow;
import de.tsl2.nano.h5.plugin.INanoPlugin;
import de.tsl2.nano.persistence.DatabaseTool;
import de.tsl2.nano.persistence.GenericLocalBeanContainer;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.persistence.PersistenceClassLoader;
import de.tsl2.nano.persistence.provider.NanoEntityManagerFactory;
import de.tsl2.nano.plugin.Plugins;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.service.util.IGenericService;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.specification.DocumentWorker;
import de.tsl2.nano.specification.PFlow;
import de.tsl2.nano.specification.Pool;
import de.tsl2.nano.specification.SpecificationExchange;
import de.tsl2.nano.specification.actions.Action;
import de.tsl2.nano.specification.rules.Rule;
import de.tsl2.nano.specification.rules.RuleDecisionTable;
import de.tsl2.nano.specification.rules.RuleScript;
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
public class NanoH5 extends NanoHTTPD implements ISystemConnector<Persistence>, IEnvChangeListener {

	private static final Log LOG = LogFactory.getLog(NanoH5.class);

    public static final String JAR_COMMON = "tsl2.nano.common.jar";
    public static final String JAR_SERVICEACCESS = "tsl2.nano.serviceaccess.jar";
    public static final String JAR_DIRECTACCESS = "tsl2.nano.directaccess.jar";
    public static final String JAR_INCUBATION = "tsl2.nano.incubation.jar";
    public static final String JAR_CURSUS = "tsl2.nano.cursus.jar";
    public static final String JAR_SAMPLE = "tsl2.nano.h5.sample.jar";
    public static final String JAR_RESOURCES = "tsl2.nano.h5.default-resources.jar";
    public static final String JAR_SIMPLEXML = "tsl2.nano.simple-xml.jar";

    public static final String ZIP_STANDALONE = "standalone.zip";

    static final String MDA_SCRIPT = "mda.xml";
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

    Map<Object, NanoH5Session> sessions;
    long requests = 0;
    IPageBuilder<?, String> builder;
    URL serviceURL;
    ClassLoader appstartClassloader;

    private EventController eventController;

    private Request lastRequest;
    private WebSecurity webSec = new WebSecurity();
    
//    /** workaround to avoid re-serving a cached request. */
//    private Properties lastHeader;

    private static final String START_HTML_FILE = /*AppLoader.getFileSystemPrefix() +*/"application.html";
    static final String START_PAGE = "Start";
    static final int OFFSET_FILTERLINES = 2;

    private static final String BEAN_GENERATION_PACKAGENAME = "bean.generation.packagename";

    public NanoH5() throws IOException {
        this(ENV.get("service.url", DEFAULT_URL), ENV.get(IPageBuilder.class));
    }

    public NanoH5(String serviceURL, IPageBuilder<?, String> builder) throws IOException {
//        super(null, getPort(serviceURL), new File(ENV.getConfigPath()), !LOG.isDebugEnabled());
        super(getPort(serviceURL), new File(ENV.getConfigPath()));
        FileUtil.writeBytes(String.valueOf(hashCode()).getBytes(), ENV.getTempPath() + "instance-id.txt", false);
        this.serviceURL = getServiceURL(serviceURL);
        this.builder = builder != null ? builder : createPageBuilder();
        ENV.registerBundle(NanoH5.class.getPackage().getName() + ".messages", true);
        appstartClassloader = Util.getContextClassLoader();
        ENV.addService(ClassLoader.class, appstartClassloader);
        eventController = new EventController();
        sessions = Collections.synchronizedMap(
            new ExpiringMap<Object, NanoH5Session>(ENV.get("session.timeout.millis", 12 * DateUtil.T_HOUR)));
        registereExpressionsAndPools();
        
        Plugins.process(INanoPlugin.class).configuration(ENV.getProperties(), ENV.services());
    }

	@Override
	protected void initENVService() {
        ENV.addService(Main.class, this);
	}
	
	public static final void registereExpressionsAndPools() {
		//the classes registere themselves on loading...
        AbstractExpression.registerExpression(PathExpression.class);
        AbstractExpression.registerExpression(RuleExpression.class);
        AbstractExpression.registerExpression(SQLExpression.class);
        AbstractExpression.registerExpression(URLExpression.class);
        AbstractExpression.registerExpression(SimpleExpression.class);
        Pool.registerTypes(Rule.class, RuleScript.class, RuleDecisionTable.class, Query.class, Action.class, WebClient.class, PFlow.class);
	}

    /**
     * main
     * 
     * @param args
     */
    public static void main(String[] args) {
        startApplication(NanoH5.class, null, args);
    }

    /**
     * starts application and shows initial html page
     */
    @Override
    public void start() {
        //print errors directly
        createExceptionhandler();
        try {
//            LogFactory.setLogLevel(LogFactory.LOG_ALL);
        	ENV.setProperty("service.entitymanagerfactory.close", false);                
            LOG.debug(System.getProperties());

            createStartPage();

            extractJarScripts();
            extractDefaultResources();
            ENV.extractResource(JAR_CURSUS);
            
            //perhaps activate secure transport layer TLS
            enableSSL(ENV.get("app.ssl.activate", false));
            runHttpServer();
        } catch (Exception ioe) {
            LOG.error("Couldn't start server: ", ioe);
            try {//if not yet done...
                ENV.persist();
            } catch (Exception e1) {
                LOG.error(e1);
            }
            ConcurrentUtil.sleep(3000);
            LOG.info("==============================================");
            LOG.info("NANOH5 SHUTDOWN -> NanoHTTPD stopped on Error!");
            LOG.info("==============================================");
            // System.exit(-1);
            stop();
        }
    }

	private void createExceptionhandler() {
		Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (e instanceof Message) System.err.println(e.getMessage()); else LOG.error(e);
            }
        });
	}

    protected void enableSSL(boolean ssl) throws IOException {
        if (ssl) {
            String keyStore = ENV.get("app.ssl.keystore.file", "nanoh5.pks");
            if (keyStore.startsWith("nanoh5")) {
                ENV.extractResource("nanoh5.pks");
                ENV.extractResource("nanoh5.jks");
            }
            LOG.info("activating ssl using keystore " + keyStore);
            makeSecure(NanoHTTPD.makeSSLSocketFactory(keyStore,
                ENV.get("app.ssl.keystore.password", "nanoh5").toCharArray()), null);
            ENV.setProperty("app.ssl.shortcut", "s");
        } else {
            ENV.setProperty("app.ssl.shortcut", "");
        }
    }

    protected void extractDefaultResources() {
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
            ENV.extractResource("permissions.xsd");
            /*
             * DEPRECATED: we integrate the 'anyway' database as sample
             * on first start, extract the sample files
             */
            if (ENV.get("app.create.sample.files.on.first.start", false))
                ((Html5Presentation) ENV.get(BeanPresentationHelper.class)).createSampleEnvironment();
        }
    }

    protected void extractJarScripts() {
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
            ENV.extractResource(MDA_SCRIPT);
            ENV.extractResource("compilejar.cmd");
            ENV.extractResource("tsl2nano-appcache.mf");
            ENV.extractResource("favicon.ico");
            
            ENV.extractResource("doc/beanconfigurator.help.html");
            ENV.extractResource("doc/attributeconfigurator.help.html");
            ENV.extractResource("doc/entry.help.html");
            ENV.extractResource("doc/persistence.help.html");

            ENV.extractResource("specification/specification-timesheet.properties.csv");
            ENV.extractResource("specification/import-timesheet.log");
            onStandaloneExtractJars();
        } catch (Exception ex) {
            LOG.error("couldn't extract ant or shell script", ex);
            ENV.get(UncaughtExceptionHandler.class).uncaughtException(null, ex);
        }
    }

    private void runHttpServer() {
        System.setProperty("java.net.preferIPv6Addresses", "true");
        super.start();

        try {
            if (System.getProperty("os.name").startsWith("Windows")
                    && ENV.get("app.show.startpage", true)) {
                SystemUtil.executeRegisteredWindowsPrg(applicationHtmlFile());
            } else if (System.getProperty("os.name").startsWith("Linux")
                        && ENV.get("app.show.startpage", true)) {
                    SystemUtil.executeRegisteredLinuxBrowser(applicationHtmlFile());
            } else {
                LOG.info("Please open the URL '" + serviceURL.toString() + "' in your browser");
            }
        } catch(Exception ex) {
            //ok, no problem - server run should continue...
            LOG.warn("couldn't start browser", ex);
        }

        try {
            LOG.info("Listening on port " + serviceURL.getPort() + ". Hit Enter or Strg+C to stop.\n");
            LOG.debug("waiting for input on " + System.in);
            if (System.in.read() == -1)
                throw new IllegalStateException("Empty System-Input returning -1");
        } catch (Exception ex) {
            LOG.info("server mode without input console available (message: " + ex.toString() + ")");
            while (true) {
                ConcurrentUtil.sleep(3000, false);
            }
        }
    }

    /**
     * if this jar is a standalone (containing o/r-mapper and a database), extract that jars to be available for ant
     * scripts. for more informations, see {@link #LIBS_STANDALONE}.
     */
    private void onStandaloneExtractJars() {
        final String JAR_JPA_API = ENV.get("app.standalone.zip.file.check", "hibernate-jpa-2.1-api-1.0.2.Final.jar");
        if (FileUtil.hasResource(ZIP_STANDALONE) && !new File(ENV.getConfigPath() + JAR_JPA_API).exists()) {
            LOG.info("setting tsl2nano to offfline modus...");
            ENV.setProperty("tsl2nano.offline", true);
            System.setProperty("tsl2nano.offline", "true");
            LOG.info("extracting " + ZIP_STANDALONE + " and " + JAR_JPA_API);
            ENV.extractResource(JAR_JPA_API, true, false);
            FileUtil.extractNestedZip(ZIP_STANDALONE, ENV.getConfigPath(), null);
            //wait, until the ant jars are stored and loaded
//            ConcurrentUtil.startDaemon("regex-replace-run-scripts", new Runnable() {
//                @Override
//                public void run() {
//                    ConcurrentUtil.sleep(3000);
//                    try {
//                        AntRunner.runRegexReplace("(NAME=).*", "\\1" + ENV.getName(), System.getProperty("user.dir"), "run.bat");
//                        AntRunner.runRegexReplace("[#](STANDALONE)", "\\1", System.getProperty("user.dir"), "run.sh");
//                    } catch (Exception e) {
//                        //ok, no real problem, but log it...
//                        LOG.error("", e);
//                    }
//                }
//            });
        }
    }

	private NanoH5Session getSession(Map<String, String> header, InetAddress requestor) {
		NanoH5Session session;
		Object sessionID = webSec.getSessionID(header, requestor);
		session = sessions.get(sessionID);
		//fallback to requestor - if cookie or etag is wrong!
		if (session == null && sessionID != requestor) //yes, id. object!
		    session = sessions.get(requestor);
		return session;
	}

    public NanoH5Session getSession(InetAddress address) {
        return sessions.get(address);
    }
    
    public NanoH5Session getSession(String userName) {
        for (NanoH5Session s : sessions.values()) {
            if (s.getUserAuthorization() != null && s.getUserAuthorization().getUser().toString().equals(userName))
                return s;
        }
        return null;
    }
    
    public static URL getServiceURL(String serviceURLString) {
        if (serviceURLString == null) {
            serviceURLString = ENV.get("service.url", getDefaultURL());
            //set defaults
            ENV.get("app.ssl.activate", false);
            ENV.get("app.ssl.shortcut", "");
            return NetUtil.url(serviceURLString);
        }
        //perhaps activate secure transport layer TLS
        if (serviceURLString.startsWith("https"))
            ENV.setProperty("app.ssl.activate", true);
        if (ENV.get("app.ssl.activate", false)) {
            ENV.setProperty("app.ssl.shortcut", "s");
            serviceURLString = serviceURLString.replace("http://", "https://");
        } else {
            ENV.setProperty("app.ssl.shortcut", "");
            serviceURLString = serviceURLString.replace("https://", "http://");
        }
        if (!serviceURLString.matches(".*[:][0-9]{3,7}")) {
            serviceURLString = (ENV.get("service.access.remote", false) ? NetUtil.getMyIP() + ":" : "localhost:") + serviceURLString;
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

    private static String getDefaultURL() {
        return ENV.get("service.access.remote", false) ? DEFAULT_URL.replace("localhost", NetUtil.getMyIP()) : DEFAULT_URL;
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
        String page =
            Html5Presentation.createMessagePage("start.template", ENV.translate("tsl2nano.start", true) + " "
                + ENV.translate(ENV.getName(), true), getServiceURL(serviceURL.toString()));
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
        NanoH5Session session = null;
        long startTime = 0;
        try {
            Plugins.process(INanoPlugin.class).requestHandler(uri, m, header, parms, files);
            InetAddress requestor = ((Socket) ((Map) header).get("socket")).getInetAddress();
            Response whiteListError = checkWhiteList(requestor);
            if (whiteListError != null)
                return whiteListError;
            if (RestUI.canRest(uri)) {
            	session = getSession(header, requestor);
            	if (session != null && checkSessionTimeout(session, requestor)) {
            		addRestAuthorizationFromSession(session, uri, method, header);
            	}
                if (RestUI.canRestUI(uri))
                    return new RestUI().serve(session, uri, method, header, parms, files);
                else
            	    return new RESTDynamic().serve(uri, method, header, parms, files);
            }
            if (method.equals("GET") && !isAdmin(uri) && !uri.endsWith("/help") && !RESTDynamic.canRest(uri)) {
                // serve files
                if (!NumberUtil.isNumber(uri.substring(1)) && HtmlUtil.isURI(uri)
                    && !uri.contains(Html5Presentation.PREFIX_BEANREQUEST)) {
                    Response response = super.serve(uri, method, header, parms, files);
                    // webSec.addETag(uri, response, ENV.get("app.session.etag.timeout", "" + 24 * 3600));
                    webSec.addSessionHeader(null, response);
                    return response;
                }
            }

            startTime = System.currentTimeMillis();
            //TODO: in InternetExporer/Edge we get sometimes IP4 and sometimes IP6. should we set system property java.net.preferIPv6Addresses?
            //           sessions.keySet().iterator().next().getAllByName(requestor.getHostName()).equals(requestor) returns false
            Request req = new Request(requestor, uri, m, header, parms, files);
            if (lastRequest != null && lastRequest.equals(req)) {//waiting for the first request to 
                LOG.warn("duplicated request from " + requestor);
                ConcurrentUtil.sleep(2000);
            }
            lastRequest = req;
            //ETag from Browser: If-None-Match
            session = getSession(header, requestor);
            // application commands
            if ((method.equals("GET") && uri.endsWith("/help")) || method.equals("OPTIONS"))
                return help();
            if (isAdmin(uri)) {
                control(StringUtil.substring(uri, String.valueOf(hashCode())+"-", null), session);
            }
            if (session != null && session.getNavigationStack() == null) {
                Message.send("bad session state, please login again!");
                session.close();
                session = null;
            }
            //if a user reconnects on the same machine, we remove his old session
            if (session != null && session.getUserAuthorization() != null && parms.containsKey("connectionUserName")
                && !parms.get("connectionUserName").equals(session.getUserAuthorization().getUser().toString())) {
                Message.send("cretaing new user session");
                session.close();
                session = null;
            }
            if (session == null) {
                //on a new session, no parameter should be set
                session = createSession(requestor);
            } else {//perhaps session was interrupted/closed but not removed
                if (checkSessionTimeout(session, requestor) 
                	&& session.response != null && method.equals("GET") && parms.size() < 2/* contains 'QUERY_STRING = null' */
                    && (uri.length() < 2 || header.get("referer") == null) || (isDoubleClickDelay(session) && session.response != null)) {
                        Response result = reloadSessionPage(session);
                        if (result != null)
                            return result;
                }
            }
    //        //workaround to avoid doing a cached request twice
    //        lastHeader = header;
        } catch(Throwable ex) {
        	if (session == null)
            	return createResponse(Status.UNAUTHORIZED, MIME_HTML, ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "Request unauthorized");
            else {
                if (ex instanceof Exception) {
                    Message.send(ex);//don't let the server go down on any exception
                    Response result = reloadSessionPage(session);
                    if (result != null)
                        return result;
                    else
            	        return createResponse(Status.BAD_REQUEST, MIME_HTML, ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "Bad Request");
                }
                else
                    return createResponse(Status.INTERNAL_ERROR, MIME_HTML, "Internal Server Error");
            }
        }
        requests++;
        session.startTime = startTime;
        return session.serve(uri, method, header, parms, files);
    }

    Response reloadSessionPage(NanoH5Session session) {
        LOG.debug("reloading cached page...");
        try {
            session.response.getData().reset();
            if (!session.cacheReloaded) {
                session.cacheReloaded = true;
                return webSec.addSessionHeader(session, new Response(Status.OK, "text/html", session.response.getData(), -1));
            }
        } catch (IOException e) {
            LOG.error(e);
        }
        return null;
    }

	private Response checkWhiteList(InetAddress requestor) {
        return requestor == null
                || requestor.getHostAddress().matches(ENV.get("app.session.requestor.whitelist.regex", ".*"))
            ? null
            : createResponse(Status.FORBIDDEN, MIME_HTML, "Request forbidden");
    }

    private void addRestAuthorizationFromSession(ISession session, String uri, String method, Map<String, String> header) {
		header.put("authorization", ARESTDynamic.createDigest(uri, method, session.getId().toString()));
		((Map)header).put(ARESTDynamic.H5SESSION, session);
		header.put("user", ((IAuthorization)session.getUserAuthorization()).getUser().toString());
		ConcurrentUtil.setCurrent(BeanContainer.instance(), session.getUserAuthorization());
	}

	private boolean checkSessionTimeout(NanoH5Session session, InetAddress requestor) {
		if (!session.check(ENV.get("session.timeout.millis", 30 * DateUtil.T_MINUTE),
				ENV.get("session.timeout.throwexception", false))) {
			// TODO: show page with 'session expired'
			if (ENV.get("session.workflow.close", false)) {
				session.close();
				session = createSession(requestor);
			}
			boolean done = session.nav != null && session.nav.done();
			if (done) {
				// the workflow was done, now create the entity browser
				LOG.info("session-workflow of " + session + " was done. creating an entity-browser now...");
				session.nav = createGenericNavigationModel(true);
				// don't lose the connection - the first item is the login
//                    session.nav.next(session.nav.toArray()[1]);
			}
			return false;
		}
		return true;
	}

    private boolean isAdmin(String uri) {
        return uri != null && HtmlUtil.isURI(uri) && uri.contains(String.valueOf(hashCode()));
    }

    private Response help() {
        String help = "{application-hash}-{shutdown|close|back}";
        return new Response(Status.OK, "text/html", StringUtil.toInputStream(help), -1);
    }

    //TODO: do some encryptions...
    //TODO: provide generic semantics on beans
    private void control(String cmd, NanoH5Session session) {
        if (cmd.equals("close"))
            session.close();
        else if (cmd.equals("back"))
            session.nav.next(null);
        else if (cmd.equals("shutdown")) {
        	//give the server the chance to return the current request
            ConcurrentUtil.startDaemon(new Runnable() {
				@Override
				public void run() {
					ConcurrentUtil.sleep(1000);
					// System.exit(0);
                    stop();
				}
			});
        }
    }

    protected Map<Object, NanoH5Session> getSessions() {
        return sessions;
    }
    
    private boolean isDoubleClickDelay(NanoH5Session session) {
        return System.currentTimeMillis() - ENV.get("app.event.dblclick.delay", 100) < session.getLastAccess();
    }

    public Response createResponse(String msg) {
        return createResponse(Response.Status.OK, MIME_HTML, msg);
    }

    public static Response createResponse(Status status, String type, String msg) {
        byte[] bytes = msg.getBytes();
        return new NanoHTTPD.Response(status, type, new ByteArrayInputStream(bytes), bytes.length);
    }

    /**
     * adds services for presentation and page-builder
     */
    protected IPageBuilder<?, String> createPageBuilder() {
        IPageBuilder pageBuilder = Plugins.process(INanoPlugin.class).definePresentationType(new Html5Presentation());
        if (pageBuilder instanceof BeanPresentationHelper)
            ENV.addService(BeanPresentationHelper.class, (BeanPresentationHelper)pageBuilder);
        else 
            ENV.addService(BeanPresentationHelper.class, new Html5Presentation<>());
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
            //TODO: the new created session is not yet authorized...is it the right time to add it as known session?
            sessions.put(session.getKey(), session);
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
        if (!ENV.get(CompatibilityLayer.class).isAvailable("de.tsl2.nano.vnet.workflow.ComparableMap")) {
            ENV.extractResource(JAR_INCUBATION);
            //ENV holds an old class reference, so we use the threads context classloader
//            ((NetworkClassLoader)Thread.currentThread().getContextClassLoader()).addFile(ENV.getConfigPath() + System.getProperty(JAR_INCUBATION));
            //wait until the classloader found the new jar file
            ConcurrentUtil.sleep(2000);
        }

        IBeanNavigator workflow = ENV.get(Workflow.class);

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
            workflow = new EntityBrowser("entity-browser", navigationModel);
        } else {
            //create a copy for the new session
            try {
                 workflow = workflow.clone();
            } catch (CloneNotSupportedException e) {
                ManagedException.forward(e);
            }
            if (ENV.get("app.login.use.gui", true)) {
                workflow.setRoot(login);
            } else {
                workflow.add(connect((Persistence) login.getInstance()));
            }
        }
        Plugins.process(INanoPlugin.class).workflowHandler(workflow);
        return workflow;
    }

    private static int createNavigationStartPoint(IBeanNavigator navigator, BeanCollector root) {
        //simple optional entry point
        //root will only be added, if at least one entrypoint was found!
        String beanCollectorNames = ENV.get("session.navigation.start.beandefinitions", null);
        if (beanCollectorNames != null) {
            String[] beanDefNames = beanCollectorNames.split("[,;]");
            navigator.add(root);
            for (int i = 0; i < beanDefNames.length; i++) {
                LOG.info("adding " + beanDefNames[i] + " to navigation stack");
                navigator.add(BeanDefinition.getBeanDefinition(beanDefNames[i]));
            }
            return beanDefNames.length;
        }
        return 0;
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
        ClassLoader rootCl;
        if (rootClassloader() instanceof Cloneable)
            rootCl = ObjectUtil.cloneObject(rootClassloader());
        else {
            LOG.warn("classloader " + rootClassloader() + " not clonable for new session - using rootClassloader itself");
            rootCl = rootClassloader();
        }
        PersistenceClassLoader runtimeClassloader = new PersistenceClassLoader(new URL[0], rootCl);
        runtimeClassloader.addLibraryPath(ENV.getConfigPath());
        //TODO: the environment and current thread shouldn't use the new sessions classloader! 
        Thread.currentThread().setContextClassLoader(runtimeClassloader);
        ENV.addService(ClassLoader.class, runtimeClassloader);
    	SpecificationH5Exchange h5Exchange = new SpecificationH5Exchange();
		ENV.addService(IBeanDefinitionSaver.class, h5Exchange);
    	ENV.addService(SpecificationExchange.class, h5Exchange);

        createAuthorization(persistence.getAuth());
        if (ENV.get("app.login.administration", true))
            NanoH5Util.enrichFromSpecificationProperties();
        
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
    private IAuthorization createAuthorization(final String userName) {
        Message.send("creating authorization for " + userName);
        Authorization auth = Authorization.create(userName, ENV.get("app.login.secure", false));
        Plugins.process(INanoPlugin.class).onAuthentication(auth);
        ConcurrentUtil.setCurrent(auth);
        return auth;
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
    	File docWorkerFile = new File(ENV.getConfigPath() + "specification-documentworker.md.html");
    	if (docWorkerFile.exists() && ENV.get("app.login.administration", true))
        {
	    		if (Message.ask("Run Specification Documentworker on file " + docWorkerFile + "?", true)) {
			    	ENV.addService(new DocumentWorker());
			    	ENV.get(DocumentWorker.class).consume(docWorkerFile.getAbsolutePath());
	    	}
    	}
    	
        Message.send("loading bean collectors for " + beanClasses.size() + " types");
        LOG.debug("creating collector for: ");
        List<BeanDefinition> types = new ArrayList(beanClasses.size());
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

        for (BeanDefinition beanDef : types) {
            Plugins.process(INanoPlugin.class).defineBeanDefinition(beanDef);
        }

        provideResourceBundelWithAutoTranslation();

        BeanCollector root = new BeanCollector(BeanCollector.class, types,
            ENV.get("collector.root.mode", MODE_EDITABLE | MODE_SEARCHABLE), null);
        root.setName(StringUtil.toFirstUpper(StringUtil
            .substring(Persistence.current().getJarFile().replace("\\", "/"), "/", ".jar", true)));
        
        ((IIPresentable)root.getPresentable()).setLabel(root.getName());
        root.setAttributeFilter("name");
        root.setSimpleList(ENV.get("collector.root.listhorizontal", false));
        initBeanCollectorFormatter(root);
        //perhaps, create the first environment.xml
        if (!ENV.isPersisted()) {
            ENV.persist();
            BeanDefinition.dump();
        }
        ENV.setAutopersist(true);
        
        EntityBrowser entityBrowser = ConcurrentUtil.getCurrent(EntityBrowser.class);
        int entryPoints = 0;
        if (entityBrowser != null)
            entryPoints = createNavigationStartPoint(entityBrowser, root);
        return entryPoints == 0 ? root : entityBrowser.next(null); //if entrypoints are found, root and entrypoints are push to the navigationstack
    }

    private void provideResourceBundelWithAutoTranslation() {
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
    }

    private void initBeanCollectorFormatter(BeanCollector root) {
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
        List<Class> model = Plugins.process(INanoPlugin.class).createBeanContainer(persistence, runtimeClassloader);
        if (model != null)
            return model;

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
            selectedFile = new File(ENV.getConfigPath() + FileUtil.getURIFilePath(jarName));
        }
        jarName = selectedFile.getPath();
        DatabaseTool dbTool = new DatabaseTool(persistence);
        if (!selectedFile.exists() && !isAbsolutePath) {
        	if (dbTool.hasLocalDatabaseFile()
        			&& ! ENV.hasResourceOrFile(persistence.getDatabase() + ".sql")) {
       			runLocalDatabaseAndGenerateEntities(persistence, jarName, dbTool);
        	} else {
            //ant-scripts can't use the nested jars. but normal beans shouldn't have dependencies to simple-xml.
        		generateDatabaseAndEntities(persistence, jarName);
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
        if (dbTool.isLocalDatabase(persistence) && !dbTool.canConnectToLocalDatabase(persistence)) {
            runLocalDatabase(persistence);
        }

        createPersistenceProvider(persistence, runtimeClassloader);

        createLuceneIntegration(persistence);
        
        List<Class> beanClasses =
            runtimeClassloader.loadBeanClasses(jarName,
                ENV.get("bean.class.presentation.regexp", ".*"), null);
        ENV.setProperty("service.loadedBeanTypes", beanClasses);

        return beanClasses;
    }

	private void runLocalDatabaseAndGenerateEntities(final Persistence persistence, String jarName, DatabaseTool dbTool) {
		dbTool.copyJavaDBDriverFiles(persistence);
		if (!dbTool.canConnectToLocalDatabase()) {
			runAntScript(new Properties(), MDA_SCRIPT, "create.db.server.run.file");
			runLocalDatabase(persistence);
		}
		provideJarFileGenerator(persistence);
		generateJarFile(jarName, persistence.getGenerator(), persistence.getDefaultSchema());
	}

	private void createLuceneIntegration(Persistence persistence) {
        if (isH2(persistence.getConnectionUrl()) && ENV.get("app.db.h2.lucene.integration", false)) {
            new H2LuceneIntegration(persistence).activateOnTables();

            //create a view for fulltext search
            String search = null; //TODO
            Query fts = new Query("fulltextsearch", "select * from " + H2LuceneIntegration.createSearchQuery(search), true, null);
            QueryResult<Collection<Object>,Object> queryResult = new QueryResult<>(fts.getName());
            queryResult.saveDefinition();
        }
    }

    protected void createPersistenceProvider(final Persistence persistence, PersistenceClassLoader runtimeClassloader) {
        Message.send("loading persistence provider" + persistence.getGenerator());
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
    }

    protected void generateDatabaseAndEntities(final Persistence persistence, String jarName) throws ManagedException {
        Message.send("generating database and entities: " + jarName);
        ENV.extractResource(JAR_SIMPLEXML);
        ENV.extractResource(JAR_COMMON);
        ENV.extractResource(JAR_DIRECTACCESS);
        ENV.extractResource(JAR_SERVICEACCESS);

        DatabaseTool dbTool = new DatabaseTool(persistence);
        provideScripts(persistence);
        dbTool.copyJavaDBDriverFiles(persistence);

        provideJarFileGenerator(persistence);

        if ((dbTool.isLocalDatabase(persistence) 
        		&& (!dbTool.canConnectToLocalDatabase(persistence) || !dbTool.checkJDBCConnection(false)))
        		|| persistence.autoDllIsCreateDrop()) {
            if (ENV.get("app.db.generate.database", true))
                generateDatabase(persistence);
        }
        if (ENV.get("app.db.check.connection", true))
            dbTool.checkJDBCConnection(true);
        
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
    }

	private void provideJarFileGenerator(final Persistence persistence) {
		String generatorTask;
        if (persistence.getGenerator().equals(Persistence.GEN_HIBERNATE)) {
            generatorTask = "org.hibernate.tool.ant.HibernateToolTask";
        } else {
            generatorTask = "org.apache.openjpa.jdbc.ant.ReverseMappingToolTask";
        }
        ENV.loadClassDependencies("org.apache.tools.ant.taskdefs.Taskdef",
            generatorTask, persistence.getConnectionDriverClass(), persistence.getProvider());
	}

    protected void runLocalDatabase(final Persistence persistence) {
        Message.send("starting local database: " + persistence.getConnectionUrl());
    	if (DatabaseTool.isDBRunInternally() && isH2(persistence.getConnectionUrl())) {
    		// at this moment, only implemented for h2 (hsqldb in future!)
    		new DatabaseTool(persistence).runDBServer();
    		ConcurrentUtil.sleep(1000);
    	} else {
	        String runserverFile = "runServer.cmd";
	        if (!new File(ENV.getConfigPathRel() + runserverFile).exists())
	            generateDatabase(persistence);
	        String[] cmd =
	            AppLoader.isUnix() ? new String[] { "sh", runserverFile } : new String[] { "cmd", "/C", "start",
	                runserverFile };
	        SystemUtil.execute(new File(ENV.getConfigPathRel()), cmd);
    	}
        //prepare shutdown and backup
        DatabaseTool dbTool = new DatabaseTool(persistence);
        dbTool.addShutdownHook();
        dbTool.doPeriodicalBackup();
        addShutdownHookMessageToSessions();
    }

    private void addShutdownHookMessageToSessions() {
        Runtime.getRuntime().addShutdownHook(Executors.defaultThreadFactory().newThread(new Runnable() {
            @Override
            public void run() {
            	for (NanoH5Session s : sessions.values()) {
					s.sendMessage(" === APPLICATION STOPPED! === ");
				}
            }
        }));
	}

	private void provideScripts(Persistence persistence) {
        System.setProperty(HIBREVNAME_TEMPLATE + ".destination", HIBREVNAME);

        //check if an equal named ddl-script is inside our jar file. should be done on 'anyway' or 'timedb'.
        try {
            ENV.extractResource(persistence.getDatabase() + ".sql", false, false, false);
            ENV.extractResource("drop-" + persistence.getDatabase() + ".sql", false, false, false);
            ENV.extractResource("init-" + persistence.getDatabase() + ".sql", false, false, false);
            ENV.extractResource("create-sql-graphviz.cmd", true, true);
            SystemUtil.executeShell(new File(ENV.getConfigPath()), false, "./create-sql-graphviz.cmd");
        } catch (Exception e) {
            LOG.warn(e);
            //ok, it was only a try ;-)
        }
        ENV.extractResource(HIBREVNAME_TEMPLATE);

    }

    private boolean isH2(String url) {
        return DatabaseTool.isH2(url);
    }

    private void generateDatabase(Persistence persistence) {
    	if (DatabaseTool.isDBRunInternally()) {
    		DatabaseTool databaseTool = new DatabaseTool(persistence);
    		if (!databaseTool.isOpen()) {
				databaseTool.runDBServer();
	    		ConcurrentUtil.sleep(1000);
    		}
    	}
        ENV.extractResource(REVERSE_ENG_SCRIPT);
        Message.send("creating new database " + persistence.getDatabase() + " for url "
            + persistence.getConnectionUrl());
        Properties p = new Properties();
        ENV.setProperty("app.doc.name", persistence.getDatabase());
        if (!FileUtil.userDirFile(persistence.getDatabase() + ".sql").exists() && ENV.isModeStrict())
            throw new IllegalStateException("please provide the database script file '" + persistence.getDatabase() + ".sql' inside the environment directory");

        //give mda.xml the information to don't start nano.h5
        p.put("nano.h5.running", "true");
        runAntScript(p, MDA_SCRIPT, "do.all");
        Plugins.process(INanoPlugin.class).databaseGenerated(Persistence.current());
    }

	private static Boolean runAntScript(Properties p, String script, String target) {
		p.put("basedir", ENV.getConfigPath());
        return (Boolean) ENV.get(CompatibilityLayer.class).runRegistered("ant",
            ENV.getConfigPath() + script,
            target,
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
        properties.setProperty("plugin.dir", FileUtil.userDirFile(plugin_dir).getAbsolutePath());
        if (plugin_dir.endsWith(".jar/")) {
            properties.setProperty("plugin_isjar", Boolean.toString(true));
        } 
        Message.send("starting generation of '" + jarFile + "' through script " + REVERSE_ENG_SCRIPT);
        //If no environment was saved before, we should do it now!
        ENV.persist();

        Boolean result = runAntScript(properties, REVERSE_ENG_SCRIPT, "create.bean.jar");
        if (result != null && result)
            Plugins.process(INanoPlugin.class).beansGenerated(Persistence.current());
        return result;
    }

    private String applicationHtmlFile() {
        return ENV.getTempPath() + START_HTML_FILE;
    }

    @Override
    public void stop() {
    	LOG.info("===> NANOH5 SERVER SHUTDOWN! <===");
    	if (!ENV.isTestMode())
    		clear();
        super.stop();
        LogFactory.stop();
        if (ENV.get("app.stop.allow.system.exit", !ENV.isTestMode() && !SystemUtil.isNestedApplicationStart())) {
        	FileUtil.writeBytes(("System.exit(0) called on: " + this.toString()).getBytes(), "systemexit.txt", false);
        	LOG.info("===> SYSTEM EXIT!");
        	ConcurrentUtil.sleep(5000);
        	System.exit(0);
        } else
        	//SystemUtil.softExitOnCurrentThreadGroup(null, ENV.get("app.softexit.runhooks", false));
        	new DatabaseTool(Persistence.current()).shutdownDBServer();
    }

    @Override
    public void reset() {
        String configPath = clear();
        HtmlUtil.reset();
        
        ENV.reload();
        ENV.setProperty(ENV.KEY_CONFIG_PATH, configPath);
        ENV.setProperty("service.url", serviceURL.toString());
        BeanClass.callStatic("de.tsl2.nano.core.classloader.NetworkClassLoader", "resetUnresolvedClasses", ENV.getConfigPath());
        Thread.currentThread().setContextClassLoader(appstartClassloader);

        createPageBuilder();
        builder = ENV.get(IPageBuilder.class);
    }

    private String clear() {
        String configPath = ENV.get(ENV.KEY_CONFIG_PATH, "config");

        accept(new ChangeEvent("app.configuration.persist.yaml", null, ENV.get("app.configuration.persist.yaml", false)));

        ConcurrentUtil.removeAllCurrent(NanoH5Session.getThreadLocalTypes());

        DatabaseTool databaseTool = new DatabaseTool(Persistence.current());
        if (databaseTool.isInternalDatabase())
        	databaseTool.shutdownDBServer();
        
        //TODO: the following is done in pagebuilder.reset, too?
        ENV.addService(ClassLoader.class, appstartClassloader);
        ENV.removeService(Workflow.class);
        ENV.removeService(Persistence.class);
        ENV.removeService(BeanConfigurator.class);
        if (ENV.get(UncaughtExceptionHandler.class) instanceof ExceptionHandler)
        	((ExceptionHandler)ENV.get(UncaughtExceptionHandler.class)).clearExceptions();
        ENV.removeService(UncaughtExceptionHandler.class);
        createExceptionhandler();
        ENV.get(Pool.class).reset();
        BeanContainer.reset();
        ENV.removeService(IBeanContainer.class);
        BeanContainerUtil.clear();
        Bean.clearCache();
        AttributeCover.resetTypeCache();
        lastRequest = null;
        requests = 0;
        new HashMap<Object, NanoH5Session>(sessions).forEach( (id, session) -> session.close());
        sessions.clear();

        IPageBuilder pageBuilder = ENV.get(IPageBuilder.class);
        if (pageBuilder != null)
            pageBuilder.reset();
        else
            new Html5Presentation<>().reset();
        return configPath;
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
        return Util.toString(NanoH5.class, ENV.getName(), "serviceURL: " + serviceURL,
            "sessions: " + (sessions != null ? sessions.size() : 0), "requests: " + requests);
    }

    class Request {
        String uri;
        Method m;
        Map<String, String> header;
        Map<String, String> parms;
        Map<String, String> files;
        InetAddress requestor;
        Request(InetAddress requestor, String uri,
                Method m,
                Map<String, String> header,
                Map<String, String> parms,
                Map<String, String> files) {
            super();
            this.requestor = requestor;
            this.uri = uri;
            this.m = m;
            this.header = header;
            this.parms = parms;
            this.files = files;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof Request))
                return false;
            Request o = (Request) obj;
            return Util.equals(requestor, o.requestor) && Util.equals(uri, o.uri) && Util.equals(m, o.m) 
                    && header.equals(o.header) && parms.equals(o.parms) && files.equals(o.files);
        }
    }

    public void removeSession(NanoH5Session session) {
        sessions.remove(session.getKey());
        getEventController().removeListener(session);

        if (sessions.isEmpty() && requests > 0) {
            Message.send("All sessions closed -> resetting BeanContainer / GenericService / NetworkClassloader!");
            reset();
            new UnboundAccessor(rootClassloader()).call("reset", Void.class);
            BeanContainerUtil.resetServices();
            ENV.removeService(IGenericService.class);
        }
    }

	@Override
	public void accept(ChangeEvent t) {
		if (t.getSource().equals("*") || (t.getSource().equals("app.configuration.persist.yaml") && (boolean)t.newValue)) {
			createYAMLFiles();
		}
	}

	public synchronized void createYAMLFiles() {
		String originalFileExtension = ".xml";
		ENV.persist(Users.load());
		if (ENV.get(IAuthorization.class) != null)
			ENV.persist(ENV.get(IAuthorization.class)); //TODO: that's only the current user!
		ENV.persist(Persistence.current());
		Collection<Class> beantypes = (Collection<Class>) ENV.get("service.loadedBeanTypes");
		if (!Util.isEmpty(beantypes) && beantypes.iterator().next() instanceof Class) { //sometimes Collection<String>...
			for (Class c: beantypes) {
				BeanDefinition.getBeanDefinition(c).saveDefinition();
			}
		}
		Collection<BeanDefinition<?>> virtualDefinitions = BeanDefinition.loadVirtualDefinitions(originalFileExtension);
		virtualDefinitions.forEach(vd -> vd.saveDefinition() );
		
		// TODO: save all context files - use ENV.save(..) in Context
		
		ENV.get(Pool.class).saveAll();

		// deactivate the 
		if (ENV.get("app.configuration.persist.yaml", false)) {
			try {
				String envXml = ENV.getConfigPath() + ENV.CONFIG_NAME + originalFileExtension;
				Files.move(Paths.get(envXml), Paths.get(envXml + "_"));
			} catch (IOException e) {
                LOG.error(e);
			}
		}
	}
}
