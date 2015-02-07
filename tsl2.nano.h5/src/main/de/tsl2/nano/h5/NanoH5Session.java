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
import static de.tsl2.nano.h5.NanoH5.OFFSET_FILTERLINES;
import static de.tsl2.nano.h5.NanoHTTPD.HTTP_BADREQUEST;
import static de.tsl2.nano.h5.NanoHTTPD.MIME_HTML;

import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.java_websocket.WebSocket;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.IBeanCollector;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.ListSet;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.Main;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.exception.ExceptionHandler;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.h5.NanoHTTPD.Response;
import de.tsl2.nano.h5.configuration.BeanConfigurator;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.h5.navigation.Parameter;
import de.tsl2.nano.h5.websocket.NanoWebSocketServer;
import de.tsl2.nano.h5.websocket.WebSocketExceptionHandler;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.serviceaccess.IAuthorization;
import de.tsl2.nano.util.NumberUtil;

/**
 * user session for nano.h5 server
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class NanoH5Session implements ISession {
    private static final Log LOG = LogFactory.getLog(NanoH5Session.class);

    String id;
    NanoH5 server;
    IPageBuilder<?, String> builder;
    /** workflow or bean navigator */
    IBeanNavigator nav;
    /** html response */
    Response response;
    /** concatencation of database-name+schema+beans.jar */
    Iterable<BeanDefinition> context;

    /** sessions classloader */
    ClassLoader sessionClassloader;
    /** requests user internet adress */
    InetAddress inetAddress;

    /** port of websocket, if used */
    int websocketPort;

    /** sessions exceptionHandler */
    ExceptionHandler exceptionHandler;

    /** logs all user actions to be given on error-handling */
    List<String> actionLog;

    /** for profiling in status-line (current work-time of last request) */
    long startTime;

    /** session start */
    private long sessionStart;

    private IAuthorization authorization;

    public static final String PREFIX_STATUS_LINE = "@";

    /**
     * constructor
     * 
     * @param server
     * @param inetAddress
     * @param navigation
     * @param appstartClassloader
     * @param authorization
     */
    public NanoH5Session(NanoH5 server,
            InetAddress inetAddress, IBeanNavigator navigator,
            ClassLoader appstartClassloader, IAuthorization authorization, Iterable<BeanDefinition> context) {
        super();
        this.server = server;
        this.inetAddress = inetAddress;
        this.builder = server.builder;
        this.nav = navigator;
        this.sessionClassloader = appstartClassloader;
        createExceptionHandler();
        this.actionLog = new LinkedList<>();
        this.authorization = authorization;
        this.context = context;
        this.sessionStart = System.currentTimeMillis();
        Persistence p = Persistence.current();
        this.id = inetAddress + p.getConnectionUrl() + "." + p.getConnectionUserName() + "." + p.getJarFile();
    }

    /**
     * createExceptionHandler
     */
    private void createExceptionHandler() {
        if (Environment.get("use.websocket", true)) {
            final NanoWebSocketServer socketServer =
                new NanoWebSocketServer(this, createSocketAddress());
            websocketPort = socketServer.getPort();
            this.exceptionHandler =
                (ExceptionHandler) Environment.addService(UncaughtExceptionHandler.class,
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
                (ExceptionHandler) Environment.addService(UncaughtExceptionHandler.class, new ExceptionHandler());
        }
        Thread.currentThread().setUncaughtExceptionHandler(exceptionHandler);
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
    public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
        String msg = "[undefined]";
        try {
            Thread.currentThread().setContextClassLoader(sessionClassloader);
            LOG.info(String.format("serving request:\n\turi: %s\n\tmethod: %s\n\theader: %s\n\tparms: %s\n\tfiles: %s",
                uri,
                method,
                header,
                parms,
                files));
            //WORKAROUND for uri-problem
            String referer = header.getProperty("referer");
            if (parms.containsKey(IAction.CANCELED)
                || (method.equals("POST") && referer != null && uri.length() > 1 && referer.contains(uri)))
                uri = "/";
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
                    if (HtmlUtil.isURL(msg))
                        return server.serve(msg, "GET", header, parms, files);
                    else if (!HtmlUtil.containsHtml(msg))
                        msg = HtmlUtil.createMessagePage(msg);
                } else {
                    if (userResponse instanceof BeanDefinition)
                        ((BeanDefinition) userResponse).onActivation();
                    if (!exceptionHandler.hasExceptions())
                        Message.send(exceptionHandler, createStatusText(startTime));
                    msg = getNextPage(userResponse);
                }
                response = server.createResponse(msg);
            } else {
                close();
                return server.createResponse(Html5Presentation.createMessagePage("start.template",
                    Environment.getName() + "<br/>" + "Restart Session", server.serviceURL));
            }
        } catch (Throwable e /*respect errors like NoClassDefFound...the application should continue!*/) {
            LOG.error(e);
            ManagedException ex = (ManagedException) new ManagedException(e) {
                @Override
                public String getMessage() {
                    return super.getMessage() + "\n\nAction-Stack:\n"
                        + StringUtil.toFormattedString(actionLog, 1000, true);
                }
            };
            msg = refreshPage(ex);
            Message.send(exceptionHandler, ex.toString());
            response = server.createResponse(HTTP_BADREQUEST, MIME_HTML, msg);
            actionLog.clear();
            //don't forget that there was an exception. to be seen on the next exception ;-)
            logaction(ex.toString(), parms);
        }
        //TODO: eliminate bug in NanoHTTPD not resetting uri...
