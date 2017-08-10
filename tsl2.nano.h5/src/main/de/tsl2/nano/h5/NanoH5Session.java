/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 11.10.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.h5;

import static de.tsl2.nano.bean.def.BeanPresentationHelper.KEY_FILTER_FROM_LABEL;
import static de.tsl2.nano.bean.def.BeanPresentationHelper.KEY_FILTER_TO_LABEL;
import static de.tsl2.nano.bean.def.IBeanCollector.MODE_SEARCHABLE;
import static de.tsl2.nano.h5.HtmlUtil.BTN_ASSIGN;
import static de.tsl2.nano.h5.HtmlUtil.BTN_CANCEL;
import static de.tsl2.nano.h5.HtmlUtil.BTN_SUBMIT;
import static de.tsl2.nano.h5.HtmlUtil.beanID;
import static de.tsl2.nano.h5.NanoH5.OFFSET_FILTERLINES;
import static de.tsl2.nano.h5.NanoHTTPD.MIME_HTML;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.java_websocket.WebSocket;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.IBeanCollector;
import de.tsl2.nano.bean.def.IBeanFinder;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.MethodAction;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.MapEntrySet;
import de.tsl2.nano.core.Context;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.Main;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.exception.ExceptionHandler;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.XmlUtil;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.h5.NanoHTTPD.Method;
import de.tsl2.nano.h5.NanoHTTPD.Response;
import de.tsl2.nano.h5.NanoHTTPD.Response.Status;
import de.tsl2.nano.h5.configuration.BeanConfigurator;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.h5.navigation.Parameter;
import de.tsl2.nano.h5.websocket.NanoWebSocketServer;
import de.tsl2.nano.h5.websocket.WebSocketExceptionHandler;
import de.tsl2.nano.messaging.EMessage;
import de.tsl2.nano.messaging.IListener;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;
import de.tsl2.nano.util.operation.IRange;

/**
 * user session for nano.h5 server
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class NanoH5Session implements ISession<BeanDefinition>, Serializable, IListener<EMessage> {
    /** serialVersionUID */
    private static final long serialVersionUID = -8299446546343086394L;

    private static final Log LOG = LogFactory.getLog(NanoH5Session.class);

    transient String id;
    transient NanoH5 server;
    transient IPageBuilder<?, String> builder;
    /** workflow or bean navigator */
    transient IBeanNavigator nav;
    /** html response */
    transient Response response;
    /** concatencation of database-name+schema+beans.jar */
    Context context;

    /** sessions classloader */
    transient ClassLoader sessionClassloader;
    /** requests user internet adress */
    transient InetAddress inetAddress;

    /** port of websocket, if used */
    transient int websocketPort;

    /** sessions exceptionHandler */
    transient ExceptionHandler exceptionHandler;

    /** logs all user actions to be given on error-handling */
    transient List<String> actionLog;

    /** for profiling in status-line (current work-time of last request) */
    transient long startTime;

    /** session start */
    private long sessionStart;

    /** session access */
    private long lastAccess;
    transient int requests;

    transient private IAuthorization authorization;
    transient private BeanContainer beanContainer;
    transient private BeanConfigurator beanConfigurator;

    public static final String PREFIX_STATUS_LINE = "@";

    public static final String PREFIX_CONTEXT_RANGE = "range:";

