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

import static de.tsl2.nano.h5.HtmlUtil.BTN_ASSIGN;
import static de.tsl2.nano.h5.HtmlUtil.BTN_CANCEL;
import static de.tsl2.nano.h5.HtmlUtil.BTN_SUBMIT;
import static de.tsl2.nano.h5.NanoH5.OFFSET_FILTERLINES;
import static de.tsl2.nano.h5.NanoH5.START_PAGE;
import static de.tsl2.nano.h5.NanoHTTPD.HTTP_BADREQUEST;
import static de.tsl2.nano.h5.NanoHTTPD.MIME_HTML;
import static de.tsl2.nano.util.bean.def.BeanPresentationHelper.KEY_FILTER_FROM_LABEL;
import static de.tsl2.nano.util.bean.def.BeanPresentationHelper.KEY_FILTER_TO_LABEL;
import static de.tsl2.nano.util.bean.def.IBeanCollector.MODE_SEARCHABLE;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Stack;

import org.apache.commons.logging.Log;

import de.tsl2.nano.Environment;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.ListSet;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.h5.NanoHTTPD.Response;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.util.DateUtil;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.bean.BeanContainer;
import de.tsl2.nano.util.bean.def.Bean;
import de.tsl2.nano.util.bean.def.BeanCollector;
import de.tsl2.nano.util.bean.def.BeanDefinition;
import de.tsl2.nano.util.bean.def.BeanPresentationHelper;
import de.tsl2.nano.util.bean.def.BeanValue;
import de.tsl2.nano.util.bean.def.IBeanCollector;
import de.tsl2.nano.util.bean.def.IPageBuilder;
import de.tsl2.nano.util.bean.def.IPresentable;
/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class NanoH5Session {
    private static final Log LOG = LogFactory.getLog(NanoH5Session.class);

    NanoH5 server;
    IPageBuilder<?, String> builder;
    Stack<BeanDefinition<?>> navigation = new Stack<BeanDefinition<?>>();
    BeanDefinition<?> model;
    Response response;
    ClassLoader sessionClassloader;
    InetAddress inetAddress;
    
    /**
     * constructor
     * @param server
     * @param inetAddress 
     * @param navigation
     * @param appstartClassloader
     */
    public NanoH5Session(NanoH5 server,
            InetAddress inetAddress, Stack<BeanDefinition<?>> navigation,
            ClassLoader appstartClassloader) {
        super();
        this.server = server;
        this.inetAddress = inetAddress;
        this.builder = server.builder;
        this.navigation = navigation;
        this.sessionClassloader = appstartClassloader;
    }

    /**
     * {@inheritDoc}
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
            if (parms.containsKey(IAction.CANCELED)/* || (method.equals("POST") && referer != null && uri.length() > 1 && referer.contains(uri))*/)
                uri = "/";
            BeanDefinition<?> linkToModel = evalLinkToModel(uri);
            Object userResponse = null;
            //direct link to another page/bean
            //selection-link-number in beancollector
            Number uriLinkNumber = linkToModel != null ? null : NumberUtil.extractNumber(uri.substring(1));
            //form-button clicked - or first page
            if (!parms.isEmpty() || linkToModel != null || uriLinkNumber != null || response == null) {
                if (linkToModel != null) {
                    userResponse = linkToModel;
                } else {
                    userResponse = processInput(uri, parms, uriLinkNumber);
                }
                if (userResponse instanceof String && !userResponse.equals(IAction.CANCELED)) {
                    if (!msg.contains("<html>"))
                        msg = "<html><body><pre>" + userResponse + "</pre></body></html>";
                } else {
                    msg = getNextPage(userResponse);
                }
                response = server.createResponse(msg);
            } else {
                return server.createResponse("<a href=\"http://" + Environment.get("http.ip", "localhost") + ":" + Environment.get("http.port", 8067) + "\">restart session</a>");
            }
        } catch (Exception e) {
            RuntimeException ex = ForwardedException.toRuntimeEx(e, true);
            msg = refreshPage(ex.getMessage());
            response = server.createResponse(HTTP_BADREQUEST, MIME_HTML, msg);
        }
        //TODO: eliminate bug in NanoHTTPD not resetting uri...