//        header.clear();
//        response.header.remove(uri);
        return response;
    }

    @Override
    public void close() {
        server.sessions.remove(inetAddress);
        nav = null;
        response = null;
        authorization = null;
        builder = null;
        authorization = null;
        sessionClassloader = null;
    }

    String createStatusText(long startTime) {
        String user =
            authorization != null ? Environment.translate("tsl2nano.login.user", true) + ": "
                + authorization.getUser() + ", " + "Online: "
                + DateUtil.getFormattedMinutes(getDuration()) + " min, " : "";
        return PREFIX_STATUS_LINE + user
            + Environment.translate("tsl2nano.time", true)
            + ": " + DateUtil.getFormattedDateTime(new Date()) + ", "
            + Environment.translate("tsl2nano.request", true) + ": "
            + DateUtil.getFormattedMinutes(System.currentTimeMillis() - startTime) + " min"
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
        BeanDefinition<?> model = nav.next(returnCode);
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
    private Object processInput(String uri, Properties parms, Number uriLinkNumber) {
//        if (parms.containsKey(PageBuilder.COMMAND_RESTART)) {
//            stop();
//            main(null);
//        }
        Object responseObject = null;
        if (parms.containsKey(IAction.CANCELED)) {
            logaction(IAction.CANCELED, null);
            if (nav.current() != null) {
                ((BeanDefinition) nav.current()).onDeactivation();

                //perhaps remove configuration bean
                BeanConfigurator configurator = Environment.get(BeanConfigurator.class);
                if (configurator != null
                    && ((BeanDefinition) nav.current()).getDeclaringClass().equals(BeanConfigurator.class)) {
                    Environment.removeService(BeanConfigurator.class);
                }
            }
            return IAction.CANCELED;
        }

        convertDates(parms);

        refreshCurrentBeanValues(parms);

        if (nav.current() instanceof Controller) {
            Controller ctrl = (Controller) nav.current();
            String actionName = (String) parms.keySet().iterator().next();
            if (actionName != null && actionName.startsWith(Controller.PREFIX_CTRLACTION))
                return ctrl.doAction(actionName);
        }
        //follow links or fill selected items
        if (nav.current() instanceof BeanCollector) {
            //follow given link
            if (uriLinkNumber != null) {
                BeanCollector collector = (BeanCollector) nav.current();
                Collection data = collector.getCurrentData();
                ListSet listSet = CollectionUtil.asListSet(data);
                Object selectedItem =
                    listSet.get(uriLinkNumber.intValue()
                        - (collector.hasMode(MODE_SEARCHABLE) && collector.hasFilter() ? 2 : 0));
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
                        if (Environment.get("application.edit.multiple", true))
                            responseObject = putSelectionOnStack((BeanCollector) nav.current());
                        else
                            responseObject = nav.current();
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
            if (nav.current().getActions() != null)
                actions.addAll(c.getActions());
            actions.addAll(c.getPresentationHelper().getPageActions());
            actions.addAll(c.getPresentationHelper().getSessionActions(this));
            actions.addAll(c.getPresentationHelper().getApplicationActions(this));
            if (c.isMultiValue()) {
                actions.addAll(((BeanCollector) c).getColumnSortingActions());
                actions.add(((BeanCollector) c).getQuickSearchAction());
            }
            //start the actions
            //respect action-call through menu-link (with method GET but starting with '!!!'
            Set<Object> keySet = new HashSet<Object>();
            if (uri.contains(Html5Presentation.PREFIX_ACTION))
                keySet.add(StringUtil.substring(uri, Html5Presentation.PREFIX_ACTION, null));
            keySet.addAll(parms.keySet());
            for (Object k : keySet) {
                String p = (String) k;
                IAction<?> action = getAction(actions, p);
                if (action != null) {
                    logaction(action, parms);
                    //send this information to the client to show a progress bar.
                    Message.send("submit");
                    Message.send(Environment.translate("tsl2nano.starting", true) + " "
                        + action.getShortDescription() + " ...");
                    if (c.isMultiValue() && action.getId().endsWith(BeanCollector.POSTFIX_QUICKSEARCH)) {
                        action.setParameter(parms.get(Html5Presentation.ID_QUICKSEARCH_FIELD));
                        responseObject = action.activate();
                    } else if (c.isMultiValue()
                        && isSearchRequest(action.getId(), (BeanCollector<?, ?>) c)) {
                        responseObject = processSearchRequest(parms, (BeanCollector<?, ?>) c);
                    } else {
                        /*
                         * submit/assign and cancel will not push a new element to the navigation stack!
                         * TODO: refactore access to names ('reset' and 'save')
                         */
                        action.setParameter(getContextParameter());
                        Object result = action.activate();

                        /*
                         * if action is asynchron, it's a long term action showing the same page again
                         * with progress informations
                         */
//                        if (action.isSynchron())
//                            throw new Message("starting long term request:\t" + action.getShortDescription());
                        if (result != null && responseObject != IAction.CANCELED && !action.getId().endsWith("save")) {
                            responseObject = result;
                            if (c instanceof Bean
                                && ((Bean) c).getInstance() instanceof Persistence)
                                authorization = Environment.get(IAuthorization.class);
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

    /**
     * context parameters will be evaluated from context beans. these beans may have references to other beans. so the
     * context beans have to be in the right order.
     * 
     * @return context parameters
     */
    private Parameter getContextParameter() {
        Iterable<BeanDefinition> con = getContext();
        Parameter p = new Parameter();
        for (BeanDefinition c : con) {
            p.putAll(c.toValueMap(p));
        }
        if (LOG.isDebugEnabled())
            LOG.debug("session:" + this + "\n\tcontext parameters: " + p.keySet());
        return p;
    }

    private void logaction(IAction<?> action, Properties parameter) {
        logaction(action.getId(), parameter);
    }

    private void logaction(String id, Properties p) {
        actionLog.add(DateUtil.getFormattedTimeStamp() + " ==> " + id + " (" + nav.current() + ")"
            + (!Util.isEmpty(p) ? "\n\t" + p : ""));
    }

    /**
     * refreshCurrentBeanValues
     * 
     * @param parms
     */
    private void refreshCurrentBeanValues(Properties parms) {
        LOG.info("refreshing current bean values");
        if (nav.current() instanceof Bean) {
            Collection<Exception> exceptions = new LinkedList<Exception>();
            Bean vmodel = (Bean) nav.current();
            for (String p : parms.stringPropertyNames()) {
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
                        String newString = parms.getProperty(p);
                        if (oldString == null || !oldString.equals(newString))
                            vmodel.setParsedValue(p, newString);
                        else
                            LOG.debug("ignoring unchanged attribute " + vmodel.getAttribute(p));
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
                if (a.getId().equals(id))
                    return a;
            }
        }
        return null;
    }

    protected boolean isCanceled(Properties parms) {
        return parms.containsKey(BTN_CANCEL);
    }

    protected boolean isReturn(Properties parms) {
        return isCanceled(parms) || parms.containsKey(BTN_ASSIGN) || parms.containsKey(BTN_SUBMIT);
    }

    protected <T> boolean isNewAction(Properties parms, BeanCollector<?, T> model) {
        for (Object k : parms.keySet()) {
            if (isNewAction((String) k, model))
                return true;
        }
        return false;
    }

    protected <T> boolean isNewAction(String actionId, BeanCollector<?, T> model) {
        return actionId.equals(BeanContainer.getActionId(model.getClazz(), true, "new"));
    }

    protected <T> boolean isOpenAction(Properties parms, BeanCollector<?, T> model) {
        for (Object k : parms.keySet()) {
            if (isOpenAction((String) k, model))
                return true;
        }
        return false;
    }

    protected <T> boolean isOpenAction(String actionId, BeanCollector<?, T> model) {
        return actionId.equals(BeanContainer.getActionId(model.getClazz(), true, "open"));
    }

    protected <T> boolean isSearchRequest(Properties parms, BeanCollector<?, T> model) {
        for (Object k : parms.keySet()) {
            if (isSearchRequest((String) k, model))
                return true;
        }
        return false;
    }

    protected <T> boolean isSearchRequest(String actionId, BeanCollector<?, T> model) {
        return model.hasMode(IBeanCollector.MODE_SEARCHABLE) && actionId.equals(model.getSearchAction().getId());
    }

    protected <T> BeanCollector<?, T> processSearchRequest(Properties parms, BeanCollector<?, T> model) {
//        try {
        //fill the search values
        Bean<?> filterBean = model.getBeanFinder().getFilterRange();
        if (filterBean != null) {
            Bean<?> from = (Bean<?>) filterBean.getValueAsBean("from", false);
            Bean<?> to = (Bean<?>) filterBean.getValueAsBean("to", false);
            final String NAME = "name";
            if (!from.getAttributeNames()[0].equals(NAME) || from.getAttributeNames().length != 1) {
                from.getPresentationHelper().change(BeanPresentationHelper.PROP_DOVALIDATION, false);
                if (from.hasAttribute(NAME))
                    from.setAttributeFilter(NAME);
//            from.setName(null);
                to.getPresentationHelper().change(BeanPresentationHelper.PROP_DOVALIDATION, false);
                if (to.hasAttribute(NAME))
                    to.setAttributeFilter(NAME);
//            to.setName(null);
            }

            for (String p : parms.stringPropertyNames()) {
                String rowName = StringUtil.substring(p, null, ".", true);
                String colName = StringUtil.substring(p, ".", null, true);
                if (from.getPresentationHelper().prop(KEY_FILTER_FROM_LABEL).equals(rowName)
                    && from.hasAttribute(colName) && from.getAttribute(colName).hasWriteAccess()) {
                    from.setParsedValue(colName, parms.getProperty(p));
                } else if (to.getPresentationHelper().prop(KEY_FILTER_TO_LABEL).equals(rowName)
                    && to.hasAttribute(colName) && to.getAttribute(colName).hasWriteAccess()) {
                    to.setParsedValue(colName, parms.getProperty(p));
                } else if (from.hasAttribute(colName) && from.getAttribute(colName).hasWriteAccess()) {
                    from.setParsedValue(colName, parms.getProperty(p));
                    to.setParsedValue(colName, parms.getProperty(p));
                }
            }
        }
        model.getSearchAction().activate();
//        } catch (Exception ex) {
//            //don't break the panel-creation - the full exception will be handled in main-session-routine.
//            LOG.error("couldn' fill search-panel values", ex);
//        }
        //a search request will show the same search panel again - but with filtered data.
        return model;
    }

    /**
     * converts the standard date format yyyy-MM-dd to the locale specific date format - to be parseable
     * 
     * @param parms
     */
    private void convertDates(Properties parms) {
        LOG.info("converting dates");
        String v;
        for (String p : parms.stringPropertyNames()) {
            v = parms.getProperty(p);
            if (v != null && v.matches(RegExpFormat.FORMAT_DATE_SQL))
                parms.setProperty(p, DateUtil.getFormattedDate(DateUtil.getDateSQL(v)));
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
    Collection<Object> getSelectedElements(BeanCollector c, Properties parms) {
        Collection<Object> selectedElements = new LinkedList<Object>();
        Number selection;
        Collection<?> data = c.getCurrentData();
        for (String p : parms.stringPropertyNames()) {
            selection = NumberUtil.extractNumber(p);
            if (selection != null && "on".equalsIgnoreCase(parms.getProperty(p))) {
                //evaluate selected element to be used by an action
                Object selectedBean = CollectionUtil.getList(data.iterator())
                    .get(selection.intValue() - (c.hasMode(MODE_SEARCHABLE) && c.hasFilter() ? OFFSET_FILTERLINES : 0));
                selectedElements.add(selectedBean);
            }
        }
        return selectedElements;
    }

    /**
     * uses {@link #getSelectedElements(BeanCollector, Properties)} to provide the selection to the selectionprovider of
     * the beancollector.
     * 
     * @param c table model
     * @param parms user response
     */
    boolean provideSelection(BeanCollector c, Properties parms) {
        Collection<Object> elements = getSelectedElements(c, parms);
        c.getSelectionProvider().setValue(elements);
        if (c.getComposition() != null)
            c.getCurrentData().retainAll(elements);
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
    public Iterable<BeanDefinition> getContext() {
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
    public Main getApplication() {
        return server;
    }

    @Override
    public IAuthorization getUserAuthorization() {
        return authorization;
    }

    @Override
    public Object[] getNavigationStack() {
        return nav.toArray();
    }

    public Object getWorkingObject() {
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
    public String toString() {
        return id;
    }
}