//    public static final String KEY_WEBSOCKET_PORT = "websocket.port";

    public static final NanoH5Session createSession(NanoH5 server,
            InetAddress inetAddress,
            IBeanNavigator navigator,
            ClassLoader appstartClassloader,
            IAuthorization authorization,
            Map context) {
        //TODO: respect the bean-set (bean-jar-file!)
        File storedSessionFile = getSessionFile(authorization);
        NanoH5Session session = null;
        if (storedSessionFile != null && storedSessionFile.exists()) {
            try {
                //try to load a temp stored session
                session = ENV.get(XmlUtil.class).loadXml(storedSessionFile.getPath(), NanoH5Session.class);
                session.init(server, inetAddress, navigator, appstartClassloader, authorization, context);
            } catch (Exception e) {
                //on error we create a new session object
                LOG.error(e);
            }
        }
        if (session == null) {
            session = new NanoH5Session(server, inetAddress, navigator, appstartClassloader, authorization, context);
        }
        return session;
    }

    private static File getSessionFile(IAuthorization authorization) {
        return authorization != null ? new File(ENV.getTempPathRel() + "session-" + authorization.getUser()) : null;
    }

    /**
     * constructor
     * 
     * @param server
     * @param inetAddress
     * @param navigation
     * @param appstartClassloader
     * @param authorization
     */
    protected NanoH5Session(NanoH5 server,
            InetAddress inetAddress,
            IBeanNavigator navigator,
            ClassLoader appstartClassloader,
            IAuthorization authorization,
            Map context) {
        super();
        init(server, inetAddress, navigator, appstartClassloader, authorization, context);
    }

    /**
     * init
     * 
     * @param server
     * @param inetAddress
     * @param navigator
     * @param appstartClassloader
     * @param authorization
     * @param context
     */
    void init(NanoH5 server,
            InetAddress inetAddress,
            IBeanNavigator navigator,
            ClassLoader appstartClassloader,
            IAuthorization authorization,
            Map context) {
        this.server = server;
        this.server.getEventController().addListener(this);
        this.inetAddress = inetAddress;
        this.builder = server.builder;
        this.nav = navigator;
        this.sessionClassloader = appstartClassloader;
        createExceptionHandler();
        this.actionLog = new LinkedList<>();
        this.authorization = authorization;
        initContext(authorization, context);
        this.sessionStart = System.currentTimeMillis();
        Persistence p = Persistence.current();
        this.id = inetAddress + p.getConnectionUrl() + "." + p.getConnectionUserName() + "." + p.getJarFile();

    }

    /**
     * initContext
     * 
     * @param authorization
     * @param context
     */
    void initContext(IAuthorization authorization, Map context) {
        this.context = Context.create(authorization != null ? authorization.getUser().toString() : null, true);
        this.context.putAll(context);
    }

    /**
     * injectFromContext
     * 
     * @param beandefs
     * @param type
     * @param range
     * @param i
     */
    BeanDefinition injectContext(BeanDefinition beandef) {
        //inject search filter only on first time - before first search...
        if (beandef != null && beandef.isMultiValue() && !((BeanCollector) beandef).wasActivated()) {
            //fill search parameters...
            Iterator<IRange> ranges = this.context.get(IRange.class);
            Class type;
            IRange range = null;
            while (ranges.hasNext()) {
                range = ranges.next();
                type = range.getFrom() != null ? range.getFrom().getClass() : null;
                IBeanFinder beanFinder;
                if (beandef.getDeclaringClass().equals(type)) {
                    beanFinder = ((BeanCollector) beandef).getBeanFinder();
                    if (beanFinder != null) {
                        Bean brange = beanFinder.getFilterRange();
                        brange.getAttribute("from").setValue(BeanContainer.attachEntities(range.getFrom()));
                        brange.getAttribute("to").setValue(BeanContainer.attachEntities(range.getTo()));
                        break;
                    }
                }
            }
        }
        return beandef;
    }

    /**
     * createExceptionHandler
     */
    private void createExceptionHandler() {
        if (ENV.get("websocket.use", true)) {
            final NanoWebSocketServer socketServer =
                new NanoWebSocketServer(this, createSocketAddress());
            websocketPort = socketServer.getPort();
            this.exceptionHandler =
                (ExceptionHandler) ENV.addService(UncaughtExceptionHandler.class,
                    new WebSocketExceptionHandler(socketServer));
            socketServer.start();

            Runtime.getRuntime().addShutdownHook(Executors.defaultThreadFactory().newThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Set<WebSocket> sockets = socketServer.connections();
                        for (WebSocket webSocket : sockets) {
                            webSocket.send(" === APPLICATION STOPPED! === ");
//                            Message.send("APPLICATION STOPPED!");

                        }
                        socketServer.stop();
                    } catch (Exception e) {
                        LOG.error(e);
                    }
                }
            }));
        } else {
            this.exceptionHandler =
                (ExceptionHandler) ENV.addService(UncaughtExceptionHandler.class, new ExceptionHandler());
        }
        Thread.currentThread().setUncaughtExceptionHandler(exceptionHandler);
        ConcurrentUtil.setCurrent(exceptionHandler);
    }

    private InetSocketAddress createSocketAddress() {
        URL url = NanoH5.getServiceURL(null);
        //workaround - see doc of NetUtil.getFreePort()
        return new InetSocketAddress(url.getHost(), NetUtil.getFreePort());
    }

    /**
     * setAuthorization
     * 
     * @param authorization
     */
    @Override
    public void setUserAuthorization(Object authorization) {
        this.authorization = (IAuthorization) authorization;
        initContext((IAuthorization) authorization, context);
    }

    void setBeanContainer(BeanContainer beanContainer) {
        this.beanContainer = beanContainer;
    }

    /**
     * main session serve method. requests of type 'GET' and file-links are handled by the application class (NanoH5).
     * 
     * @param uri url, name of a bean, or number of selected item in a beancollector
     * @param method 'POST'
     * @param header request header
     * @param parms request
     * @param files
     * @return html response
     */
    public Response serve(String uri,
            String method,
            Map<String, String> header,
            Map<String, String> parms,
            Map<String, String> files) {
        String msg = "[undefined]";
        ManagedException ex = null;
        try {
            //refresh session values on the current thread
            assignSessionToCurrentThread(true, MapUtil.filter(header, "User-Agent"));

            if (LOG.isDebugEnabled()) {
                LOG.debug(
                    String.format("serving request:\n\turi: %s\n\tmethod: %s\n\theader: %s\n\tparms: %s\n\tfiles: %s",
                        uri,
                        method,
                        header,
                        parms,
                        files));
            } else {
                LOG.info(String.format("serving request " + requests + " : uri: %s, method: %s, %s, parms: %s",
                    uri,
                    method,
                    MapUtil.filter(header, "http-client-ip", "User-Agent"),
                    parms));
            }
            //WORKAROUND for uri-problem
            String referer = header.get("referer");
            if (parms.containsKey(IAction.CANCELED)
                || (method.equals("POST") && referer != null && uri.length() > 1 && referer.contains(uri))) {
                uri = "/";
            }
            //extract bean-specific prefix
            BeanDefinition<?> linkToModel = nav.fromUrl(uri);
            Object userResponse = null;
            /*
             * uri:
             * - file-system links is already handled in parent class.
             * - direct link to another page/bean
             * - selection-link-number in beancollector
             * - bean action of Html5PresentationHelper
             */
            Number uriLinkNumber = linkToModel != null ? null : NumberUtil.extractNumber(uri.substring(1));
            //form-button clicked - or first page
            if (!parms.isEmpty() || linkToModel != null || uriLinkNumber != null || response == null
                || uri.contains(Html5Presentation.PREFIX_BEANREQUEST)) {
                if (linkToModel != null) {
                    userResponse = linkToModel;
                } else {
                    userResponse = processInput(uri, parms, uriLinkNumber);
                }
                if (userResponse instanceof String && !userResponse.equals(IAction.CANCELED)) {
                    msg = (String) userResponse;
                    if (HtmlUtil.isURI(msg)) {
                        return server.serve(msg, Method.GET, header, parms, files);
                    } else if (!HtmlUtil.containsHtml(msg)) {
                        msg = HtmlUtil.createMessagePage(ENV.translate("tsl2nano.info", true), msg);
                    }
                } else {
//                    if (!exceptionHandler.hasExceptions()) {
//                    }
                    msg = getNextPage(userResponse);
                }
                response = server.createResponse(msg);
            } else {
                close();
                return server.createResponse(Html5Presentation.createMessagePage("start.template",
                    ENV.getName() + "<br/>" + "Restart Session", server.serviceURL));
            }
        } catch (Throwable e /*respect errors like NoClassDefFound...the application should continue!*/) {
            LOG.error(e);
            ex = new ManagedException(e) {
                /** serialVersionUID */
                private static final long serialVersionUID = 1L;

                @Override
                public String getMessage() {
                    return super.getMessage() + "\n\nAction-Stack:\n"
                        + StringUtil.toFormattedString(actionLog, 1000, true);
                }
            };
            msg = refreshPage(ex);
            response = server.createResponse(Status.BAD_REQUEST, MIME_HTML, msg);
            actionLog.clear();
            //don't forget that there was an exception. to be seen on the next exception ;-)
            logaction(ex.toString(), parms);
            Message.broadcast(this,
                ENV.translate("nanoh5.error", false, this.getUserAuthorization().getUser(), 
                    nav.current(), e.getLocalizedMessage()),
                "*");
        }
        //TODO: eliminate bug in NanoHTTPD not resetting uri...
//        header.clear();
//        response.header.remove(uri);
        Message.send(exceptionHandler, createStatusText(startTime));
        if (ex != null)
            Message.send(exceptionHandler, ex.toString());
        return response;
    }

    /**
     * assignSessionToCurrentThread
     */
    public void assignSessionToCurrentThread(boolean newRequest, Map properties) {
        if (newRequest) {
            lastAccess = System.currentTimeMillis();
            requests++;
            if (nav.current() instanceof Bean && ((Bean) nav.current()).getInstance() instanceof BeanConfigurator) {
                this.beanConfigurator = (BeanConfigurator) ((Bean) nav.current()).getInstance();
            }
            if (properties != null && !properties.isEmpty()) {
                ConcurrentUtil.setCurrent(properties);
            }
        }
        Thread.currentThread().setContextClassLoader(sessionClassloader);
        Thread.currentThread().setUncaughtExceptionHandler(exceptionHandler);
        ConcurrentUtil.setCurrent(getUserAuthorization(), beanContainer, beanConfigurator);
    }

    /**
     * isMobile
     * @return true, if client request comes from mobile device
     */
    public boolean isMobile() {
        Map props = ConcurrentUtil.getCurrent(Map.class);
        if (props == null)
            return false;
        Object userAgent = props.get("User-Agent");
        return userAgent != null && userAgent.toString().contains("Mobile");
    }
    
    /**
     * defines, which values have to be provided to the current thread. each thread lives only for one request! this is
     * needed because the app instance provides most of this values and is open for all sessions.
     * 
     * @return types to be provided to the current thread
     */
    static final Class[] getThreadLocalTypes() {
        return new Class[] { BeanContainer.class, Authorization.class, BeanConfigurator.class, ExceptionHandler.class };
    }

    @Override
    public void close() {
        LOG.debug("closing session " + this);
        server.sessions.remove(inetAddress);
        server.getEventController().removeListener(this);
        nav = null;
        response = null;
        authorization = null;
        beanContainer = null;
        builder = null;
        sessionClassloader = null;
        if (exceptionHandler instanceof WebSocketExceptionHandler) {
            try {
                ((WebSocketExceptionHandler) exceptionHandler).close();
            } catch (IOException e) {
                ManagedException.forward(e);
            }
        }
        exceptionHandler = null;
        context = null;
        ConcurrentUtil.removeCurrent(getThreadLocalTypes());
//        server = null;
    }

    String createStatusText(long startTime) {
        String user =
            authorization != null ? ENV.translate("tsl2nano.login.user", true) + ": "
                + authorization.getUser() + "§"
                + StringUtil.toHexString((getUserAuthorization() + "\n" + "RequestID: " + requests).getBytes())
                + ", " + "Online: "
                + DateUtil.getFormattedMinutes(getDuration()) + " min, " : "";
        return PREFIX_STATUS_LINE + user
            + ENV.translate("tsl2nano.time", true)
            + ": " + DateUtil.getFormattedDateTime(new Date()) + ", "
            + (nav != null ? ENV.translate("tsl2nano.session", true)
                + ": " + ENV.translate(nav.getName(), true) + "§"
                + StringUtil.toHexString(getContext().toString().getBytes()) : "")
            + ", "
            + ENV.translate("tsl2nano.request", true) + ": "
            + DateUtil.seconds(System.currentTimeMillis() - startTime) + " msec"
            + (LOG.isDebugEnabled() ? ", " + "Memory: " + (Profiler.getUsedMem() / (1024 * 1024)) + " MB" : "")
            + (LOG.isDebugEnabled() ? ", " + "working sessions: " + server.sessions.size() : "");
    }

    private String refreshPage(Object message) {
        return builder.build(this, nav.current(), message, true, nav.toArray());
    }

    /**
     * pops the next model from navigation stack (see {@link #getNextModel(Object)} and asks the pagebuilder to create a
     * full html page for the given model.
     * 
     * @param returnCode return code of last request
     * @return html string
     */
    private String getNextPage(Object returnCode) {
        String msg = "";
        if (exceptionHandler.hasExceptions()) {
            msg = StringUtil.toFormattedString(exceptionHandler.clearExceptions(), 200, false);
        }
        BeanDefinition<?> model = injectContext(nav.next(returnCode));
        if (model != null)
            model.onActivation(getContextParameter());
        return model != null ? builder.build(this, model, msg, true, nav.toArray()) : server.createStartPage();
    }

    /**
     * process user input
     * 
     * @param uri page uri
     * @param parms response parameter
     * @param uriLinkNumber if navigator.current() is a bean-collector, it is the selected element number
     * @return user response object. may be {@link IAction#CANCELED} any saved or selected object or null.
     */
    private Object processInput(String uri, Map<String, String> parms, Number uriLinkNumber) {
//        if (parms.containsKey(PageBuilder.COMMAND_RESTART)) {
//            stop();
//            main(null);
//        }
        convertIDs(parms);
        Object responseObject = null;
        if (parms.containsKey(IAction.CANCELED)) {
            logaction(IAction.CANCELED, null);
            if (nav.current() != null) {
                ((BeanDefinition) nav.current()).onDeactivation(getContextParameter());

                //perhaps remove configuration bean
                BeanConfigurator configurator = ConcurrentUtil.getCurrent(BeanConfigurator.class);
                if (configurator != null
                    && ((BeanDefinition) nav.current()).getDeclaringClass().equals(BeanConfigurator.class)) {
                    ConcurrentUtil.removeCurrent(BeanConfigurator.class);
                }
            }
            return IAction.CANCELED;
        }

        convertDates(parms);

        refreshCurrentBeanValues(parms);

        if (nav.current() instanceof Controller) {
            Controller ctrl = (Controller) nav.current();
            String actionName = (String) parms.keySet().iterator().next();
            if (actionName != null && actionName.startsWith(Controller.PREFIX_CTRLACTION)) {
                return ctrl.doAction(actionName, getContextParameter());
            }
        }
        //follow links or fill selected items
        if (nav.current() instanceof BeanCollector) {
            //follow given link
            if (uriLinkNumber != null) {
                BeanCollector collector = (BeanCollector) nav.current();
                Collection data = collector.getCurrentData();
//                if (data.isEmpty())
//                    
                ListSet listSet = CollectionUtil.asListSet(data);
                Object selectedItem =
                    listSet.get(uriLinkNumber.intValue()
                        - (ENV.get("layout.grid.searchrow.show", true) && collector.hasMode(MODE_SEARCHABLE)
                            && collector.hasFilter() ? 2 : 0));
                boolean isTypeList = BeanCollector.class.isAssignableFrom(collector.getClazz());
                responseObject = isTypeList ? selectedItem : Bean.getBean((Serializable) selectedItem);
                return responseObject;
            } else {
                if (!isCanceled(parms)
                    && (/*isNewAction(parms, (BeanCollector) nav.current()) || */provideSelection(
                        (BeanCollector) nav.current(), parms))) {
                    if (isReturn(parms)) {
                        responseObject = null;
                    } else if (isOpenAction(parms, (BeanCollector) nav.current())) {
                        //normally, after a selection the navigation object will be hold on stack
                        if (ENV.get("app.edit.multiple", true)) {
                            responseObject = putSelectionOnStack((BeanCollector) nav.current());
                        } else {
                            responseObject = nav.current();
                        }
                    }
                }
            }
        } else if (nav.current() instanceof Bean) {//detail bean
            //on database models with composite-ids, these ids should be synchronized with standard values.
            Bean bean = (Bean) nav.current();
            if (bean.isPersistable()) {
                BeanContainerUtil.synchronizeEmbeddedCompositeID((Serializable) bean.getInstance());
            }
        }
        //collect available actions
        Collection<IAction> actions = null;
        if (nav.current() != null) {
            BeanDefinition<?> c = nav.current();
            actions = new ArrayList<IAction>();
            if (c.getActions() != null) {
                actions.addAll(c.getActions());
            }
            actions.addAll(c.getPresentationHelper().getPageActions(this));
            actions.addAll(c.getPresentationHelper().getSessionActions(this));
            actions.addAll(c.getPresentationHelper().getApplicationActions(this));
            if (c.isMultiValue() && c instanceof BeanCollector) {
                actions.addAll(((BeanCollector) c).getColumnSortingActions());
                actions.add(((BeanCollector) c).getQuickSearchAction());
            }
            //start the actions
            //respect action-call through menu-link (with method GET but starting with '!!!'
            Set<Object> keySet = new HashSet<Object>();
            if (uri.contains(Html5Presentation.PREFIX_ACTION)) {
                keySet.add(StringUtil.substring(uri, Html5Presentation.PREFIX_ACTION, null));
            }
            keySet.addAll(parms.keySet());
            for (Object k : keySet) {
                String p = (String) k;
                IAction<?> action = getAction(actions, p);
                if (action != null) {
                    logaction(action, parms);
                    //send this information to the client to show a progress bar.
                    Message.send("submit");
                    Message.send(ENV.translate("tsl2nano.starting", true) + " "
                        + action.getShortDescription() + " ...");
                    if (c.isMultiValue() && action.getId().endsWith(BeanCollector.POSTFIX_QUICKSEARCH)) {
                        action.setParameter(parms.get(Html5Presentation.ID_QUICKSEARCH_FIELD));
                        responseObject = action.activate();
                    } else if (c.isMultiValue() && c instanceof BeanCollector
                        && isSearchRequest(action.getId(), (BeanCollector<?, ?>) c)) {
                        responseObject = processSearchRequest(parms, (BeanCollector<?, ?>) c);
                    } else {
                        /*
                         * submit/assign and cancel will not push a new element to the navigation stack!
                         * TODO: refactore access to names ('reset' and 'save')
                         */
                        if (action.getParameter() == null) {
                            action.setParameter(getContextParameter());
                        }
                        Object result;
                        //on parametrized actions provide a new detail dialog/page
                        if (!Util.isEmpty(action.getArgumentTypes())) {
                            if (!nav.current().getName().equals(action.getShortDescription())) {//define the arguments
                                if (action instanceof MethodAction) {
                                    result = ((MethodAction)action).toBean();
                                } else {
                                    Map<String, Object> args = MapUtil.fromKeys(MethodAction.getArgumentNames(action));
                                    //TODO: extend the BeanCollector to use @Constraint of each argument (=row)
                                    BeanDefinition bean = BeanCollector.getBeanCollector(Util.getContainer(args), 0);
                                    bean.setName(action.getShortDescription());
                                    bean.addAction(action);
                                    result = bean;
                                }
                            } else {//set the arguments and start the parametrized action
                                Object[] values = null;
                                if (nav.current() instanceof BeanCollector) {
                                    MapEntrySet argSet = (MapEntrySet) ((BeanCollector) nav.current()).getCurrentData();
                                    values = argSet.map().values().toArray();
                                } else {
                                    values = nav.current().toValueMap(null).values().toArray();
                                }
                                Object[] args = action.getParameter() != null ? CollectionUtil.concat(Arrays.copyOfRange(action.getParameter(), 0, 1),
                                    values) : values;
                                action.setParameter(args);
                                result = action.activate();
                            }
                        } else {
                            result = action.activate();
                            if (action.getId().endsWith(".save") || action.getId().endsWith(".delete"))
                                Message.broadcast(this,
                                    ENV.translate("tsl2nano.value.changed", false,
                                        nav.current().getName() + ": " + nav.current(),
                                        this.getUserAuthorization().getUser(), action.getShortDescription()),
                                    "*");
                        }

                        /*
                         * if action is asynchron, it's a long term action showing the same page again
                         * with progress informations
                         */
//                        if (action.isSynchron())
//                            throw new Message("starting long term request:\t" + action.getShortDescription());
                        if (result != null && responseObject != IAction.CANCELED && !action.getId().endsWith("save")) {
                            responseObject = result;
                            if (c instanceof Bean
                                && ((Bean) c).getInstance() instanceof Persistence) {
                                setUserAuthorization(ConcurrentUtil.getCurrent(Authorization.class));
                                setBeanContainer(ConcurrentUtil.getCurrent(BeanContainer.class));
                            }
                        } else if (action.getId().endsWith("reset")) {
                            responseObject = c;
                        } else {
//                            action.activate();
                            return responseObject;
                        }
                    }
                    break;
                } else {
                    if (p.endsWith(IPresentable.POSTFIX_SELECTOR)) {
                        logaction(p, null);
                        String n = StringUtil.substring(p, null, IPresentable.POSTFIX_SELECTOR);
                        final BeanValue assignableAttribute = (BeanValue) c.getAttribute(n);
                        responseObject = assignableAttribute.connectToSelector(c);
                        break;
                    }
                }
            }
        }
        return responseObject;
    }

    private void convertIDs(Map<String, String> parms) {
        Set<String> keys = parms.keySet();
        for (String k : keys) {
            parms.put(k, beanID(parms.get(k)));
        }
    }

    /**
     * context parameters will be evaluated from context beans. these beans may have references to other beans. so the
     * context beans have to be in the right order.
     * 
     * @return context parameters
     */
    private Parameter getContextParameter() {
        Collection con = getContext().values();
        Parameter p = new Parameter();
        LOG.debug("filling context for session: " + this);
        //full beans
        BeanDefinition c;
        for (Object v : con) {
            c = Bean.getBean(v);
            p.put(c.getName(), c);
        }
        //all bean attributes
        for (Object v : con) {
            c = Bean.getBean(v);
            p.putAll(c.toValueMap(p));
        }
        //do that twice to let rules and queries use defined parameter
        LOG.debug("second iteration on context for session: " + this);
        for (Object v : con) {
            c = Bean.getBean(v);
            p.putAll(c.toValueMap(p));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("session:" + this + "\n\tcontext parameters: " + p.keySet());
        }
        return p;
    }

    @SuppressWarnings("static-access")
    protected void addContextObject(BeanDefinition<?> object) {
        ((Collection) this.context).add(object);
        ENV.get(XmlUtil.class).saveXml(getSessionFile(authorization).getPath(), this);
    }

    @SuppressWarnings("static-access")
    protected boolean removeContextObject(BeanDefinition<?> object) {
        boolean result = ((Collection) this.context).remove(object);
        if (result)
            ENV.get(XmlUtil.class).saveXml(getSessionFile(authorization).getPath(), this);
        return result;
    }

    private void logaction(IAction<?> action, Map<String, String> parameter) {
        logaction(action.getId(), parameter);
    }

    private void logaction(String id, Map<String, String> p) {
        actionLog.add(DateUtil.getFormattedTimeStamp() + " ==> " + id + " (" + nav.current() + ")"
            + (!Util.isEmpty(p) ? "\n\t" + p : ""));
    }

    /**
     * refreshCurrentBeanValues
     * 
     * @param parms
     */
    private void refreshCurrentBeanValues(Map<String, String> parms) {
        LOG.info("refreshing current bean values");
        if (nav.current() instanceof Bean) {
            Collection<Exception> exceptions = new LinkedList<Exception>();
            Bean vmodel = (Bean) nav.current();
            for (String p : parms.keySet()) {
                if (vmodel.hasAttribute(p)) {
                    try {
                        /*
                         * check, if input was changed - so, don't lose instances if unchanged
                         * the oldString was sent to html-page - the newString returns from request
                         */
                        BeanValue bv = (BeanValue) vmodel.getAttribute(p);
                        Class<?> type = bv.getType();
                        /*
                         * if the type is object, the bean doesn't know exactly it's real type, so
                         * we assume it should be serializable...
                         */
                        if (!type.isPrimitive() && !Serializable.class.isAssignableFrom(type)
                            && !Object.class.isAssignableFrom(type)) {
                            LOG.debug("ignoring not-serializable attribute " + vmodel.getAttribute(p));
                            continue;
                        }
                        String oldString = bv.getValueText();
                        String newString = parms.get(p);
                        // checkboxes will send 'on' as value
                        if (boolean.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type))
                            if (newString.equals("on"))
                                newString = "true";
                        if (Date.class.isAssignableFrom(type))
                            if (newString.matches("\\d{2,2}[:]\\d{2,2}"))
                                newString += ":00"; //append 0 seconds to respect format HH:mm:ss
                        
                        if (oldString == null || !oldString.equals(newString)) {
                            vmodel.setParsedValue(p, newString);
                        } else {
                            LOG.debug("ignoring unchanged attribute " + vmodel.getAttribute(p));
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                }
            }
            /*
             * create one exception, holding all messages of thrown sub-exceptions
             */
            if (exceptions.size() > 0) {
                StringBuffer buf = new StringBuffer();
                for (Exception ex : exceptions) {
                    buf.append(ex.getMessage() + "\n");
                }
                throw new ManagedException(buf.toString(), exceptions.iterator().next());
            }
        }
    }

    private BeanDefinition<?> putSelectionOnStack(BeanCollector c) {
        Collection selection = (Collection) c.getSelectionProvider().getValue();
        BeanDefinition<?> firstElement = null;
        BeanDefinition<?> bean;
        for (Object object : selection) {
            bean =
                (BeanDefinition<?>) (object instanceof BeanDefinition ? object : Bean.getBean((Serializable) object));
            //don't add the first element, see behaviour in getNextModel()
            if (firstElement != null) {
                nav.add(bean);
            } else {
                firstElement = bean;
            }
        }
        return firstElement;
    }

    private IAction<?> getAction(Collection<IAction> actions, String id) {
        if (actions != null) {
            for (IAction a : actions) {
                if (a.getId().trim().equals(id)) {
                    return a;
                }
            }
        }
        return null;
    }

    protected boolean isCanceled(Map<String, String> parms) {
        return parms.containsKey(BTN_CANCEL);
    }

    protected boolean isReturn(Map<String, String> parms) {
        return isCanceled(parms) || parms.containsKey(BTN_ASSIGN) || parms.containsKey(BTN_SUBMIT);
    }

    protected <T> boolean isNewAction(Map<String, String> parms, BeanCollector<?, T> model) {
        for (Object k : parms.keySet()) {
            if (isNewAction((String) k, model)) {
                return true;
            }
        }
        return false;
    }

    protected <T> boolean isNewAction(String actionId, BeanCollector<?, T> model) {
        return actionId.equals(BeanContainer.getActionId(model.getClazz(), true, "new"));
    }

    protected <T> boolean isOpenAction(Map<String, String> parms, BeanCollector<?, T> model) {
        for (Object k : parms.keySet()) {
            if (isOpenAction((String) k, model)) {
                return true;
            }
        }
        return false;
    }

    protected <T> boolean isOpenAction(String actionId, BeanCollector<?, T> model) {
        return actionId.equals(BeanContainer.getActionId(model.getClazz(), true, "open"));
    }

    protected <T> boolean isSearchRequest(Map<String, String> parms, BeanCollector<?, T> model) {
        for (Object k : parms.keySet()) {
            if (isSearchRequest((String) k, model)) {
                return true;
            }
        }
        return false;
    }

    /* *******************************************************************
     * search algorithms
     * ******************************************************************/

    protected <T> boolean isSearchRequest(String actionId, BeanCollector<?, T> model) {
        return model.hasMode(IBeanCollector.MODE_SEARCHABLE) && actionId.equals(model.getSearchAction().getId());
    }

    protected <T> BeanCollector<?, T> processSearchRequest(Map<String, String> parms, BeanCollector<?, T> model) {
//        try {
        //fill the search values
        Bean<?> filterBean = model.getBeanFinder().getFilterRange();
        if (filterBean != null) {
            Bean<?> from = (Bean<?>) filterBean.getValueAsBean("from", false);
            Bean<?> to = (Bean<?>) filterBean.getValueAsBean("to", false);
            final String NAME = "name";
            if (!from.getAttributeNames()[0].equals(NAME) || from.getAttributeNames().length != 1) {
                from.getPresentationHelper().change(BeanPresentationHelper.PROP_DOVALIDATION, false);
//                if (from.hasAttribute(NAME)) {
//                    from.setAttributeFilter(NAME);
//                }
//            from.setName(null);
                to.getPresentationHelper().change(BeanPresentationHelper.PROP_DOVALIDATION, false);
//                if (to.hasAttribute(NAME))
//                {
//                    to.setAttributeFilter(NAME);
//            to.setName(null);
//                }
            }

            for (String p : parms.keySet()) {
                String rowName = StringUtil.substring(p, null, ".", true);
                String colName = StringUtil.substring(p, ".", null, true);
                if (from.getPresentationHelper().prop(KEY_FILTER_FROM_LABEL).equals(rowName)) {
                    from.changeToParsedValue(colName, parms.get(p));
                } else if (to.getPresentationHelper().prop(KEY_FILTER_TO_LABEL).equals(rowName)) {
                    to.changeToParsedValue(colName, parms.get(p));
                } else {
                    from.changeToParsedValue(colName, parms.get(p));
                    to.changeToParsedValue(colName, parms.get(p));
                }
            }
        }
        model.getSearchAction().activate();
        //TODO: put only, if range is filled - how can we evaluate that?
        putSearchParameterToContext(model);
//        } catch (Exception ex) {
//            //don't break the panel-creation - the full exception will be handled in main-session-routine.
//            LOG.error("couldn' fill search-panel values", ex);
//        }
        //a search request will show the same search panel again - but with filtered data.
        return model;
    }

    /**
     * putSearchParameterToContext
     * 
     * @param model
     */
    private <T> void putSearchParameterToContext(BeanCollector<?, T> model) {
        // create shallow copies of filter, from, to
        if (model.getBeanFinder().getFilterRange() != null) {
            IRange<?> filter = BeanUtil.clone(model.getBeanFinder().getFilterRange().getInstance());
            //replace entities with copies holding only the id
            Bean.getBean(filter).setValue("from", BeanContainer.detachEntities(BeanUtil.clone(filter.getFrom())));
            Bean.getBean(filter).setValue("to", BeanContainer.detachEntities(BeanUtil.clone(filter.getTo())));
            context.put(PREFIX_CONTEXT_RANGE + model.getDeclaringClass().getName(), filter);
        }
    }

    /**
     * converts the standard date format yyyy-MM-dd to the locale specific date format - to be parseable
     * 
     * @param parms
     */
    private void convertDates(Map<String, String> parms) {
        LOG.info("converting dates");
        String v;
        for (String p : parms.keySet()) {
            v = parms.get(p);
            if (v != null && v.matches(RegExpFormat.FORMAT_DATE_SQL)) {
                parms.put(p, DateUtil.getFormattedDate(DateUtil.getDateSQL(v)));
            }
        }
    }

    /**
     * evaluates all selected checkboxes of a table (given by a beancollector) and fills a collection with selected
     * table-beans
     * 
     * @param c model, holding table data
     * @param parms user response
     * @return all selected table beans
     */
    Collection<Object> getSelectedElements(BeanCollector c, Map<String, String> parms) {
        Collection<Object> selectedElements = new LinkedList<Object>();
        Number selection;
        Collection<?> data = c.getCurrentData();
        for (String p : parms.keySet()) {
            selection = NumberUtil.extractNumber(p);
            if (selection != null && "on".equalsIgnoreCase(parms.get(p))) {
                //evaluate selected element to be used by an action
                Object selectedBean = CollectionUtil.getList(data.iterator())
                    .get(selection.intValue()
                        - (ENV.get("layout.grid.searchrow.show", true) && c.hasMode(MODE_SEARCHABLE) && c.hasFilter()
                            ? OFFSET_FILTERLINES : 0));
                selectedElements.add(selectedBean);
            }
        }
        return selectedElements;
    }

    /**
     * uses {@link #getSelectedElements(BeanCollector, Map<String, String>)} to provide the selection to the
     * selectionprovider of the beancollector.
     * 
     * @param c table model
     * @param parms user response
     */
    boolean provideSelection(BeanCollector c, Map<String, String> parms) {
        Collection<Object> elements = getSelectedElements(c, parms);
        c.getSelectionProvider().setValue(elements);
        if (c.getComposition() != null) {
            c.getCurrentData().retainAll(elements);
        }
        return !c.getSelectionProvider().isEmpty();
//
//        if (elements.size() > 0)
//            navigation.push(new Bean(elements.iterator().next()));
    }

    @Override
    public Object getId() {
        return inetAddress.getHostName() + "&" + authorization.getUser();
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public ClassLoader getSessionClassLoader() {
        return sessionClassloader;
    }

    @Override
    public UncaughtExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    @Override
    public long getDuration() {
        return System.currentTimeMillis() - sessionStart;
    }

    @Override
    public long getLastAccess() {
        return lastAccess;
    }

    @Override
    public Main getApplication() {
        return server;
    }

    @Override
    public IAuthorization getUserAuthorization() {
        return authorization;
    }

    @Override
    public BeanDefinition[] getNavigationStack() {
        return nav.toArray();
    }

    @Override
    public BeanDefinition getWorkingObject() {
        return nav.current();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWebsocketPort() {
        return websocketPort;
    }

    @Override
    public boolean check(long timeout, boolean throwException) {
        boolean authenicatedButNotConnected =
            nav.current() != null && !Persistence.class.isAssignableFrom(nav.current().getDeclaringClass())
                && authorization != null && !BeanContainer.isConnected();
        boolean expired = System.currentTimeMillis() - getLastAccess() > timeout || nav == null || nav.isEmpty()
            || authenicatedButNotConnected;
        if (expired) {
            LOG.info("session " + this + " expired!");
            if (throwException) {
                close();
                throw new ManagedException("session closed");
            }
        }
        return !expired;
    }

    @Override
    public void handleEvent(EMessage e) {
        if (ENV.get("session.onpersist.broadcast.alert", true)) {
            if (BeanContainer.instance().hasPermission(e.getMsg().toString(), null)
                && (e.getDestPath() == null
                    || toString().startsWith(StringUtil.substring(e.getDestPath(), null, "*")))) {
                Message.send(exceptionHandler, e.getMsg().toString());
            }
        }
    }

    @Override
    public String toString() {
        return id;
    }

}