//        header.clear();
//        response.header.remove(uri);
        return response;
    }

    void close() {
        server.sessions.remove(inetAddress);
        navigation = null;
        model = null;
        response = null;
    }
    /**
     * evaluates, if a navigation item (bean) was clicked.
     * 
     * @param uri uri to analyze
     * @return navigation bean or null
     */
    private BeanDefinition<?> evalLinkToModel(String uri) {
        BeanDefinition<?> linkBean = null;
        String link = StringUtil.substring(uri, "/", null, true);
        //reset-link clicked? recreate the navigation
        if (link.equals(START_PAGE)) {
            close();
            return null;
        }
        for (BeanDefinition<?> bean : navigation) {
            if (bean.getName().equals(link)) {
                linkBean = bean;
                break;
            }
        }
        return linkBean;
    }

    private String refreshPage(String message) {
        return builder.build(model, message, true);
    }

    /**
     * pops the next model from navigation stack (see {@link #getNextModel(Object)} and asks the pagebuilder to create a
     * full html page for the given model.
     * 
     * @param returnCode return code of last request
     * @return html string
     */
    private String getNextPage(Object returnCode) {
        return builder.build(getNextModel(returnCode), "", true, getNavigationQueue());
    }

    /**
     * navigation stack
     * 
     * @return current navigation queue wrapped into an object array
     */
    private BeanDefinition<?>[] getNavigationQueue() {
        BeanDefinition<?>[] beans = new BeanDefinition[navigation.size()];
        for (int i = 0; i < navigation.size(); i++) {
            beans[i] = navigation.get(i);
        }
        return beans;
    }

    
    /**
     * @return Returns the navigation.
     */
    public Stack<BeanDefinition<?>> getNavigation() {
        return navigation;
    }

    /**
     * process user input
     * 
     * @param uri page uri
     * @param parms response parameter
     * @param uriLinkNumber if model is a bean-collector, it is the selected element number
     * @return user response object. may be {@link IAction#CANCELED} any saved or selected object or null.
     */
    private Object processInput(String uri, Properties parms, Number uriLinkNumber) {
//        if (parms.containsKey(PageBuilder.COMMAND_RESTART)) {
//            stop();
//            main(null);
//        }
        Object responseObject = null;
        if (parms.containsKey(IAction.CANCELED))
            return IAction.CANCELED;

        convertDates(parms);

        //refresh bean values
        if (model instanceof Bean) {
            Bean vmodel = (Bean) model;
            for (String p : parms.stringPropertyNames()) {
                if (vmodel.hasAttribute(p)) {
                    vmodel.setParsedValue(p, parms.getProperty(p));
                }
            }
        }
        //follow links or fill selected items
        if (model instanceof BeanCollector) {
            //follow given link
            if (uriLinkNumber != null) {
                BeanCollector collector = (BeanCollector) model;
                Collection data = collector.getBeanFinder().getData();
                ListSet listSet = CollectionUtil.asListSet(data);
                //visible numbers starting with '1', but indexes starting with '0'
                responseObject = new Bean(listSet.get(uriLinkNumber.intValue() - 1));
                return responseObject;
            } else {
                if (!isCanceled(parms) && provideSelection((BeanCollector) model, parms)) {
                    if (isReturn(parms)) {
                        responseObject = null;
                    } else if (isOpenAction(parms, (BeanCollector) model)) {
                        //normally, after a selection the navigation object will be hold on stack
                        if (Environment.get("application.edit.multiple", true))
                            responseObject = putSelectionOnStack((BeanCollector) model);
                        else
                            responseObject = model;
                    }
                }
            }
        }
        //start the actions
        Collection<IAction> actions = null;
        if (model != null) {
            actions = new ArrayList<IAction>();
            if (model.getActions() != null)
                actions.addAll(model.getActions());
            actions.addAll(model.getPresentationHelper().getPresentationActions());
            if (model.isMultiValue()) {
                actions.addAll(((BeanCollector) model).getColumnSortingActions());
            }
        }
        if (actions != null) {
            for (Object k : parms.keySet()) {
                String p = (String) k;
                IAction<?> action = getAction(actions, p);
                if (action != null) {
                    if (model.isMultiValue() && isSearchRequest(action.getId(), (BeanCollector<?, ?>) model)) {
                        responseObject = processSearchRequest(parms, (BeanCollector<?, ?>) model);
                    } else {
                        /*
                         * submit/assign and cancel will not push a new element to the navigation stack!
                         * TODO: refactore access to names ('reset' and 'save')
                         */
                        Object result = action.activate();
                        if (result != null && responseObject != IAction.CANCELED && !action.getId().endsWith("save")) {
                            responseObject = result;
                        } else if (action.getId().endsWith("reset")) {
                            responseObject = model;
                        } else {
//                        action.activate();
                            return responseObject;
                        }
                    }
                } else {
                    if (p.endsWith(IPresentable.POSTFIX_SELECTOR)) {
                        String n = StringUtil.substring(p, null, IPresentable.POSTFIX_SELECTOR);
                        final BeanValue assignableAttribute = (BeanValue) model.getAttribute(n);
                        responseObject = assignableAttribute.connectToSelector(model);
                    }
                }
            }
        }
        return responseObject;
    }

    private BeanDefinition<?> putSelectionOnStack(BeanCollector c) {
        Collection selection = (Collection) c.getSelectionProvider().getValue();
        BeanDefinition<?> firstElement = null;
        BeanDefinition<?> bean;
        for (Object object : selection) {
            bean = (BeanDefinition<?>) (object instanceof BeanDefinition ? object : Bean.getBean((Serializable) object));
            //don't add the first element, see behaviour in getNextModel()
            if (firstElement != null) {
                navigation.add(bean);
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
        return actionId.equals(BeanContainer.getActionId(model.getClazz(), true, "search"));
    }

    protected <T> BeanCollector<?, T> processSearchRequest(Properties parms, BeanCollector<?, T> model) {
        //fill the search values
        Bean<?> filterBean = model.getBeanFinder().getFilterRange();
        if (filterBean != null) {
            Bean<?> from = (Bean<?>) filterBean.getValueAsBean("from");
            Bean<?> to = (Bean<?>) filterBean.getValueAsBean("to");

            from.getPresentationHelper().change(BeanPresentationHelper.PROP_DOVALIDATION, false);
            from.setAttributeFilter("name");
            from.setName(null);
            to.getPresentationHelper().change(BeanPresentationHelper.PROP_DOVALIDATION, false);
            to.setAttributeFilter("name");
            to.setName(null);

            for (String p : parms.stringPropertyNames()) {
                String rowName = StringUtil.substring(p, null, ".", true);
                String colName = StringUtil.substring(p, ".", null, true);
                if (from.getPresentationHelper().prop(KEY_FILTER_FROM_LABEL).equals(rowName) && from.hasAttribute(colName)) {
                    from.setParsedValue(colName, parms.getProperty(p));
                } else if (to.getPresentationHelper().prop(KEY_FILTER_TO_LABEL).equals(rowName) && to.hasAttribute(colName)) {
                    to.setParsedValue(colName, parms.getProperty(p));
                } else if (from.hasAttribute(colName)) {
                    from.setParsedValue(colName, parms.getProperty(p));
                    to.setParsedValue(colName, parms.getProperty(p));
                }
            }
            model.getSearchAction().activate();
        }
        //a search request will show the same search panel again - but with filtered data.
        return model;
    }

    /**
     * converts the standard date format yyyy-MM-dd to the locale specific date format - to be parseable
     * 
     * @param parms
     */
    private void convertDates(Properties parms) {
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
                    .get(selection.intValue() - (c.hasMode(MODE_SEARCHABLE) ? OFFSET_FILTERLINES : 0));
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
        return !c.getSelectionProvider().isEmpty();
//
//        if (elements.size() > 0)
//            navigation.push(new Bean(elements.iterator().next()));
    }

    /**
     * the next model may be a new bean model, if the response object is not null, not in the current navigation stack
     * and not a cancel action.
     * 
     * @param userResponseObject result of {@link #processInput(String, Properties, Number)}
     * @return next bean model or null
     */
    private BeanDefinition<?> getNextModel(Object userResponseObject) {
        boolean isOnWork = false;
        boolean goBack = userResponseObject == null || userResponseObject == IAction.CANCELED;
        if (!goBack) {
            BeanDefinition<?> userResponseBean = (BeanDefinition<?>) (userResponseObject instanceof BeanDefinition<?> ? userResponseObject
                : Bean.getBean((Serializable) userResponseObject));
            isOnWork = navigation.contains(userResponseBean);
            if (!isOnWork) //--> go forward
                return (model = navigation.push(userResponseBean));
            else {
                if (model != userResponseBean) {
                    while (!userResponseBean.equals(navigation.peek()))
                        navigation.pop();
                    return model = navigation.peek();
                }
            }

        }
        //go back
        if (!isOnWork && response != null)//checking to be not the first page
            navigation.pop();

        model = navigation.size() > 0 ? navigation.peek() : null;
        //workaround for a canceled new action
        if (userResponseObject == IAction.CANCELED && model instanceof IBeanCollector) {
            removeUnpersistedNewEntities((BeanCollector) model);
        }
        return model;
    }

    /**
     * workaround for 'new' action on a beancollector followed by a cancel action - means the new instance is added to
     * the beancollector, but the cancel action has to remove the instance.
     * 
     * @param collector collector holding a canceled/transient instance.
     */
    private void removeUnpersistedNewEntities(BeanCollector collector) {
        if (!BeanContainer.instance().isPersistable(collector.getBeanFinder().getType()))
            return;
        Collection currentData = collector.getCurrentData();
        for (Iterator iterator = currentData.iterator(); iterator.hasNext();) {
            Object item = iterator.next();
            if (BeanContainer.isTransient(item))
                iterator.remove();
        }
    }

}
