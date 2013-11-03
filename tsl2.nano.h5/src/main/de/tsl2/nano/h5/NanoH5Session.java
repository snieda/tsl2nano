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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.commons.logging.Log;

import de.tsl2.nano.Environment;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.ListSet;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.h5.NanoHTTPD.Response;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.util.DateUtil;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.StringUtil;
/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class NanoH5Session {
    private static final Log LOG = LogFactory.getLog(NanoH5Session.class);

    NanoH5 server;
    IPageBuilder<?, String> builder;
    IBeanNavigator nav;
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
            InetAddress inetAddress, IBeanNavigator navigator,
            ClassLoader appstartClassloader) {
        super();
        this.server = server;
        this.inetAddress = inetAddress;
        this.builder = server.builder;
        this.nav = navigator;
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
            if (parms.containsKey(IAction.CANCELED) || (method.equals("POST") && referer != null && uri.length() > 1 && referer.contains(uri)))
                uri = "/";
            BeanDefinition<?> linkToModel = nav.fromUrl(uri);
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
                close();
                return server.createResponse("<a href=\"" + server.serviceURL + "\">restart session</a>");
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
        nav = null;
        response = null;
    }
    
    private String refreshPage(String message) {
        return builder.build(nav.current(), message, true);
    }

    /**
     * pops the next model from navigation stack (see {@link #getNextModel(Object)} and asks the pagebuilder to create a
     * full html page for the given model.
     * 
     * @param returnCode return code of last request
     * @return html string
     */
    private String getNextPage(Object returnCode) {
        return builder.build(nav.next(returnCode), "", true, nav.toArray());
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
        if (parms.containsKey(IAction.CANCELED))
            return IAction.CANCELED;

        convertDates(parms);

        //refresh bean values
        if (nav.current() instanceof Bean) {
            Bean vmodel = (Bean) nav.current();
            for (String p : parms.stringPropertyNames()) {
                if (vmodel.hasAttribute(p)) {
                    vmodel.setParsedValue(p, parms.getProperty(p));
                }
            }
        }
        //follow links or fill selected items
        if (nav.current() instanceof BeanCollector) {
            //follow given link
            if (uriLinkNumber != null) {
                BeanCollector collector = (BeanCollector) nav.current();
                Collection data = collector.getBeanFinder().getData();
                ListSet listSet = CollectionUtil.asListSet(data);
                //visible numbers starting with '1', but indexes starting with '0'
                responseObject = new Bean(listSet.get(uriLinkNumber.intValue() - 1));
                return responseObject;
            } else {
                if (!isCanceled(parms) && provideSelection((BeanCollector) nav.current(), parms)) {
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
        }
        //start the actions
        Collection<IAction> actions = null;
        if (nav.current() != null) {
            actions = new ArrayList<IAction>();
            if (nav.current().getActions() != null)
                actions.addAll(nav.current().getActions());
            actions.addAll(nav.current().getPresentationHelper().getPresentationActions());
            if (nav.current().isMultiValue()) {
                actions.addAll(((BeanCollector) nav.current()).getColumnSortingActions());
            }
        }
        if (actions != null) {
            for (Object k : parms.keySet()) {
                String p = (String) k;
                IAction<?> action = getAction(actions, p);
                if (action != null) {
                    if (nav.current().isMultiValue() && isSearchRequest(action.getId(), (BeanCollector<?, ?>) nav.current())) {
                        responseObject = processSearchRequest(parms, (BeanCollector<?, ?>) nav.current());
                    } else {
                        /*
                         * submit/assign and cancel will not push a new element to the navigation stack!
                         * TODO: refactore access to names ('reset' and 'save')
                         */
                        Object result = action.activate();
                        if (result != null && responseObject != IAction.CANCELED && !action.getId().endsWith("save")) {
                            responseObject = result;
                        } else if (action.getId().endsWith("reset")) {
                            responseObject = nav.current();
                        } else {
//                        action.activate();
                            return responseObject;
                        }
                    }
                } else {
                    if (p.endsWith(IPresentable.POSTFIX_SELECTOR)) {
                        String n = StringUtil.substring(p, null, IPresentable.POSTFIX_SELECTOR);
                        final BeanValue assignableAttribute = (BeanValue) nav.current().getAttribute(n);
                        responseObject = assignableAttribute.connectToSelector(nav.current());
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
}
