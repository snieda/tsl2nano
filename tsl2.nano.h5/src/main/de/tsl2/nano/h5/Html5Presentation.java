/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Oct 1, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.h5;

import static de.tsl2.nano.bean.def.IBeanCollector.MODE_ASSIGNABLE;
import static de.tsl2.nano.bean.def.IBeanCollector.MODE_MULTISELECTION;
import static de.tsl2.nano.bean.def.IPresentable.POSTFIX_SELECTOR;
import static de.tsl2.nano.bean.def.IPresentable.STYLE_ALIGN_CENTER;
import static de.tsl2.nano.bean.def.IPresentable.STYLE_ALIGN_RIGHT;
import static de.tsl2.nano.bean.def.IPresentable.STYLE_DATA_FRAME;
import static de.tsl2.nano.bean.def.IPresentable.STYLE_DATA_IMG;
import static de.tsl2.nano.bean.def.IPresentable.STYLE_MULTI;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_ATTACHMENT;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_DATA;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_DATE;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_INPUT;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_INPUT_EMAIL;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_INPUT_MULTILINE;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_INPUT_NUMBER;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_INPUT_PASSWORD;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_INPUT_SEARCH;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_INPUT_TEL;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_INPUT_URL;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_OPTION;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_OPTION_RADIO;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_SELECTION;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_TIME;
import static de.tsl2.nano.h5.HtmlUtil.*;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.bean.def.Attachment;
import de.tsl2.nano.bean.def.AttributeCover;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.GroupBy;
import de.tsl2.nano.bean.def.GroupingPresentable;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.IBeanCollector;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.IPresentableColumn;
import de.tsl2.nano.bean.def.IValueDefinition;
import de.tsl2.nano.bean.def.IsPresentable;
import de.tsl2.nano.bean.def.MethodAction;
import de.tsl2.nano.bean.def.Presentable;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.bean.def.ValueExpressionFormat;
import de.tsl2.nano.bean.def.ValueGroup;
import de.tsl2.nano.core.AppLoader;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.cls.IValueAccess;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.messaging.ChangeEvent;
import de.tsl2.nano.core.messaging.IListener;
import de.tsl2.nano.core.util.BitUtil;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.CollectionUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.format.GenericParser;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.h5.collector.Controller;
import de.tsl2.nano.h5.collector.QueryResult;
import de.tsl2.nano.h5.collector.Statistic;
import de.tsl2.nano.h5.configuration.BeanConfigurator;
import de.tsl2.nano.h5.configuration.ExpressionDescriptor;
import de.tsl2.nano.h5.expression.Query;
import de.tsl2.nano.h5.plugin.IDOMDecorator;
import de.tsl2.nano.h5.websocket.WSEvent;
import de.tsl2.nano.h5.websocket.WebSocketRuleDependencyListener;
import de.tsl2.nano.h5.websocket.dialog.WSDialog;
import de.tsl2.nano.incubation.specification.Pool;
import de.tsl2.nano.incubation.specification.rules.RuleDependencyListener;
import de.tsl2.nano.persistence.DatabaseTool;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.plugin.Plugins;
import de.tsl2.nano.script.ScriptTool;

/**
 * is able to present a bean as an html page. main method is {@link #build(Element, String)}.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
/**
 * @param <T>
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Html5Presentation<T> extends BeanPresentationHelper<T> implements IPageBuilder<Element, String> {
    //this class is not serializable - but simple-xml will serialize <- we need transient modifiers
    protected transient int tabIndex;
    private transient List<Character> availableshortCuts;
    private transient static final Character SHORTCUTS[];

    static {
        SHORTCUTS = new Character[222];
        for (char i = 0; i < SHORTCUTS.length; i++) {
            SHORTCUTS[i] = (char) (i + 33);
        }
    }

    private transient String row1style;
    private transient String row2style;

    /** on sidenav action bars, all action are integrated into this one sidebar */
    private transient Element sideNav;

    static final Log LOG = LogFactory.getLog(Html5Presentation.class);
    private transient boolean isAuthenticated;
    private static transient String jsWebsocketTemplate;

    public static final String L_GRIDWIDTH = "layout.gridwidth";

    public static final String PREFIX_BEANREQUEST = "~~~";
    /** indicator for server to handle a link, that was got as link (method=GET) not as a file */
    public static final String PREFIX_ACTION = PREFIX_BEANREQUEST + "!!!";
    /** indicator for server to handle a link, that was got as link (method=GET) not as a file */
    public static final String PREFIX_BEANLINK = PREFIX_BEANREQUEST + "--)";

    public static final String ID_QUICKSEARCH_FIELD = "field.quicksearch";

    static final String MSG_FOOTER = "progress";

    static final String ICON_DEFAULT = "icons/trust_unknown.png";
    private static final String CSS_CLASS_PANEL_ACTION = "panelaction";
    private static final String CSS_CLASS_ACTION = "action";
	private static final String CSS_CLASS_SELECTOR = "selector";

    /**
     * constructor
     */
    public Html5Presentation() {
        super();
    }

    /**
     * constructor
     * 
     * @param bean
     */
    public Html5Presentation(BeanDefinition<T> bean) {
        super(bean);
    }

    @SuppressWarnings("serial")
    @Override
    public Collection<IAction> getApplicationActions(ISession session) {
        boolean firstTime = appActions == null;
        super.getApplicationActions(session);

        if (firstTime) {
            if (bean instanceof Bean && !isBeanConfiguration()) {
                appActions.add(new SecureAction(bean.getClazz(),
                    "configure",
                    IAction.MODE_UNDEFINED,
                    false,
                    "icons/compose.png") {
                    @Override
                    public Object action() throws Exception {
                        return BeanConfigurator.create((Class<Serializable>) bean.getClazz());
                    }
                });
            }
            //extensions of beancollector may be configured (the beancollector is defined through beandefinition!)
            //there are a lot of exceptions, so we constraint it to a debug level
            else if (LogFactory.isEnabled(LogFactory.DEBUG)
            		&& bean instanceof BeanCollector && !BeanCollector.class.equals(bean.getClass())) {
                appActions.add(new SecureAction(bean.getClass(),
                    "configure",
                    IAction.MODE_UNDEFINED,
                    false,
                    "icons/compose.png") {
                    @Override
                    public Object action() throws Exception {
                        return Bean.getBean(bean);
                    }
                });
            }
        }
        return appActions;
    }

    /**
     * addAdministrationActions
     */
    @SuppressWarnings("serial")
    @Override
    protected void addAdministrationActions(final ISession session, Bean bEnv) {
        if (ENV.get("app.login.administration", true)) {
            bEnv.addAction(new SecureAction(bean.getClazz(),
                "Scripttool",
                IAction.MODE_UNDEFINED,
                false,
                "icons/go.png") {
                Bean beanTool;

                @Override
                public Object action() throws Exception {
                    /*
                     * show the script tool to do direct sql or ant
                     */
                    if (beanTool == null) {
                        BeanConfigurator.defineAction(null);
                        final ScriptTool tool = ScriptTool.createInstance();
                        beanTool = Bean.getBean(tool);
                        beanTool.setAttributeFilter("sourceFile", "selectedAction", "name", "text"/*, "result"*/);
                        beanTool.getAttribute("text").getPresentation().setType(TYPE_INPUT_MULTILINE);
                        beanTool.getAttribute("text").getConstraint().setLength(100000);
                        beanTool.getAttribute("text").getConstraint()
                            .setFormat(null/*RegExpFormat.createLengthRegExp(0, 100000, 0)*/);
                        //                        beanTool.getAttribute("result").getPresentation().setType(TYPE_TABLE);
                        beanTool.getAttribute("sourceFile").getPresentation().setType(TYPE_ATTACHMENT);
                        beanTool.getAttribute("selectedAction").setRange(tool.availableActions());
                        beanTool.addAction(tool.runner());

                        String id = "scripttool.define.query";
                        String lbl = ENV.translate(id, true);
                        IAction queryDefiner = new CommonAction(id, lbl, lbl) {
                            @Override
                            public Object action() throws Exception {
                                String name = tool.getName();
                                if (Util.isEmpty(name)) {
                                    name = tool.getSourceFile() != null ? tool.getSourceFile().toLowerCase() : FileUtil
                                        .getValidFileName(tool.getText().replace('.', '_'));
                                }
                                //some file-systems may have problems on longer file names!
                                name = StringUtil.cut(name, 64);
                                Query query =
                                    new Query(name, tool.getText(), tool.getSelectedAction().getId()
                                        .equals("scripttool.sql.id"),
                                        null);
                                ENV.get(Pool.class).add(query);
                                QueryResult qr = new QueryResult(query.getName());
                                qr.getPresentable().setIcon("icons/barchart.png");
                                qr.saveDefinition();
                                return "New created specification-query: " + name;
                            }

                            @Override
                            public String getImagePath() {
                                return "icons/save.png";
                            }
                        };
                        beanTool.addAction(queryDefiner);
                        id = "scripttool.define.urltodatabase";
                        lbl = ENV.translate("database tool", true);
                        IAction dbToolURL = new CommonAction(id, lbl, lbl) {
                            @Override
                            public Object action() throws Exception {
                                return new DatabaseTool(Persistence.current()).getSQLToolURL();
                            }

                            @Override
                            public boolean isEnabled() {
                                return super.isEnabled() && Persistence.current() != null && new DatabaseTool(Persistence.current()).getSQLToolURL() != null;
                            }
                            @Override
                            public String getImagePath() {
                                return "icons/equipment.png";
                            }
                        };
                        beanTool.addAction(dbToolURL);
                    }
                    return beanTool;
                }
            });
        }
        super.addAdministrationActions(session, bEnv);
    }

    @SuppressWarnings("serial")
    @Override
    public Collection<IAction> getPageActions(ISession session) {
        boolean firstTime = pageActions == null;
        super.getPageActions(session);

        if (firstTime && bean.isMultiValue()) {
            pageActions.add(new SecureAction(bean.getClazz(),
                "statistic",
                IAction.MODE_UNDEFINED,
                false,
                "icons/barchart.png") {
                @Override
                public Object action() throws Exception {
                    BeanValue<T> from = null, to = null;
                    if (bean instanceof BeanCollector) {
                        BeanCollector collector = (BeanCollector) bean;
                        from = (BeanValue<T>) collector.getBeanFinder().getFilterRange().getAttribute("from");
                        to = (BeanValue<T>) collector.getBeanFinder().getFilterRange().getAttribute("to");
                    }
                    Statistic s = new Statistic(bean.getDeclaringClass(), from.getValue(), to.getValue());
                    s.getPresentable().setIcon("icons/barchart.png");
                    s.saveDefinition();
                    return s;
                }

                @Override
                public boolean isEnabled() {
                    return super.isEnabled() && bean.isPersistable() && !bean.getDeclaringClass().isArray();
                }
            });
            pageActions
            .add(new MethodAction(Replication.getReplicationMethod()) {
                @Override
                public Object action() throws Exception {
                	setParameter(new Replication(((BeanCollector)bean).getCurrentData()));
                	return super.action();
                }

                @Override
                public String getLongDescription() {
                    return "replicates the selected beans";
                }
                
                @Override
                public boolean isEnabled() {
                    return super.isEnabled() && bean.isPersistable() && !bean.getDeclaringClass().isArray();
                }
                
            });
        }
        return pageActions;
    }

    @Override
    public void reset() {
        AttributeCover.resetTypeCache();
        ENV.get(Pool.class).reset();
        super.reset();
        //clear template cache
        jsWebsocketTemplate = null;
        tableDivStyle = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String build(ISession session,
            BeanDefinition<?> model,
            Object message,
            boolean interactive,
            BeanDefinition<?>... navigation) {
        try {
            Element form;
            if (model != null) {
                //TODO: don't forget the old presentation instance
                if (!(model.getPresentationHelper() instanceof Html5Presentation)) {
                    model.setPresentationHelper(createHelper(model));
                }
                form = ((Html5Presentation) model.getPresentationHelper()).createPage(session, null,
                    message instanceof String && !(message.toString().contains(".") && NumberUtil.isNumber(message)) 
                        ? ENV.translate(message, true) : message,
                    interactive,
                    navigation);
            } else {
                form = createPage(session, null, "Leaving Application!<br/>Restart", false, navigation);
            }

            //Some external extensions...
            Plugins.process(IDOMDecorator.class).decorate(form.getOwnerDocument(), session);
            
            String html = HtmlUtil.toString(form.getOwnerDocument());
            if (LOG.isDebugEnabled()) {
                FileUtil.writeBytes(html.getBytes(), ENV.getConfigPath() + "html-server-response.html", false);
            }
            return html;
        } catch (Exception ex) {
            return HtmlUtil.createMessagePage(ENV.translate("tsl2nano.error", true), message + "<p/>" +
                ManagedException.toRuntimeEx(ex, true, true).getMessage());
        }
    }

    Element createFormDocument(ISession session, String name, String image, boolean interactive) {
        isAuthenticated = session.getUserAuthorization() != null;
        Element body = createHeader(session, name, image, interactive);
        Element glasspane = createGlasspane(body);
        Element form = appendElement(glasspane,
            TAG_FORM,
            ATTR_ID,
            "page.form",
            ATTR_ACTION,
            "?",
            ATTR_METHOD,
            ENV.get("html5.http.method", "post"),
            enable("autocomplete", ENV.get("html5.form.autocomplete", true)), null);
        addAntiCSRFTokenToForm(session, form);
        return form;
    }

    protected Element createGlasspane(Element body) {
        /* Display it on the layer with index 1001. cover the whole screen
         * Make sure this is the highest z-index value used by layers on that page 
         */
        return appendElement(body, TAG_DIV, ATTR_ID, "glasspane", ATTR_STYLE,
            ENV.get("app.page.glasspane.style", "background: transparent;z-index:1001;"));
    }

    Element createHeader(ISession session, String title, String image, boolean interactive) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            /*
             * try to read html-page from a template. if not existing, create header and
             * body programatically.
             * TODO: cache the page
             */
            Document doc;
            Element body = null;
            File metaFrame = new File(ENV.getConfigPath() + "css/meta-frame.html");
            boolean useCSS = metaFrame.canRead();
            if (useCSS) {
                try {
                    doc = factory.newDocumentBuilder().parse(metaFrame);
                    NodeList childs = doc.getFirstChild().getChildNodes();
                    for (int i = 0; i < childs.getLength(); i++) {
                        if (childs.item(i).getNodeName().equals(TAG_BODY)) {
                            body = (Element) childs.item(i);
                            break;
                        }
                    }
                    if (body == null) {
                        throw new IllegalStateException("error on loading file " + metaFrame.getAbsolutePath()
                            + ": missing body tag!");
                    }
                } catch (Exception e) {
                    LOG.error("error on loading file " + metaFrame.getAbsolutePath());
                    ManagedException.forward(e);
                    return null;
                }
            } else {
                doc = factory.newDocumentBuilder().newDocument();
                Element html = doc.createElement(TAG_HTML);
                doc.appendChild(html);
                body = createMetaAndBody(session, html, title, interactive);
            }

            return createHeaderElements(session, title, image, interactive, body, useCSS);
        } catch (ParserConfigurationException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    private Element createHeaderElements(ISession session, String title, String image, boolean interactive, Element body,
            boolean useCSS) {
        /*
         * create the header elements:
         * c1: left: App-Info with image
         * c2: Page-Buttons
         * c3: center: Page-Title with image
         */
        Element row = appendTag(createGrid(body, "page.header.table", "page.header.table", 0), TABLE(TAG_ROW));
        Element c1 = appendTag(row, TABLE(TAG_CELL));
        Element c2 = appendTag(row, TABLE(TAG_CELL));
        Element c3 = appendTag(row, TABLE(TAG_CELL));
        String localDoc = ENV.getConfigPath() + "nano.h5.html";
        String docLink =
            new File(localDoc).canRead() ? "./nano.h5.html" : "https://sourceforge.net/p/tsl2nano/wiki/";
        c1 = appendElement(c1, TAG_LINK, ATTR_HREF, docLink);
        appendElement(c1,
            TAG_IMAGE,
            content(),
            ATTR_SRC,
            "icons/beanex-logo-micro.jpg",
            ATTR_TITLE,
            "Framework Version: " + ENV.getBuildInformations());

        if (image != null) {
            c3 = appendElement(c3, TAG_H3, content(), ATTR_ALIGN, ALIGN_CENTER);
            appendElement(c3,
                TAG_IMAGE,
                content(getTitleWithLink(title, session)),
                ATTR_SRC,
                image,
                ATTR_CLASS,
                "title",
                ATTR_STYLE,
                style("display", "inline"));
        } else {
            String docURL;
            if (bean != null && ENV.class.isAssignableFrom(bean.getClazz()))
                docURL = new File("./").getAbsolutePath();
            else
                docURL = ENV.get("doc.url." + bean.getName().toLowerCase(),
                    "doc/" + StringUtil.toFirstLower(title) + "/index.html");
            if (new File(ENV.getConfigPath() + docURL).canRead() || (!docURL.contains(" ") && NetUtil.isURL(docURL))) {
                c3 = appendElement(c3, TAG_H3, ATTR_ALIGN, ALIGN_CENTER, ATTR_STYLE,
                    style("display", "inline"));
                appendElement(c3, TAG_LINK, content(title), ATTR_HREF, ENV.getConfigPath() + docURL, ATTR_CLASS, "title");
            } else {
                c3 = appendElement(c3, TAG_H3, content(getTitleWithLink(title, session)), ATTR_ID, "title", ATTR_ALIGN, ALIGN_CENTER, ATTR_STYLE,
                    style("display", "inline"));
            }
        }
        if (interactive && bean != null) {
            //fallback: setting style from environment-properties
            appendAttributes((Element) c2.getParentNode(), ATTR_STYLE,
                ENV.get("layout.header.grid.style", /*"background-image: url(icons/spe.jpg);"*/"background: transparent;"));
            c2 = appendElement(c2,
                TAG_FORM,
                ATTR_ACTION,
                "?",
                ATTR_METHOD,
                ENV.get("html5.http.method", "post"), ATTR_STYLE,
                style("display", "inline"));
            addAntiCSRFTokenToForm(session, c2);
            if (!useSideNav(99)) {
                Element menu = createMenu(c2, "Menu");
                createSubMenu(menu, ENV.translate("tsl2nano.application", true), "icons/equipment.png",
                    getApplicationActions(session));
                createSubMenu(menu, ENV.translate("tsl2nano.session", true), "icons/home.png",
                    getSessionActions(session));
                createSubMenu(menu, ENV.translate("tsl2nano.page", true), "icons/full_screen.png",
                    getPageActions(session));
            } else {
                Collection<IAction> actions = new ArrayList<IAction>(getPageActions(session));
                actions.addAll(getApplicationActions(session));
                actions.addAll(getSessionActions(session));
                if (!useSideNav(1 + actions.size()))
                   ;//c3 = createExpandable(c3, "Menu", ENV.get("layout.header.menu.open", false));
                createActionPanel(c2, actions,
                    ENV.get("layout.header.button.text.show", true),
                    ATTR_ALIGN, ALIGN_RIGHT);
            }
        }
        return body;
    }

	private void addAntiCSRFTokenToForm(ISession session, Element c2) {
		if (WebSecurity.useAntiCSRFToken() && session instanceof NanoH5Session) {
			String token = ((NanoH5Session)session).createAntiCSRFToken();
			appendElement(c2, TAG_INPUT, ATTR_NAME, WebSecurity.HIDDEN_NAME, ATTR_ID, WebSecurity.HIDDEN_NAME, ATTR_VALUE, token, ATTR_HIDDEN);
		}
	}

    private String getTitleWithLink(String title, ISession session) {
    	if (session.getUserAuthorization() == null) {
    		return title;
    	} else {
	    	String ddlName = StringUtil.substring(Persistence.current().getJarFile(), "/", ".jar", true);
			String defaultUrl = "https://editor.ponyorm.com/user/pony/" + ddlName + "/designer";
			String url = ENV.get("app.database.ddl.designer.url", defaultUrl);
			return "<a href=\"" + url + "\" class=\"title\">" + title + "</a>";
    	}
	}

	private boolean useSideNav(int actionCount) {
        // use sideNav after login...
        return isAuthenticated && ENV.get("layout.sidenav", false) 
                && actionCount > ENV.get("layout.sidenav.min.count.action", 3);
    }

    private Element createMetaAndBody(ISession session, Element html, String title, boolean interactive) {
        appendElement(html, TAG_STYLE,
            content(
                ENV.get("app.frame.style", CSS_BACKGROUND_FADING_KEYFRAMES + tableDivStyles())));
        appendAttributes(html, "manifest", ENV.get("html5.manifest.file", "tsl2nano-appcache.mf"));
        Element head = appendElement(html, TAG_HEAD, ATTR_TITLE, "Nano-H5 Application: " + title);

        appendElement(head, "meta", "name", "author", "content", "tsl2.nano.h5 (by Thomas Schneider/2012-2020)");
        appendElement(head, "meta", "name", "viewport", "content",
            "width=device-width, height=device-height, initial-scale=1");
//        appendElement(head, "link", "rel", "stylesheet", "href", "css/style.css");
        appendElement(head, "meta", "charset", "UTF-8");

        /*
         * WebSocket integration
         */
        if (interactive) {
            createWebSocket(session, head, MSG_FOOTER);
        }

        /*
         * The body
         */
        Element body =
            appendElement(html, TAG_BODY, ATTR_ID, bean != null ? bean.getPresentable().getLabel() : title);
        if (interactive) {
            String style =
                ENV.get("app.page.style", STYLE_BACKGROUND_RADIAL_GRADIENT
                    + STYLE_BACKGROUND_FADING_KEYFRAMES);
            appendAttributes(body, /*"background", "icons/spe.jpg", */ATTR_STYLE,
                style, "ononline", "showStatusMessage('ONLINE')", "onoffline", "showStatusMessage('-- OFFLINE --')");
        }
        return body;
    }

    /**
     * createWebSocket
     * 
     * @param session
     * 
     * @param parent
     */
    private void createWebSocket(ISession session, Element parent, String elementId) {
        if (ENV.get("websocket.use", true)) {
            if (jsWebsocketTemplate == null) {
                InputStream jsStream = ENV.getResource("websocket.client.js.template");
                jsWebsocketTemplate = String.valueOf(FileUtil.getFileData(jsStream, "UTF-8"));
                ENV.get("websocket.window.alert.message", true);
                ENV.get("websocket.speak.alert.message", true);
                ENV.get("app.login.secure", false);
            }
            Element script = appendElement(parent, TAG_SCRIPT, ATTR_TYPE, ATTR_TYPE_JS);

            TreeMap p = new TreeMap<>();
            //on reset, before re-loading ENV, it may be null
            if (ENV.isAvailable())
                p.putAll(ENV.getProperties());
            URL url = NanoH5.getServiceURL(null);
            p.put("websocket.server.ip", url.getHost());
            p.put("websocket.server.port", session.getWebsocketPort());
            p.put("websocket.element.id", elementId);

            String jsWebSocket = StringUtil.insertProperties(jsWebsocketTemplate, p);
            if (ENV.get("app.mode.strict", false))
                if (jsWebSocket.matches("^(//).*\\$\\{\\}"))
                        throw new IllegalStateException("websocket.client.js.template has unfilled variables of type ${...}");
                
            script.appendChild(script.getOwnerDocument().createTextNode(
                jsWebSocket));
        }
    }

    @Override
    public String buildDialog(Object title, Object model) {
        return WSDialog.createHtmlFromBean(String.valueOf(title), model);
    }

    /**
     * builds a full html document
     * 
     * @param session
     * 
     * @param parent (optional) parent element to place itself into
     * @param message (optional) status message to be presented at bottom
     * @param interactive if false, no buttons and edit fields are shown
     * @return html document
     */
    public Element createPage(ISession session,
            Element parent,
            Object message,
            boolean interactive,
            BeanDefinition<?>... navigation) {
        boolean isRoot = parent == null;
        if (isRoot) {
            sideNav = null;
            availableshortCuts = new ArrayList(Arrays.asList(SHORTCUTS));
            tabIndex = -1;
            row1style = "";
            ENV.get("layout.grid.row1.style", "background-color: rgba(128,128,128,.3);");
            row2style = "";
            ENV.get("layout.grid.row2.style", "background-color: rgba(247,247,247,.3);");

            if (bean == null) {
                return createFormDocument(session, message.toString(), null, interactive);
            } else {
                parent =
                    createFormDocument(session, ENV.translate(bean.getPresentable().getLabel(), true), getIcon(bean, null), interactive);
            }
        }

        //navigation bar
        if (interactive && !Util.isEmpty(navigation)) {
            createNavigationbar(parent, navigation);
        }

        Element panel =
            appendElement(parent, TAG_DIV, ATTR_STYLE,
                (interactive ? ENV.get("layout.page.data.style", "overflow: auto; height: 70vh;") : null));
        createContentPanel(session, panel, bean, interactive, ENV.get("layout.page.data.fullwidth", false));

        if (isRoot) {
            if (interactive) {
                createBeanActions(parent, bean);
                createFooter(parent.getOwnerDocument(), message);
            }
        }
        return parent;
    }

    /**
     * createNavigationbar
     * 
     * @param parent
     * @param navigation
     */
    protected void createNavigationbar(Element parent, BeanDefinition<?>... navigation) {
        Element link;
        Element nav = appendElement(parent, "nav", ATTR_ID, "navigation", ATTR_ALIGN, ALIGN_RIGHT);
        for (BeanDefinition<?> bean : navigation) {
            link = appendElement(nav, TAG_LINK, ATTR_HREF, PREFIX_BEANLINK
                + bean.getName()
                /*ATTR_STYLE, ENV.get("layout.page.navigation.section.style", "color: #AAAAAA;")*/);
            appendElement(link, TAG_IMAGE, content(ENV.translate(bean.toString(), true)), ATTR_SRC,
                "icons/goback.png");
        }
    }

    /**
     * createContentPanel
     * 
     * @param session
     * @param parent
     * @param bean
     * @param interactive
     */
    private Element createContentPanel(ISession session,
            Element panel,
            BeanDefinition bean,
            boolean interactive,
            boolean fullwidth) {
//        Element frame = appendElement(parent, "iframe", ATTR_SRC, "#data", ATTR_NAME, "dataframe");
//        panel = appendElement(parent, TAG_LINK, ATTR_NAME, "data", "target", "_blank");
        if (bean instanceof Controller) {
            panel = createController(session, panel, (Controller) bean, interactive, fullwidth);
        } else if (bean instanceof BeanCollector) {
            panel = createCollector(session, panel, (BeanCollector) bean, interactive, fullwidth);
        } else if (bean instanceof Bean) {
            //prefill a new bean with the current navigation stack objects
            if (BeanContainer.isConnected() && BeanContainer.instance().isTransient(((Bean) bean).getInstance())) {
                addSessionValues(session, (Bean) bean);
            }
            panel = createBean(session, panel, (Bean<?>) bean, interactive, fullwidth);
        } else {
            throw new IllegalStateException(bean.toString() + " is only a base definition but must be an explicit instance (Bean) or collection (BeanCollector)!"
                + "\nThis happens, if an extension of BeanCollector crashes on construction. Perhaps wrong database?");
        }
        return panel;
    }

    private Element createController(ISession session,
            Element parent,
            Controller controller,
            boolean interactive,
            boolean fullwidth) {
        Element table = appendTag(parent,
            TABLE(TAG_TABLE,
                /*            ATTR_BORDER,
                border ? "1" : "0",*/
                ATTR_FRAME,
                "box",
                fullwidth ? ATTR_WIDTH : ATTR_ALIGN,
                fullwidth ? VAL_100PERCENT : ALIGN_CENTER,
//                ATTR_BGCOLOR,
//                COLOR_LIGHT_BLUE,
                    ATTR_STYLE,
                    VAL_TRANSPARENT + VAL_ROUNDCORNER,
                enable("sortable", true)));
//        if (Environment.get("html5.table.show.caption", false))
//            appendElement(table, "caption", content(title));
        //fallback: setting style from environment-properties

        if (controller.getPresentable() != null) {
            addAttributes(table, controller.getPresentable(), true);
        }
        createActionTableContent(table, controller, controller.getCurrentData());
        return table;
    }

    @Override
    protected void addSessionValues(ISession session, Bean bean) {
        List<BeanDefinition> v = new ArrayList<>();
        //do the Object-casting trick to cast from List<Object> to List<BeanDefinition>
        Object navigation = Arrays.asList(session.getNavigationStack());
        v.addAll((List<BeanDefinition>) navigation);
        v.addAll(CollectionUtil.getList(((NanoH5Session) session).getContext().get(BeanDefinition.class)));
        addSessionValues(v, bean);
    }

    public static String createMessagePage(String templateName, String message, URL serviceURL) {
        InputStream stream = ENV.getResource(templateName);
        String startPage = String.valueOf(FileUtil.getFileData(stream, null));
        return StringUtil.insertProperties(startPage,
            MapUtil.asMap("url", serviceURL, "text", message));
    }

    public static String embed(String url) {
        //height doesn't work properly - some browsers wont interpret the '%' - setting a fixed height of the given value
        return "<object data=\"" + url + "\" alt=\"" + url
            + " width=\"100%\" height=\"700%\" pluginspage=\"http://www.adobe.com/products/acrobat/readstep2.html\">";
    }

    /**
     * createBean
     * 
     * @param parent
     */
    private Element createBean(ISession session,
            Element parent0,
            Bean<?> bean,
            boolean interactive,
            boolean fullwidth) {
        Collection<ValueGroup> valueGroups = bean.getValueGroups();
        if (Util.isEmpty(valueGroups)) {
            return createFieldPanel(session, parent0, bean.getPresentable(), bean.getBeanValues(), bean.getActions(),
                interactive, fullwidth);
        } else {//work on value groups
//            parent = appendElement(parent, TAG_DIV);
            Element parent;
            Collection<IAction> noActions = new LinkedList<IAction>();
            for (ValueGroup valueGroup : valueGroups) {
                if (!valueGroup.isVisible()) {
                    continue;
                }
//                if (ENV.get("bean.valuegroup.expandable", true) && bean.getValueGroups() != null) {
//                    parent = appendElement(parent0, TAG_EXP_DETAILS, ATTR_TITLE, valueGroup.getLabel(), "open");
//                    parent = appendElement(parent, TAG_EXP_SUMMARY, content(valueGroup.getLabel()));
//                } else {
                parent = parent0;
//                }
                Collection<BeanValue<?>> beanValues =
                    new ArrayList<BeanValue<?>>(valueGroup.getAttributes().size());
                BeanValue bv;
                for (String name : valueGroup.getAttributes().keySet()) {
                    IValueDefinition<?> attr = bean.getAttribute(name);
                    if (attr == null) {
                        throw new IllegalArgumentException("bean-attribute " + name
                            + ", defined in valuegroup not avaiable in bean " + bean);
                    }

                    if (!attr.getPresentation().isVisible()) {
                        continue;
                    }

                    Object v = attr.getValue();

                    if (valueGroup.isDetail(name) && v != null && !Util.isJavaType(attr.getType())) {
                        if (attr.isMultiValue()) {
                            bv =
                                BeanValue.getBeanValue(
                                    BeanCollector.createBeanCollectorHolder((Collection) bean.getValue(name),
                                        IBeanCollector.MODE_ALL),
                                    ValueHolder.ATTR_VALUE);
                            bv.setDescription(name);
                        } else {
//                            bv =
//                                BeanValue.getBeanValue(
//                                    new ValueHolder(Bean.getBean(bean.getValue(name))),
//                                    ValueHolder.ATTR_VALUE);
                            createBean(session, parent, Bean.getBean(bean.getValue(name)), interactive,
                                fullwidth);
                            continue;
                        }
                    } else {
                        bv = BeanValue.getBeanValue(bean.getInstance(), name);
                    }
                    beanValues.add(bv);
                }
                createFieldPanel(session, parent, valueGroup, beanValues, noActions, interactive, fullwidth);
            }
            return parent0;
        }
    }

    private Element createFieldPanel(ISession session,
            Element parent,
            IPresentable p,
            Collection<BeanValue<?>> beanValues,
            Collection<IAction> actions,
            boolean interactive,
            boolean fullwidth) {
        int columns = -1;
        if (p instanceof GroupingPresentable) {
            GroupingPresentable gp = (GroupingPresentable) p;
            columns = gp.getGridWidth();
        } if (columns < 1) {
            int maxrows = ENV.get("layout.panel.maxrowcount", 25);
            int maxcols =
                p.layout(L_GRIDWIDTH, ENV.get("layout.panel.columncount", (AppLoader.isDalvik() ? 3 : 9)));
    
            columns = (int) Math.ceil(beanValues.size() / (float) maxrows) * 3;
            columns = columns > maxcols ? maxcols : columns;
        }
        parent = interactive && ENV.get("layout.field.panel.expandable", true)
            ? createExpandable(parent, p.getDescription(), p.getEnabler().isActive()) : parent;
        Element panel =
            createGrid(parent, ENV.translate("tsl2nano.input", false), "field.panel", fullwidth, 0);
        //fallback: setting style from environment-properties
        if (isBeanConfiguration()) {
            appendAttributes((Element) panel.getParentNode(), "class", "fieldpanel", ATTR_STYLE,
                ENV.get("layout.configurator.grid.style", "background-image: url(icons/art029.jpg);")
                    + VAL_ROUNDCORNER);
        } else {
            appendAttributes((Element) panel.getParentNode(), "class", "fieldpanel", ATTR_STYLE,
                ENV.get("layout.panel.style", "background-image: url(icons/spe.jpg);") + VAL_ROUNDCORNER);
        }
        //set layout and constraints into the grid
        addAttributes((Element) panel.getParentNode(), p, true);
        boolean firstFocused = false;
        int count = 0;
        Element field = null;
        for (BeanValue<?> beanValue : beanValues) {
            if (!beanValue.getPresentation().isVisible()) {
                continue;
            }
            Element fparent = field == null || ((++count * 3) % (columns) == 0) ? panel
                : getRow(field);
            if (beanValue.isBean()) {
                Bean<?> bv = (Bean<?>) beanValue.getInstance();
                ((Html5Presentation) bv.getPresentationHelper())
                    .createPage(session, panel, bv.getName(), interactive);
                actions.addAll(bv.getActions());
            } else if (beanValue.isBeanCollector()) {
                BeanCollector<?, ?> bv = (BeanCollector<?, ?>) ((IValueAccess) beanValue.getInstance()).getValue();
                ((Html5Presentation) bv.getPresentationHelper()).createPage(session, panel,
                    beanValue.getDescription(),
                    interactive);
                actions.addAll(bv.getActions());
            } else if (beanValue.getPresentation().isNesting() && !Util.isEmpty(beanValue.getValue())) {
                BeanDefinition bv =
                    beanValue.isMultiValue() ? BeanCollector.getBeanCollector((Collection) beanValue.getValue(), 0)
                        : Bean.getBean(beanValue.getValue());
                //workaround: should use fparent, but that doesn't work
                createContentPanel(session, parent, bv, interactive, false);
                actions.addAll(bv.getActions());
            } else {
                field = createField(fparent, beanValue, interactive);
                if (!firstFocused) {
                    field.setAttribute(ATTR_AUTOFOCUS, ATTR_AUTOFOCUS);
                    firstFocused = true;
                }
            }
        }
        return panel;
    }

    private Element createExpandable(Element parent, String title, boolean open) {
        parent = appendElement(parent, TAG_EXP_DETAILS, enable(ATTR_EXP_OPEN, open));
        String key = shortCut(++tabIndex);
        appendElement(parent, TAG_EXP_SUMMARY, content(title), ATTR_ACCESSKEY, key, ATTR_TITLE, "ALT+" + key,
            ATTR_ALIGN,
            ALIGN_LEFT, ATTR_STYLE, "color: #6666FF;");
        return parent;
    }

    private Element getRow(Element field) {
        return (Element) field.getParentNode().getParentNode().getParentNode();
    }

    /**
     * createCollector
     * 
     * @param session
     * 
     * @param parent parent
     * @param bean collector to create a table for
     * @param interactive if false, no buttons and edit fields are shown
     * @return html table tag
     */
    Element createCollector(ISession session,
            Element parent,
            BeanCollector<Collection<T>, T> bean,
            boolean interactive,
            boolean fullwidth) {
        /*
         * workaround to enable buttons
         */
        if (bean.getCurrentData().size() == 1
            || (bean.getSelectionProvider().isEmpty() && ENV.get("collector.data.selectfirst", false))) {
            bean.selectFirstElement();
        }

        /*
         * append a quick search panel
         */
        if (interactive && !bean.isSimpleList()) {
            createQuickSearchPanel(parent, bean.getValueExpression().getExpression(), bean.getQuickSearchAction());
        }

        /*
         * create the column header
         */
        bean.addMode(MODE_MULTISELECTION);
        if (ENV.get("layout.collector.expandable", false))
            parent =
                interactive ? createExpandable(parent, bean.getPresentable().getDescription(), bean.getPresentable()
                    .getEnabler().isActive()) : parent;
        Element grid;
        if (interactive) {
            grid = createGrid(parent, bean.toString(), false, bean, fullwidth);
        } else {
            grid = createGrid(parent, bean.toString(), "collector.table", false, fullwidth, getColumnNames(bean));
        }
        //fallback: setting style from environment-properties
        appendAttributes(grid, "class", "beancollector", ATTR_STYLE,
            ENV.get("layout.grid.style", "background: transparent, border: 10;"));

        addAttributes(grid, bean.getPresentable(), true);

        if (interactive && !bean.isSimpleList() && ENV.get("layout.grid.searchrow.show", true)
            && bean.hasMode(IBeanCollector.MODE_SEARCHABLE)
            && (!(session instanceof NanoH5Session) || !((NanoH5Session) session).isMobile())) {
            Collection<T> data = new LinkedList<T>(bean.getSearchPanelBeans());
            //this looks complicated, but if currentdata is a collection with a FilteringIterator, we need a copy of the filtered items!
            if (bean.getCurrentData() != null)
                data.addAll(CollectionUtil.getList(bean.getCurrentData().iterator()));
            createTableContent(session, grid, bean, data, interactive, 0, 1);
        } else {
            if (bean.getCurrentData() != null)
                createTableContent(session, grid, bean, bean.getCurrentData(), interactive);
        }

        return grid;
    }

    private void createQuickSearchPanel(Element parent, String tooltip, IAction<?> action) {
//        parent = appendElement(parent, TAG_FORM);
        parent = appendElement(parent, TAG_DIV, ATTR_ALIGN, VAL_ALIGN_RIGHT);
        Element input =
            appendElement(parent, TAG_INPUT, ATTR_ID, ID_QUICKSEARCH_FIELD, ATTR_NAME, ID_QUICKSEARCH_FIELD, ATTR_TYPE,
                ATTR_TYPE_SEARCH, ATTR_TITLE, tooltip, ATTR_TABINDEX, ++tabIndex + "");
        if (ENV.get("websocket.use.inputassist", true)) {
            appendAttributes(input, "onkeypress",
                ENV.get("websocket.inputassist.function", "inputassist(event)"));
        }
        createAction(parent, action, false);
    }

    /**
     * getColumnNames
     * 
     * @param colDefs
     * @return
     */
    private String[] getColumnNames(BeanCollector<?, ?> collector) {
        List<String> cnames = collector.getColumnLabels();

        cnames.add(0, Messages.getString("tsl2nano.row"));
        return cnames.toArray(new String[0]);
    }

    private void addAttributes(Element grid, IPresentable p, boolean isContainer) {
        addAttributes(grid, null, p, isContainer);
    }

    private void addAttributes(Element grid, String parentBGColor, IPresentable p, boolean isContainer) {
        appendStyle(grid, parentBGColor, STYLE_BACKGROUND_COLOR,
            convert(ATTR_BGCOLOR, p.getBackground(), null), STYLE_COLOR,
            convert(STYLE_COLOR, p.getForeground(), null));

        if (!isContainer) {
            appendAttributes(grid,
                ATTR_ALIGN,
                getTextAlignment(p.getStyle()));
        }
        if (p.getLayout() instanceof Map) {
            appendAttributes(grid, MapUtil.asArray((Map<String, Object>) p.getLayout()));
        }
        //TODO: only layout-constraints should be set
        createLayoutConstraints(grid, p);
    }

    /**
     * fill the given data to the grid
     * 
     * @param grid table grid to add rows to
     * @param columnDefinitions columns
     * @param data collection holding data
     * @param editableRowNumbers 0-based row numbers to be editable
     */
    void createTableContent(ISession session,
            Element grid,
            BeanCollector<?, T> tableDescriptor,
            Collection<T> data,
            boolean interactive,
            Integer... editableRowNumbers) {
        Collection<Integer> editableRows = Arrays.asList(editableRowNumbers);
        ValueExpressionFormat<T> vef = null;
        if (data.size() > 0 && editableRows.size() > 0) {
            vef = new ValueExpressionFormat(BeanClass.getDefiningClass(data.iterator().next().getClass()));
        }
        int i = 0;
        boolean hasSearchFilter = tableDescriptor.getBeanFinder().getFilterRange() != null && !tableDescriptor.isSimpleList();
        tabIndex = data.size() > editableRowNumbers.length ? editableRowNumbers.length * -1 : 0;

        // provide expandable details for search and group panels
        boolean useDetailsInTable = ENV.get("collector.ignore.groupby.expandables", false);
        Element searchDetail =
            hasSearchFilter && useDetailsInTable ? appendElement(grid, TAG_EXP_DETAILS, ATTR_TITLE, "Search") : grid;
        Map<String, Element> groups = new HashMap<>();
        if (tableDescriptor.getGroups() != null && useDetailsInTable) {
            for (GroupBy g : tableDescriptor.getGroups()) {
                groups.put(g.getTitle(), appendElement(grid, TAG_EXP_DETAILS, ATTR_TITLE, g.getTitle(),
                    g.isExpanded() ? ATTR_EXP_OPEN : null));
            }
        }

        for (T item : data) {
            if (hasSearchFilter && editableRows.contains(i) && ENV.get("layout.grid.searchrow.show", true)) {
                addEditableRow(searchDetail, tableDescriptor, item, i == 0 ? prop(KEY_FILTER_FROM_LABEL)
                    : i == 1 ? prop(KEY_FILTER_TO_LABEL) : toString(item, vef));
            } else {
                GroupBy group = useDetailsInTable ? tableDescriptor.getGroupByFor(item) : null;
                addRow(session, group != null ? groups.get(group.getTitle()) : grid,
                    tableDescriptor.hasMode(MODE_MULTISELECTION) && interactive, tableDescriptor,
                    item, interactive, i);
            }
            i++;
        }
        if (!tableDescriptor.isSimpleList())
            createTableFooter(session, grid, tableDescriptor);
    }

    private void createTableFooter(ISession session, Element grid, BeanCollector<?, T> tableDescriptor) {
        Element footer =
            appendElement(grid, "tfoot", ATTR_STYLE, ENV.get("layout.grid.footer.style", ""));
        Element footerRow = appendTag(footer, TABLE(TAG_ROW));

        //summary
        List<IPresentableColumn> columns = tableDescriptor.getColumnDefinitionsIndexSorted();
        Element sum = appendElement(footerRow, TABLE(TAG_CELL)[0], content(""));
        //collectors will not be used in context?
//        ((Context) session.getContext()).add(tableDescriptor);
        Map contextParameter = new PrivateAccessor<>(session).call("getContextParameter", Map.class);
        boolean hasSummary = false;
        for (IPresentableColumn c : columns) {
            String text = tableDescriptor.getSummaryText(contextParameter, c.getIndex());
            appendElement(footerRow, TABLE(TAG_CELL)[0], content(text));
            if (text.length() > 0) {
                hasSummary = true;
            }
        }
        if (hasSummary) {
            appendElement(sum, TAG_PARAGRAPH, content(CHAR_SUM), ATTR_ALIGN, VAL_ALIGN_CENTER);
        }

        //search-count
        footerRow = appendTag(footer, TABLE(TAG_ROW));
        appendTag(footerRow,
            TABLE(TAG_CELL,
                content(tableDescriptor.getSummary()),
                "colspan",
                String.valueOf(tableDescriptor.getColumnDefinitions().size() + 1)));
    }

    /**
     * fill the given data to the grid
     * 
     * @param grid              table grid to add rows to
     * @param columnDefinitions columns
     * @param data              collection holding data
     */
    void createActionTableContent(Element grid,
            Controller<?, T> tableDescriptor,
            Collection<T> data) {
        tabIndex = 0;
        LinkedList<T> collectedItems = new LinkedList<>();
        for (T item : data) {
            addActionRow(grid, tableDescriptor, item, collectedItems);
        }
        Element footer = appendElement(grid, "tfoot");
        Element footerRow = appendTag(footer, TABLE(TAG_ROW));
        appendTag(footerRow,
            TABLE(TAG_CELL,
                content(tableDescriptor.getSummary()),
                "colspan",
                String.valueOf(tableDescriptor.getColumnDefinitions().size() + 1)));
    }

    public static <T> String toString(T item, ValueExpressionFormat<T> vef) {
        return item instanceof BeanDefinition ? item.toString() : vef.format(item);
    }

    /**
     * creates a css3 specific menu. see {@link #createSubMenu(Element, String, Collection, String...)}.
     * 
     * @param parent parent element
     * @param name menu name
     * @param attributes additional attributes
     * @return menu root element
     */
    private Element createMenu(Element parent, String name, String... attributes) {
        Element div = appendElement(parent, "div"/*, "class", "wrap"*/);
        Element nav = appendElement(div, "nav", attributes);
        return appendElement(nav, "ul", "class", "menu");
    }

    /**
     * creates an html5 menu (most browser don't support the tag 'menu'). this implementation uses a specific css3-menu.
     * 
     * @param menu parent
     * @param name submenu name
     * @param actions menu items
     * @param attributes additional attributes to be set on each menu-item
     * @return sub-menu element
     */
    private Element createSubMenu(Element menu,
            String name,
            String icon,
            Collection<IAction> actions,
            String... attributes) {
        if (Util.isEmpty(actions))
            return null;
        /*
         * the main sub menu item will use the first action to link to...
         */
        Element list = appendElement(menu, "li");
        Element alink =
            appendElement(list, TAG_LINK, content(name));
        appendElement(alink, TAG_IMAGE, ATTR_SRC, icon);
        Element sub = appendElement(list, "ul");
        for (IAction a : actions) {
            Element li = appendElement(sub, "li");
            createAction(li, a);
//            li = appendElement(li, TAG_LINK, content(Messages.stripMnemonics(a.getShortDescription())),
//                enable(ATTR_HREF, a.isEnabled()),
//                enable(PREFIX_ACTION + a.getId(), a.isEnabled()));
//            appendElement(li, TAG_IMAGE, ATTR_SRC, a.getImagePath());
        }
        return list;
    }

    private Element createMenuEnd(Element parent) {
        return appendElement(parent, "div", "class", "clearfix");
    }

    private Element createActionPanel(Element parent,
            Collection<IAction> actions,
            boolean showText,
            String... attributes) {
        String width = "-1";
        Element cell = null;
        try {
            if (useSideNav(actions.size())) {
                cell = sideNav = createSidebarNavMenuButton(parent, sideNav);
                width = setTemporaryFullWidth();
            } else {
                if (useSideNav(Integer.MAX_VALUE))
                    width = setTemporaryFullWidth();
                Element panel =
                    createGrid(parent, "Actions", "action.panel", /*actions != null ? 1 + actions.size() : 1*/0);
                Element row = appendTag(panel, TABLE(TAG_ROW, ATTR_CLASS, "actionpanel"));
                cell = appendTag(row, TABLE(TAG_CELL, attributes));
            }
            appendAttributes(cell, ATTR_STYLE, ENV.get("layout.action.panel", ""));
            if (actions != null) {
                for (IAction a : actions) {
                    if (ENV.get("layout.action.show.disabled", true) || a.isEnabled())
                        createAction(cell, a, showText);
                }
            }
        } finally {
            if (useSideNav(actions.size())) {
                if (width.equals("-1"))
                    ENV.setProperty("layout.action.width", width);
            }
        }
        return cell;
    }

    private String setTemporaryFullWidth() {
        String width;
        width = ENV.get("layout.action.width", "-1");
        if (width.equals("-1"))
            ENV.setProperty("layout.action.width", "13em");
        return width;
    }

    private Element createAction(Element cell, IAction a) {
        return createAction(cell, a, true);
    }

    /**
     * createAction
     * 
     * @param cell
     * @param a
     * @return
     */
    private Element createAction(Element cell, IAction a, boolean showText) {
        String path;
        IPresentable p = a instanceof IsPresentable ? ((IsPresentable)a).getPresentable() : null;
        String imagePath = evaluateImagePath(a, p);
            
        Element element = createAction(cell,
            a.getId(),
            showText && !a.getShortDescription().equals("...") && !a.getShortDescription().isEmpty()
                ? getCSSPanelAction() : CSS_CLASS_ACTION,
            showText ? a.getShortDescription() : null,
            a.getLongDescription(),
            "submit",
            a.getKeyStroke(),
            imagePath,
            a.isEnabled(),
            a.isDefault(),
            a.getActionMode() != IAction.MODE_DLG_OK);
        if (p != null)
            addAttributes(element, p, false);
        return element;
    }

    private String evaluateImagePath(IAction a, IPresentable p) {
        String path;
        String imagePath;
        if (a.getImagePath() != null) {
            if (!a.getImagePath().equals(MethodAction.DEFAULT_ICON))
                imagePath = a.getImagePath();
            else if (p != null && p.getIcon() != null)
                imagePath = p.getIcon();
            else
                imagePath = a.getImagePath();
        } else {
            imagePath = new File(ENV.getConfigPathRel() + (path = "icons/" + StringUtil.substring(a.getId(), ".", null, true)
            + ".png")).exists() ? path 
                : ICON_DEFAULT;
        }
        return imagePath;
    }

    private String getCSSPanelAction() {
        return useSideNav(Integer.MAX_VALUE) ? CSS_CLASS_PANEL_ACTION : "panelactionsimple";
    }

    /**
     * creates html buttons
     * 
     * @param form
     * @param model
     * @return html table containing the buttons
     */
    Element createBeanActions(Element form, BeanDefinition<?> model) {
        Element panel = createActionPanel(form, model.getActions(), true, ATTR_ALIGN, ALIGN_CENTER, ATTR_WIDTH, VAL_100PERCENT);
        if (model.isMultiValue() && model instanceof BeanCollector
            && ((BeanCollector) model).hasMode(MODE_ASSIGNABLE)) {
            String assignLabel = Messages.getStringOpt("tsl2nano.assign", true);

            createAction(panel, BTN_ASSIGN, getCSSPanelAction(), assignLabel, assignLabel, "submit", null,
                "icons/links.png", true, true,
                false);
        }
        createCloseAction(panel);
        return panel;
    }

    void createCloseAction(Element panel) {
        String closeLabel = Messages.getStringOpt("tsl2nano.close", true);
        createAction(panel, IAction.CANCELED, getCSSPanelAction(), closeLabel, closeLabel, null, null, "icons/stop.png",
            true, false, true);
    }

    /**
     * createAction
     * 
     * @param cell parent cell to put the button into
     * @param id action id
     * @param type action type (see {@link HtmlUtil}
     * @return html button element
     */
    Element createAction(Element cell, String id, String type, String image) {
        String label = ENV.translate(id, true);
        return createAction(cell, id, CSS_CLASS_ACTION, label, label, type, null, image, true, false, false);
    }

    Element createAction(Element cell,
            String id,
            String cssClass,
            String label,
            String tooltip,
            String type,
            Object shortcut,
            String image,
            boolean enabled,
            boolean asDefault,
            boolean formnovalidate) {
        return createAction(cell, id, cssClass, label, tooltip, type, shortcut, image, 
            ENV.get("layout.action.width", "-1"), enabled, asDefault, formnovalidate);
    }

    /**
     * createAction
     * 
     * @param cell parent cell to put the button into
     * @param id action id
     * @param label button text
     * @param type action type (see {@link HtmlUtil}
     * @return html button element
     */
    Element createAction(Element cell,
            String id,
            String cssClass,
            String label,
            String tooltip,
            String type,
            Object shortcut,
            String image,
            String width,
            boolean enabled,
            boolean asDefault,
            boolean formnovalidate) {
        String name = label != null ? label : tooltip;
        int isc = name.indexOf('&') + 1;
        String sc;
        if (shortcut != null) {
            sc = shortcut.toString();
        } else {
            sc = isc < name.length() ? name.substring(isc, isc + 1).toLowerCase() : "?";
        }
        sc = checkedShortCut(sc.charAt(0));
        label = Messages.stripMnemonics(label);
        tooltip = tooltip + " (ALT+" + sc + ")";
        //type = type != null ? type : "button"; //close-buttons not working on iexplorer
        Element action = appendElement(cell,
            TAG_BUTTON,
            content(label),
            ATTR_ID,
            id,
            ATTR_CLASS,
            cssClass,
            ATTR_NAME,
            id,
            ATTR_TITLE,
            tooltip,
            enable(ATTR_TYPE, type != null),
            type,
            enable("onclick", type != null),
            enable("fade(this)", type != null && ENV.get("websocket.use", true)),
            ATTR_ACCESSKEY,
            sc,
            ATTR_FORMTARGET,
            VAL_FRM_SELF,
            /*ATTR_STYLE,
            VAL_OPAC,*/
            enable(ATTR_DISABLED, !enabled),
            null,
            enable(ATTR_FORMNOVALIDATE, formnovalidate),
            null,
            enable(ATTR_AUTOFOCUS, asDefault),
            null);
        if (image != null) {
            appendElement(action, TAG_IMAGE, ATTR_SRC, image, ATTR_ALT, label);
        }
        String style =
            ENV.get("layout.action.style", ""/*STYLE_BACKGROUND_TRANSPARENT + style(STYLE_COLOR, COLOR_WHITE)*/);
        if (!width.equals("-1")) {
            style += (style.isEmpty() || style.endsWith(";") ? "" : ";") + style(ATTR_WIDTH, width);
        }
        appendAttributes(action, ATTR_STYLE, style);
        return action;
    }

    Element createGrid(Element parent, String title, String id, int columns) {
        return createGrid(parent, title, id, columns, false, true);
    }

    Element createGrid(Element parent, String title, String id, boolean fullwidth, int columns) {
        return createGrid(parent, title, id, columns, false, fullwidth);
    }

    Element createGrid(Element parent, String title, String id, int columns, boolean border, boolean fullwidth) {
        String c[] = new String[columns];
        Arrays.fill(c, "");
        return createGrid(parent, title, id, border, fullwidth, c);
    }

    Element createGrid(Element parent, String title, String id, boolean border, boolean fullwidth, String... columns) {
        Element table = appendTag(parent,
            TABLE(TAG_TABLE,
                ATTR_BORDER,
                border ? ENV.get("layout.grid.border.width", "10") : "0",
                ATTR_ID,
                id,
                ATTR_FRAME,
                "box",
                fullwidth ? ATTR_WIDTH : ATTR_ALIGN,
                fullwidth ? VAL_100PERCENT : ALIGN_CENTER,
                /*ATTR_BGCOLOR,
                COLOR_LIGHT_BLUE,
                ATTR_STYLE,
                VAL_OPACITY_0_5,*/
                enable("sortable", true)));
        if (ENV.get("layout.grid.caption.show", false)) {
            appendElement(table, TABLE(TAG_CAPTION)[0], content(title));
        }
        if (columns != null && columns.length > 0) {
            Element head = appendTag(table, TABLE(TAG_THEAD));
            Element colgroup = appendTag(head, TABLE(TAG_ROW));
            for (int i = 0; i < columns.length; i++) {
                appendElement(colgroup, TABLE(TAG_HEADERCELL)[0], content(columns[i]), ATTR_STYLE,
                    ENV.get("layout.grid.header.column.style", ""));
            }
        }
        return appendTag(table, TABLE(TAG_TBODY));
    }

    Element createGrid(Element parent, String title, boolean border, BeanCollector<?, ?> collector, boolean fullwidth) {
        Element table = appendTag(parent,
            TABLE(TAG_TABLE,
                ATTR_BORDER,
                border ? ENV.get("layout.grid.border.width", "10") : "0",
                ATTR_FRAME,
                "box", /*"contenteditable", "true",*/
                fullwidth ? ATTR_WIDTH : ATTR_ALIGN,
                fullwidth ? VAL_100PERCENT : ALIGN_CENTER,
                /*ATTR_BGCOLOR,
                COLOR_LIGHT_BLUE,*/
                ATTR_STYLE,
                VAL_TRANSPARENT + VAL_ROUNDCORNER,
                enable("sortable", true)));
        if (!collector.isSimpleList()) {
            if (ENV.get("layout.grid.caption.show", false)) {
                appendElement(table, TABLE(TAG_CAPTION)[0], content(title));
            }
            Element head = appendTag(table, TABLE(TAG_THEAD));
            if (collector.getPresentable() != null) {
                addAttributes(table, collector.getPresentable(), true);
            }
            Element colgroup = appendTag(head, TABLE(TAG_ROW, ATTR_BORDER, "1"));
            if (collector.hasMode(MODE_MULTISELECTION)) {
                appendElement(colgroup, TABLE(TAG_HEADERCELL)[0], content());
            }
            Collection<IPresentableColumn> columns = collector.getColumnDefinitionsIndexSorted();
            for (IPresentableColumn c : columns) {
                Element th = appendTag(colgroup,
                    TABLE(TAG_HEADERCELL,
                        ATTR_ID,
                        c.getIndex() + ":" + c.getName(),
                        ATTR_BORDER,
                        "1",
                        ATTR_WIDTH,
                        Presentable.asText(c.getWidth()),
                        //                "style",
    //                "-webkit-transform: scale(1.2);",
                        ATTR_STYLE,
                        ENV.get("layout.grid.header.column.style", "")));
                if (c.getPresentable() != null) {
                    addAttributes(th, c.getPresentable(), true);
                }
                createAction(th, c.getSortingAction(collector));
            }
        }
        return appendTag(table, TABLE(TAG_TBODY));
    }

    protected Element addRow(ISession session,
            Element grid,
            boolean multiSelection,
            BeanCollector<?, T> tableDescriptor,
            T item,
            boolean interactive,
            int index) {
        boolean isSelected = tableDescriptor.getSelectionProvider() != null ? tableDescriptor.getSelectionProvider()
            .getValue()
            .contains(item) : false;
        tableDescriptor.nextRow();
        Bean<?> itemBean = Bean.getBean(item);
        String beanStyle = itemBean.getPresentable().layout(ATTR_STYLE);
        String rowBackground;
        if (beanStyle != null) {
            rowBackground = beanStyle;
        } else {
            rowBackground = grid.getChildNodes().getLength() % 2 == 0 ? row1style : row2style;
        }

        String tab = String.valueOf(++tabIndex);
        String shortCut = shortCut(tabIndex);
        Element row;
        // pack all items to one row - let the renderer decide to do carriage returns
        if (tableDescriptor.isSimpleList() && grid.getElementsByTagName(TAG_ROW).getLength() > 0) {
            row = (Element)grid.getElementsByTagName(TAG_ROW).item(0);
        } else { // create a row for each item
            row = appendTag(grid,
                TABLE(TAG_ROW,
                    ATTR_ID,
                    Util.asString(itemBean.getId()),
                    ATTR_CLASS,
                    "beancollectorrow",
                    ATTR_TABINDEX,
                    tab,
                    ATTR_ACCESSKEY,
                    shortCut,
                    ATTR_TITLE,
                    "ALT+" + shortCut + (LOG.isDebugEnabled() ? "\n"
                        + StringUtil.toFormattedString(BeanUtil.toValueMap(item), 80, true) : ""),
                    ATTR_FORMTARGET,
                    VAL_FRM_SELF,
                    ATTR_STYLE, row1style
                /*ATTR_BGCOLOR, rowBGColor*/));
                
                if (!tableDescriptor.isSimpleList()) {
                    appendAttributes(row, "ondblclick", "location=this.getElementsByTagName('a')[0];disablePage(e);",
                    "onclick",
                    true/*tableDescriptor.hasMode(IBeanCollector.MODE_EDITABLE)*/
                        ? "/*if (e.originalEvent.detail < 2 || window.event.originalEvent.detail < 2) */this.getElementsByTagName('input')[0].checked = !this.getElementsByTagName('input')[0].checked"
                        : null
                    );
                }
        }
        //first cell: bean reference as link
        String length = String.valueOf(index/*grid.getChildNodes().getLength() - 1*/);
        Element firstCell = appendTag(row, TABLE(TAG_CELL));
        appendElement(firstCell, TAG_LINK, content(), ATTR_HREF, length);
        if (multiSelection) {
            appendElement(firstCell,
                TAG_INPUT,
                content(),
                ATTR_NAME,
                length,
                ATTR_TYPE,
                "checkbox",
                enable(ATTR_CHECKED, isSelected));
        }

        String icon = getIcon(itemBean, item);
        if (icon != null) {
            String iconOrImage = tableDescriptor.isSimpleList() ? "image" : "icon";
            String width = ENV.get("layout." + iconOrImage + ".width", tableDescriptor.isSimpleList() ? "200" : "-1");
            String height = ENV.get("layout."+ iconOrImage + ".height", tableDescriptor.isSimpleList() ? "200" : "-1");
            appendElement(firstCell, TAG_IMAGE, ATTR_SRC, icon, ATTR_ALT, itemBean.toString(), ATTR_TITLE, itemBean.toString()
                , ATTR_WIDTH, width, ATTR_HEIGHT, height);

            if (tableDescriptor.isSimpleList()) {
                appendAttributes(firstCell, "ondblclick", "location=this.getElementsByTagName('a')[0];disablePage(e);",
                "onclick",
                true/*tableDescriptor.hasMode(IBeanCollector.MODE_EDITABLE)*/
                    ? "/*if (e.originalEvent.detail < 2 || window.event.originalEvent.detail < 2) */this.getElementsByTagName('input')[0].checked = !this.getElementsByTagName('input')[0].checked"
                    : null
                );
                return row;
            }
        }
        Collection<IPresentableColumn> colDefs = tableDescriptor.getColumnDefinitionsIndexSorted();
        Element cell;
        String value;
        IValueDefinition<?> attr;
        IPresentable colPres;
        if (colDefs.size() > 0) {
            for (IPresentableColumn c : colDefs) {
                attr = itemBean.hasAttribute(c.getName()) ? itemBean.getAttribute(c.getName()) : null;
                colPres = attr != null && attr.getColumnDefinition() != null 
                    ? attr.getColumnDefinition().getPresentable() : c.getPresentable();
                //on byte[] show an image through attached file
                //workaround: on virtuals searching the attribute may cause an error
                //TODO: if a virtual bean is empty, no attributes are available --> show a warning!
                if (attr != null && BitUtil.hasBit(attr.getPresentation().getType(),
                    TYPE_DATA,
                    TYPE_ATTACHMENT)) {
                    BeanValue<?> beanValue = (BeanValue<?>) attr;
                    File valueFile = beanValue.getValueFile();
                    value = valueFile != null ? valueFile.getPath() : null;
                    cell =
                        appendTag(row, TABLE(TAG_CELL, ATTR_TITLE,
                            itemBean.toString() + ": " + ENV.translate(c.getName(), true),
                            ATTR_HEADERS, c.getIndex() + ":" + c.getName(), ATTR_ID,
                            tableDescriptor.getId() + "[" + tabIndex
                                + ", " + c.getIndex() + "]"));
                    Element data = createDataTag(cell, beanValue);
                    if (colPres != null) {
                        addAttributes(data, rowBackground, colPres, false);
                        addAttributes(cell, rowBackground, colPres, false);
                    }
                    if (value != null && Messages.isMarkedAsProblem(value)) {
                        appendAttributes(cell, ATTR_COLOR, COLOR_RED);
                    }
                } else if (attr != null && tableDescriptor.hasMode(IBeanCollector.MODE_SHOW_NESTINGDETAILS)
                    && !BeanUtil.isStandardType(attr.getType())
                    && attr.getValue() != null) {//nesting panels
                    cell =
                        appendTag(row, TABLE(TAG_CELL, ATTR_TITLE,
                            itemBean.toString() + ": " + ENV.translate(c.getName(), true),
                            ATTR_HEADERS, c.getIndex() + ":" + c.getName(), ATTR_ID,
                            tableDescriptor.getId() + "[" + tabIndex
                                + ", " + c.getIndex() + "]"));
//                    cell = appendElement(cell, TAG_EMBED);
                    createContentPanel(session, cell,
                        Bean.getBean(attr.getValue()), interactive, false);
                } else {//standard --> text
                    value = tableDescriptor.getColumnText(item, c.getIndex());
                    cell =
                        appendTag(row, TABLE(TAG_CELL, content(value), ATTR_TITLE,
                            itemBean.toString() + ": " + ENV.translate(c.getName(), true),
                            ATTR_HEADERS, c.getIndex() + ":" + c.getName(), ATTR_ID,
                            tableDescriptor.getId() + "[" + tabIndex
                                + ", " + c.getIndex() + "]"));
                    if (colPres != null) {
                        addAttributes(cell, rowBackground, colPres, false);
                    }
                    if (Messages.isMarkedAsProblem(value)) {
                        appendAttributes(cell, ATTR_COLOR, COLOR_RED);
                    }
                    if (attr != null)
                        addManyToOnePicture(cell, attr);
                }
            }
        } else {//don't show only an empty entry!
            value = StringUtil.toString(item, Integer.MAX_VALUE);
            cell =
                appendTag(row, TABLE(TAG_CELL, content(value), ATTR_TITLE,
                    itemBean.hashCode() + ": " + ENV.translate("toString", true),
                    ATTR_HEADERS, "0: toString", ATTR_ID,
                    tableDescriptor.getId() + "[" + tabIndex
                        + ", 0" + "]"));
            if (Messages.isMarkedAsProblem(value)) {
                appendAttributes(cell, ATTR_COLOR, COLOR_RED);
            }
        }
        return row;
    }

    private void addManyToOnePicture(Element cell, IValueDefinition<?> attr) {
        if (BeanContainer.instance().isPersistable(attr.getType())) {
            addPicture(cell, attr.getType(), attr.getValue());
        }
    }
    private void addPicture(Element cell, Class<?> type, Object instance) {
        if (instance != null) {
            String iconFromField = BeanDefinition.getBeanDefinition(type).getPresentable().getIconFromField();
            if (iconFromField != null) {
                    createDataTag(cell, (BeanValue<?>) Bean.getBean(instance).getAttribute(iconFromField));
            }
        }
    }

    /**
     * getIcon
     * 
     * @param item
     * @param bean
     * @return
     */
    String getIcon(BeanDefinition bean, Object item) {
        String icon = null;
        if (item == null && bean instanceof Bean)
            item = ((Bean) bean).getInstance();
        //on data, use a generic way: search inside the environment for a name like the value-expression
        if (item != null) {
            String value = bean.getValueExpression().to(item);
            value = !Util.isEmpty(value) ? StringUtil.extract(value, "\\w+") : value;
            if (!Util.isEmpty(value)) {
                //we have to distinguish between java runtime and browser path 
                String name = "icons/" + value.toLowerCase();
                if (new File(ENV.getConfigPath() + name + ".jpg").canRead())
                    icon = name + ".jpg";
                else if (new File(ENV.getConfigPath() + name + ".png").canRead())
                    icon = name + ".png";
                else if (new File(ENV.getConfigPath() + name + ".gif").canRead())
                    icon = name + ".gif";
            }
        }
        //check the presentable for an icon 
        if (icon == null) {
            icon = (icon = bean.getPresentable().getIcon()) != null ? icon : item instanceof BeanDefinition
                && (icon = ((BeanDefinition<?>) item).getPresentable().getIcon()) != null ? icon : null;
        }
        return icon;
    }

    private String dark(String color) {
        //TODO: calculate lighter or darker color
        return COLOR_LIGHTER_BLUE;
    }

    /**
     * shortCut for tabindex
     * 
     * @param index tab index
     * @return unused shortcut
     */
    protected String shortCut(int index) {
        return checkedShortCut((char) (index + /*33*/85));
    }

    protected String checkedShortCut(Character c) {
        //if there are more than 225 buttons, no check will be done!
        if (!Util.isEmpty(availableshortCuts) && !availableshortCuts.remove(c)) {
            c = availableshortCuts.remove(0);
        }
        return String.valueOf(c);
    }

    /**
     * addEditableRow
     * 
     * @param <T>
     * @param table xml table element
     * @param tableDescriptor table descriptor holding this row
     * @param element row item
     * @param rowName (optional) first additional column label for this row
     * @return row element containing all column values of given element (=row item). all cells will have a name of
     *         type: rowname.colname
     */
    Element addEditableRow(Element table, BeanCollector<?, T> tableDescriptor, T element, String rowName) {
        Element row = appendTag(table, TABLE(TAG_ROW, ATTR_STYLE,
            ENV.get("layout.grid.searchrow.style", STYLE_BACKGROUND_LIGHTGRAY), ATTR_ALIGN, ALIGN_CENTER));
        if (rowName != null) {
            appendAttributes(row, ATTR_CLASS, "beancollectorsearchrow");
            Element r = appendTag(row, TABLE(TAG_CELL));
            appendElement(r, TAG_PARAGRAPH, content(rowName), ATTR_ALIGN, VAL_ALIGN_CENTER);
        }

        boolean focusSet = false;
        Collection<IPresentableColumn> colDefs = tableDescriptor.getColumnDefinitionsIndexSorted();
        for (IPresentableColumn c : colDefs) {
            String value = Util.asString(tableDescriptor.getColumnText(element, c.getIndex(), false));
            Element cell0 = appendTag(row, TABLE(TAG_CELL));
            Element cell = appendElement(cell0, TAG_SPAN);
            appendElement(cell,
                TAG_INPUT,
                content(),
                ATTR_ID,
                rowName + "." + c.getName(),
                ATTR_NAME,
                rowName + "." + c.getName(),
                ATTR_TYPE,
                ATTR_TYPE_SEARCH,
                //                ATTR_SIZE,/* 'width' doesn't work, so we set the displaying char-size */
//                Presentable.asText(c.getWidth()),
//                ATTR_WIDTH,
//                Presentable.asText(c.getWidth()),
                ATTR_BGCOLOR,
                COLOR_WHITE,
                ATTR_TABINDEX,
                ++tabIndex + "",
                ATTR_VALUE,
                value,
                enable(ATTR_AUTOFOCUS, !focusSet),
                enable(ATTR_DISABLED, !tableDescriptor.getAttribute(c.getName()).getPresentation().isSearchable()));
            focusSet = true;
        }
        return row;
    }

    Element addRow(Element table, Element... cells) {
        Element row = appendTag(table, TABLE(TAG_ROW));
        Element rowColumn = appendTag(row, TABLE(TAG_CELL));
        for (int i = 0; i < cells.length; i++) {
            rowColumn.appendChild(cells[i]);
        }
        return row;
    }

    protected Element addActionRow(Element grid, Controller<?, T> controller, T item, List<T> collectedItems) {
        Bean<T> itemBean = controller.getBean(item, collectedItems);
        boolean hidden = (controller.isCreationOnly() && !controller.showText() && itemBean.getActions().size() == 0);
        if (hidden) {//hidden seems not to work on chrome and edge
            tabIndex++;
            return null;
        }
        Element row =
            appendTag(grid,
                TABLE(TAG_ROW,
                    ATTR_ID,
                    itemBean.toString(),
                    ATTR_TABINDEX,
                    ++tabIndex + "",
                    ATTR_FORMTARGET,
                    VAL_FRM_SELF,
                    ATTR_WIDTH, VAL_100PERCENT,
                    ATTR_BGCOLOR, itemBean.getPresentable().layout(ATTR_BGCOLOR),
                    enableFlag(ATTR_HIDDEN, hidden) //seems not to work on chrome and edge
                    ));


        Collection<IAction> actions = itemBean.getActions();
        //first cell: bean reference
        Element cell;
        if (controller.showText() || actions.size() == 0)
            cell = appendElement(row, TABLE(TAG_CELL)[0], content(itemBean.toString()));
        for (IAction a : actions) {
            cell = appendTag(row, TABLE(TAG_CELL));
            Element btn = createAction(cell, a);
            btn.setAttribute(ATTR_NAME, Controller.createActionName(tabIndex, a.getId()));
            btn.setAttribute(ATTR_STYLE, ENV.get("layout.controller.action.style", 
                STYLE_BACKGROUND_TRANSPARENT + style(STYLE_COLOR, COLOR_WHITE) + style(ATTR_WIDTH, VAL_100PERCENT)));
            addPicture(btn, item.getClass(), item);
        }
        return row;
    }

    Element createField(Element parent, BeanValue<?> beanValue, boolean interactive) {
        IPresentable p = beanValue.getPresentation();
        boolean multiLineText = isMultiline(p);
        Element row =
            parent.getNodeName().equals(TAG_ROW) || TAG_ROW.equals(parent.getAttribute(ATTR_CLASS)) ? parent
                : appendTag(parent, TABLE(TAG_ROW)/*, ATTR_STYLE, VAL_OPAC*/);
        //first the label
        createFieldLabel(beanValue, p, row);
        //create the layout and layout-constraints
        Element cell = appendTag(row, TABLE(TAG_CELL));
        cell = createLayoutConstraints(cell, p);
        cell = createLayout(cell, p);
        //now the field itself
        Element input = null;
        if (isData(beanValue)) {
            input = createDataTag(cell, beanValue);
            addAttributes(input, p, false);
        }
        if (beanValue.getConstraint().getAllowedValues() == null) {
            if (input == null || BitUtil.hasBit(p.getType(), TYPE_ATTACHMENT)) {
                RegExpFormat regexpFormat =
                    beanValue.getFormat() instanceof RegExpFormat ? (RegExpFormat) beanValue.getFormat()
                        : null;
                String type = getType(beanValue);
                String width = Presentable.asText(p.getWidth());
                if (width == null) {
                    width = ENV.get("field.input.size", "50");
                } else if (p.getWidth() == IPresentable.UNUSABLE) {
                    width = null;
                }
                boolean isOption = "checkbox".equals(type);
                //Attention on Autoboxing with null values!
                boolean hasTrueValue =
                    isOption && beanValue.getValue() != null ? (Boolean) beanValue.getValue() : false;

                input =
                    appendElement(
                        cell,
                        multiLineText ? TAG_TEXTAREA : TAG_INPUT,
                        /*content(getSuffix(regexpFormat)),*/
                        ATTR_TYPE,
                        type,
                        ATTR_ID,
                        beanValue.getId(),
                        ATTR_CLASS,
                        "beanfieldinput",
                        ATTR_NAME,
                        beanValue.getName(),
                        ATTR_PATTERN,
                        regexpFormat != null ? regexpFormat.getPattern() : ENV.get("field.pattern.regexp", ".*"),
                        ATTR_STYLE,
                        getTextAlignmentAsStyle(p.getStyle()),
                        ATTR_SIZE, /* 'width' doesn't work, so we set the displaying char-size */
                        width,
                        ATTR_WIDTH,
                        width,
                        ATTR_MIN,
                        StringUtil.toString(beanValue.getConstraint().getMinimum()),
                        ATTR_MAX,
                        StringUtil.toString(beanValue.getConstraint().getMaximum()),
                        ATTR_MAXLENGTH,
                        (beanValue.length() > 0 ? String.valueOf(beanValue.length()) : String
                            .valueOf(Integer.MAX_VALUE)),
                        (isOption ? enable(ATTR_CHECKED, hasTrueValue) : ATTR_VALUE),
                        (isOption ? (hasTrueValue ? "checked" : null) : getValue(beanValue, type)),
                        ATTR_TITLE,
                        p.getDescription()
                            + (LOG.isDebugEnabled() ? "\n\n" + "<![CDATA[" + beanValue.toDebugString() + "]]>" : ""),
                        ATTR_TABINDEX,
                        p.layout("tabindex", ++tabIndex).toString(),
                        enableFlag(ATTR_HIDDEN, !p.isVisible()),
                        enableFlag(ATTR_DISABLED, !interactive || !p.getEnabler().isActive()),
                        enableFlag(ATTR_READONLY,
                            !interactive || !p.getEnabler().isActive() || beanValue.composition()),
                        enableFlag(ATTR_REQUIRED, !beanValue.nullable() && !beanValue.generatedValue()));

                if (multiLineText) {
                    appendAttributes(input, ATTR_ROWS, p.layout("rows", "5"), ATTR_COLS,
                        p.layout("cols", ENV.get("field.input.size", "50")),
                        enable("wrap", p.layout("wrap", true)));
                    input.setTextContent(beanValue.getValueText());
                }
                //some input assist - completion values?
                if (p.getItemList() != null) {
                    String dataListID = beanValue.getId() + "." + TAG_DATALIST;
                    appendAttributes(input, ATTR_LIST, dataListID);
                    Element datalist = appendElement(cell, TAG_DATALIST, ATTR_ID, dataListID);
                    createOptions(datalist, false, null, p.getItemList());
                }
                if (interactive) {
                    //create a finder button - on disabled items, show a view with details!
                    createFieldFinderButton(beanValue, p, cell, input);
                }
            }
        } else {
            input = createSelectorField(cell, beanValue);
        }
        if (!isData(beanValue)) {
            addAttributes(input, p, false);
        }

        if (beanValue.hasStatusError()) {
            row = appendTag(parent, TABLE(TAG_ROW));
//            appendElement(row, TAG_HEADERCELL, content(null));
            appendTag(row, TABLE(TAG_CELL, content(beanValue.getStatus().message()), ATTR_SPANCOL, "3", ATTR_SIZE, "-1",
                ATTR_STYLE, style(STYLE_FONT_COLOR, COLOR_RED)));
        }
        return input;
    }

    private void createFieldFinderButton(BeanValue<?> beanValue, IPresentable p, Element cell, Element input) {
        if (beanValue.isSelectable()) {
            String shortcut = shortCut(++tabIndex);
            Element a = createAction(cell,
                beanValue.getName() + POSTFIX_SELECTOR,
                CSS_CLASS_SELECTOR,
                ENV.translate("tsl2nano.finder.action.label", false),
                ENV.translate("tsl2nano.selection", true),
                null,
                shortcut,
                null,
                beanValue.hasWriteAccess(),
                false,
                true);
            appendAttributes(a, "tabindex", shortcut);
            addManyToOnePicture(cell, beanValue);
        }
        if (p.getEnabler().isActive()) {
            //on focus gained, preselect text
            if (ENV.get("websocket.autoselect", true)) {
                appendAttributes(input, "onfocus", "this.select();");
            }

            if (ENV.get("websocket.use", true)) {
                //perhaps create an input assist listener
                if (beanValue.getValueExpression() != null && ENV.get("websocket.use.inputassist", true)) {
                    appendAttributes(input, "onkeypress",
                        ENV.get("websocket.inputassist.function", "inputassist(event)"));
                }
                //perhaps create an dependency listener
                if (beanValue.hasListeners()) {
                    appendAttributes(input, "onblur",
                        ENV.get("websocket.dependency.function", "evaluatedependencies(event)"));
                    if (true/*(isData(beanValue)*/) {//provide mouseclicks on pictures
                        appendAttributes(input, "onclick",
                            ENV.get("websocket.dependency.function", "evaluatedependencies(event)"));
                    }
                }
                //handle attachments
                if (BitUtil.hasBit(beanValue.getPresentation().getType(), TYPE_ATTACHMENT)) {
                    appendAttributes(input, "onchange",
                        ENV.get("websocket.attachment.function", "transferattachment(this)"));
                    /*
                     * save the attachment to file system to be transferred by http-server,
                     * using bean-id and attribute name
                     */
                    Object v = beanValue.getValue();
   //                        if (beanValue instanceof Attachment) {
   //                            FileUtil.writeBytes(((Attachment)beanValue).getValue(), FileUtil.getValidFileName(beanValue.getName()), false);
   //                        } else {
                    if (!Util.isEmpty(v)) {
                        boolean writeFile = true;
                        if (v instanceof String)
                            if (new File((String) v).exists())
                                v =
                                    FileUtil.getFileBytes(Attachment.getFilename(beanValue.getInstance(),
                                        beanValue.getName(), (String) v), null);
                            else //not a file name and no data --> do nothing
                                writeFile = false;
   
                        if (writeFile && (ByteUtil.isByteStream(v.getClass())
                            || Serializable.class.isAssignableFrom(v.getClass())))
                            FileUtil.writeBytes(ByteUtil.getBytes(v),
                                beanValue.getValueFile().getPath(),
                                false);
                        else if (writeFile)
                            throw new IllegalStateException("attachment of attribute '"
                                + beanValue.getValueId()
                                + "' should be of type byte[], ByteBuffer, Blob or String - or at least Serializable!");
                    }
                }
            }
//                    }
        } else {//gray background on disabled
            appendAttributes(input, ATTR_STYLE,
                ENV.get("layout.field.disabled.style", STYLE_BACKGROUND_LIGHTGRAY));
        }
    }

    private void createFieldLabel(BeanValue<?> beanValue, IPresentable p, Element row) {
        if (p.getLabel() != null) {
            Element cellLabel = appendTag(row, TABLE(TAG_CELL, content(p.getLabel()
                + (beanValue.nullable() ? "" : isGeneratedValue(beanValue) ? " (!)" : " (*)")), ATTR_ID,
                beanValue.getId() + ".label",
                ATTR_CLASS,
                "beanfieldlabel",
                ATTR_WIDTH,
                ENV.get("layout.attribute.label.width", "250"), ATTR_STYLE, style(STYLE_FONT_COLOR,
                    (String) BeanUtil.valueOf(asString(p.getForeground()),
                        ENV.get("layout.attribute.label.color", "#0000cc"))),
                enableFlag(ATTR_HIDDEN, !p.isVisible()),
                enableFlag(ATTR_REQUIRED, !beanValue.nullable() && !isGeneratedValue(beanValue))));
            if (p.getIcon() != null) {
                String width = ENV.get("layout.icon.width", "-1");
                String height = ENV.get("layout.icon.height", "-1");
                appendElement(cellLabel, TAG_IMAGE, ATTR_SRC, p.getIcon(), ATTR_WIDTH, width, ATTR_HEIGHT, height);
            }
        } else {//create an empty label
            appendTag(row, TABLE(TAG_CELL));
        }
    }

    private String asString(int[] color) {
        if (Util.isEmpty(color))
            return null; 
        else {
            String c = Arrays.toString(color);
            return c.substring(1, c.length() - 1);
        }
    }

    /**
     * createDataTag
     * 
     * @param cell
     * @param beanValue
     * @return
     */
    private Element createDataTag(Element cell, BeanValue<?> beanValue) {
        Element data;
        String tagName = getDataTag(beanValue);
        File file = null;
        //embed the file content
        String content = null;
        if (!tagName.equals(TAG_FRAME)) {
            file = beanValue.getValueFile();
            if (tagName.equals(TAG_EMBED) || tagName.equals(TAG_SVG)) {
                if (file != null && getLayout(beanValue, "pluginspage") == null) {
                    content = new String(FileUtil.getFileBytes(file.getPath(), null));
                } else {
                    content = Util.asString(beanValue.getValue());
                }
            }
        }
        String width = ENV.get("layout.picture.width", "250");
        String height = ENV.get("layout.picture.height", "-1");
        data =
            appendElement(
                cell,
                tagName,
                content(content),
                ATTR_ID,
                beanValue.getId() + ".data",
                getDataAttribute(tagName),
                file != null ? FileUtil.getRelativePath(file,
                    ENV.getConfigPath()) : Util.asString(beanValue.getValue()),
                ATTR_CLASS,
                "beanfielddata",
                ATTR_WIDTH,
                width,
                ATTR_HEIGHT,
                height,
                ATTR_TITLE, //fallback to show an info text, if data couldn't be shown
                file != null
                    ? file
                        .getPath() /*"If no Plugin is available to show the content,\n klick on the downloaded item in your browser."*/
                    : "");
        if (tagName.equals(TAG_FRAME)) {
            String html = Util.asString(beanValue.getValue());
            if (html != null && !NetUtil.isURL(html))
                appendAttributes(data, ATTR_SRCDOC, html);
            appendAttributes(data, ATTR_WIDTH, "13em");
        }
        return data;
    }

    private String getLayout(BeanValue<?> beanValue, String name) {
        IPresentable p = beanValue.getPresentation();
        return p != null ? p.layout(name) : null;
    }

    /**
     * evaluates the html tag name through the presentable style.
     * 
     * @param beanValue
     * @return tag name
     */
    private String getDataTag(BeanValue<?> beanValue) {
        int baseBit = BitUtil.highestBitPosition(STYLE_DATA_IMG >> 1);
        int highBit = BitUtil.highestBitPosition(STYLE_DATA_FRAME >> 1);
        int style = BitUtil.retainBitRange(beanValue.getPresentation().getStyle(), baseBit, highBit);
        if (style == -1)
            style = evalDataStyle(beanValue.getValue());
        style = style >> baseBit;
        final String[] tags = { "img", "embed", "object", "canvas", "audio", "video", "device", "svg", "iframe" };
        return style == 0 ? tags[0] : BitUtil.description(style, Arrays.asList(tags));
    }

    private int evalDataStyle(Object value) {
        String c = Util.asString(value);
        if (ExpressionDescriptor.isURL(c))
            return IPresentable.STYLE_DATA_FRAME;
        else if (ExpressionDescriptor.isHtml(c))
            return IPresentable.STYLE_DATA_FRAME;
        else if (ExpressionDescriptor.isJSON(c))
            return IPresentable.STYLE_DATA_FRAME;
        else if (ExpressionDescriptor.isSVG(c))
            return IPresentable.STYLE_DATA_SVG;
        else if (ExpressionDescriptor.isAudio(c))
            return IPresentable.STYLE_DATA_SVG;
        return 0;
    }

    /**
     * evaluates the html source attribute name for the given tag-name. for the tag 'object' it is 'data' - on all other
     * tags it is 'src'.
     * 
     * @return tag name
     */
    private String getDataAttribute(String tagName) {
        return tagName.equals(TAG_OBJECT) ? ATTR_DATA : ATTR_SRC;
    }

    /**
     * createLayout
     * 
     * @param parent
     * @param presentable
     * @return
     */
    private Element createLayout(Element parent, IPresentable presentable) {
        Serializable l = presentable.getLayout();
        if (l instanceof Map) {
            parent = appendTag(parent, TABLE(TAG_TABLE, MapUtil.asStringArray((Map) l)));
        } else if (l instanceof String) {
            parent = appendTag(parent, TABLE(TAG_TABLE, ATTR_STYLE, (String) l));
        }
        return parent;
    }

    /**
     * createLayout
     * 
     * @param parent
     * @param presentable
     * @return
     */
    private Element createLayoutConstraints(Element parent, IPresentable p) {
        Object lc = null;
        try {
            lc = p.getLayoutConstraints();
        } catch (Exception ex) {
            LOG.error(ex);
            Message.send(ex);
        }
        if (lc instanceof Map) {
            appendAttributes(parent, MapUtil.asArray((Map<String, String>) lc));
        } else if (lc instanceof String) {
            appendAttributes(parent, ATTR_STYLE, lc);
        }
        return parent;
    }

    private String getValue(BeanValue<?> beanValue, String type) {
        IPresentable p = beanValue.getPresentation();
        //multi-line text will provide the text in the tags content - not the value (text may be to long)
        if (isMultiline(p)) {
            return "";
        }
        //let the browser decide, howto present a date - the date should be given as sql date (tested on chrome)
        return type.startsWith("date") && ENV.get("html5.present.type.date.tosqldate", true)
            ? StringUtil.toString(DateUtil.toSqlDateString((Date) beanValue.getValue()))
            : beanValue.getValueText();
    }

    private boolean isMultiline(IPresentable p) {
        return BitUtil.hasBit(p.getType(), TYPE_INPUT_MULTILINE)
            || BitUtil.hasBit(p.getStyle(), STYLE_MULTI);
    }

    private String getSuffix(RegExpFormat regexpFormat) {
        return regexpFormat != null && regexpFormat.getDefaultFormatter() != null ? ((GenericParser) regexpFormat
            .getDefaultFormatter()).getPostfix()
            : null;
    }

    private String getTextWithoutSuffix(String valueText, RegExpFormat regExpFormat) {
        String suffix = getSuffix(regExpFormat);
        if (suffix == null) {
            return valueText;
        }
        return StringUtil.substring(valueText, null, getSuffix(regExpFormat)).trim();
    }

    private String getTextAlignment(int style) {
        return NumberUtil.hasBit(style, STYLE_ALIGN_RIGHT) ? VAL_ALIGN_RIGHT : NumberUtil.hasBit(style,
            STYLE_ALIGN_CENTER) ? VAL_ALIGN_CENTER : VAL_ALIGN_LEFT;
    }

    private String getTextAlignmentAsStyle(int style) {
        return NumberUtil.hasBit(style, STYLE_ALIGN_RIGHT) ? style(STYLE_TEXT_ALIGN, VAL_ALIGN_RIGHT)
            : NumberUtil.hasBit(style, STYLE_ALIGN_CENTER) ? style(STYLE_TEXT_ALIGN, VAL_ALIGN_CENTER)
                : style(STYLE_TEXT_ALIGN, VAL_ALIGN_LEFT);
    }

    Element createSelectorField(Element cell, BeanValue<?> beanValue) {
        Element select =
            appendElement(cell,
                TAG_SELECT,
                ATTR_WIDTH,
                VAL_100PERCENT,
                ATTR_ID,
                beanValue.getId(),
                ATTR_NAME,
                beanValue.getName(),
                ATTR_PATTERN,
                (beanValue.getFormat() instanceof RegExpFormat ? ((RegExpFormat) beanValue.getFormat()).getPattern()
                    : null),
                ATTR_TITLE,
                beanValue.getPresentation().getDescription(),
                "tabindex",
                beanValue.getPresentation().layout(ATTR_TABINDEX, ++tabIndex).toString(),
                enableFlag(ATTR_HIDDEN, !beanValue.getPresentation().isVisible()),
                enableFlag(ATTR_READONLY, !beanValue.getPresentation().getEnabler().isActive()),
                enableFlag(ATTR_REQUIRED, !beanValue.nullable()));
        Collection<?> values = beanValue.getConstraint().getAllowedValues();
        Object selected = beanValue.getValue();
        boolean isEnum = beanValue.getType().isEnum();
        createOptions(select, isEnum, selected, values);
        return select;
    }

    /**
     * createOptions
     * 
     * @param parent
     * @param isEnum
     * @param selected
     * @param values
     */
    private void createOptions(Element parent, boolean isEnum, Object selected, Collection<?> values) {
        String content, id, description;
        for (Object v : values) {
            if (isEnum) {
                String translation = ENV.translate(v.toString(), false);
                content =
                    translation != null && !translation.startsWith(Messages.TOKEN_MSG_NOTFOUND) ? translation : v
                        .toString();
                id = content;
                description = ENV.translate(v.toString() + Messages.POSTFIX_TOOLTIP, true);
            } else {
                BeanDefinition bv = v instanceof String ? BeanDefinition.getBeanDefinition((String)v) : Bean.getBean(v);
                content = bv.toString();
                id = bv.getId().toString();
                description = bv.getPresentable().getDescription();
            }
            appendElement(parent, TAG_OPTION, content(content), ATTR_VALUE, id, "label", content,
                enable(ATTR_SELECTED, selected != null && v.equals(selected)));
        }
    }

    public String getType(BeanValue<?> beanValue) {
        return getType(beanValue.getType(), 
            beanValue.getPresentation().getType(), beanValue.getSecure() != null && !beanValue.getSecure().canDecrypt());
    }

    public static String getType(Class<?> objType, int presentationType, boolean secureCannotDecrypt) {
        String type;
        //let the specialized input types work
        presentationType = NumberUtil.filterBits(presentationType, TYPE_INPUT, TYPE_INPUT_MULTILINE);
        switch (presentationType) {
        case TYPE_INPUT_NUMBER:
            //floatings are not supported in html input type=number
            if (NumberUtil.isInteger(objType)) {
                type = "number";
                break;
            }
        case TYPE_SELECTION:
            type = "text";
            break;
        case TYPE_OPTION_RADIO:
            type = "radio";
            break;
        case TYPE_OPTION:
            type = "checkbox";
            break;
        case TYPE_DATE | TYPE_TIME:
            //most browsers are not able to present a datetime. see http://caniuse.com/#search=date
            //hibernate generates datetime annotations on dates
            type = ENV.get("html5.present.type.datetime", "date"/*"datetime-local"*/);
            break;
        case TYPE_TIME:
            type = "time";
            break;
        case TYPE_DATE:
            type = "date";
            break;
        case TYPE_INPUT_TEL:
            type = "tel";
            break;
        case TYPE_INPUT_EMAIL:
            type = "email";
            break;
        case TYPE_INPUT_URL:
            type = "url";
            break;
        case TYPE_INPUT_PASSWORD:
            type = "password";
            break;
        case TYPE_INPUT_SEARCH:
            type = "search";
            break;
        case TYPE_DATA:
            type = "img";
            break;
        case TYPE_ATTACHMENT:
        case TYPE_ATTACHMENT | TYPE_DATA:
            //on type = 'file' only the file-name is given (no path!)
            //will provide an upload button for the client system
            type = "file";
            break;
        default:
            type = "text";
            break;
        }
        //is it a text field and a hashed value?
        if (type.equals("text") && secureCannotDecrypt)
            type = "password";
        return type;
    }

    Element createFooter(Document doc, Object footer) {
        Element body = (Element) doc.getElementsByTagName(TAG_BODY).item(0);
        Element table = createGrid(body, "Status", "page.footer.table", 0);

        //fallback: setting style from environment-properties
        appendAttributes((Element) table.getParentNode(), ATTR_STYLE,
            ENV.get("layout.footer.grid.style", "background-image: url(icons/spe.jpg);"));

        Element preFooter;
        if (footer instanceof Throwable) {
            if (!ENV.get("app.login.secure", false)) {
                preFooter = doc.createElement(TAG_PRE);
                preFooter.setTextContent(((Throwable) footer).getMessage());
                Element details = doc.createElement(TAG_LINK);
                details.setAttribute(ATTR_HREF, "./" + new File(LogFactory.getLogFileName()).getName());
                details.setTextContent(ENV.translate("tsl2nano.exception", true));
                addRow(table, details);
            }
            preFooter = doc.createElement(TAG_PRE);
            preFooter.setTextContent(((Throwable) footer).getMessage());
        } else {
            String strFooter = Util.asString(footer);
            if (strFooter != null && !isHtml(strFooter)) {
                String[] split = strFooter.split("([:,=] )|[\t\n]");
                preFooter = doc.createElement(TAG_SPAN);
                preFooter.setAttribute(ATTR_ID, "footer");
                boolean isKey;
                String[] txt;//text + optional tooltip
                for (int i = 0; i < split.length; i++) {
                    isKey = i % 2 == 0;
                    if (isKey) {
                        appendElement(preFooter, TAG_IMAGE, ATTR_SRC, "icons/properties.png");
                    }
                    //evaluate the text and optional a title (tooltip)
                    txt = split[i].split("§");
                    Element e = appendElement(preFooter, isKey ? "b" : "i", content(txt[0] + "  "), ATTR_COLOR, isKey
                        ? COLOR_BLUE
                        : COLOR_BLACK);
                    if (txt.length > 1) {
                        txt[1] = StringUtil.fromHexString(txt[1]);
                        appendAttributes(e, ATTR_TITLE, txt[1]);
                        appendElement(e, TAG_PARAGRAPH, content(txt[1]), ATTR_CLASS, "tooltip");
                    }
                }
                appendElement(preFooter, TAG_IMAGE, ATTR_SRC, "icons/properties.png");
            } else {
                preFooter = doc.createElement(TAG_SPAN);
                preFooter.setNodeValue(strFooter);
            }
        }

        //append progress bar for websocket messsages
//            Element progress = doc.createElement("progress");
        appendElement(preFooter, "progress", ATTR_ID, "progressbar", "hidden", "true");
        appendElement(preFooter, TAG_SPAN, content(" \tWAITING FOR WEBSOCKET TO CONNECT...!"), ATTR_ID, MSG_FOOTER);
//            appendElement(progress, TAG_SPAN, content("0"), ATTR_STYLE, "position:relative");
//            appendAttributes(progress, "max", "100", "value", "0%", ATTR_STYLE, "position:relative");

        return addRow(table, preFooter);
    }

    @Override
    public boolean isDefaultAttribute(IAttribute attribute) {
        if (isBeanConfiguration()) {
            return true;
        }
        return super.isDefaultAttribute(attribute);
    }

    private boolean isBeanConfiguration() {
        return bean != null && Util.isFrameworkClass(bean.getDeclaringClass());
    }

    @Deprecated
    public void createSampleEnvironment() {
        try {
            String dir = System.getProperty("user.dir") + "/sample/";
            new File(dir).mkdirs();
            //the common-jar will be used by ant-scripts...
            ENV.extractResourceToDir(NanoH5.JAR_COMMON, dir);
            ENV.extractResourceToDir(NanoH5.JAR_INCUBATION, dir);
            FileUtil.extractNestedZip(NanoH5.JAR_SAMPLE, dir, null);
            FileUtil.writeBytes("call run.bat sample 8070".getBytes(), System.getProperty("user.dir")
                + "/sample.bat", false);
            //"Sample code and database created.\nPlease replace 'tsl2.nano.h5.jar/META-INF/MANIFEST.MF' with 'sample/META-INF/MANIFEST.MF' and start the new sample batch file...";
        } catch (Exception ex) {
            //while this only extracts some sample files, it should not break the application flow
            LOG.warn(ex);
        }
    }

    @Override
    public Html5Presentation createHelper(BeanDefinition def) {
        return new Html5Presentation<>(def);
    }

    @Override
    public Html5Presentable createPresentable() {
        return new Html5Presentable();
    }

    @Override
    public Html5Presentable createPresentable(AttributeDefinition<?> attr) {
        return new Html5Presentable(attr);
    }

    @Override
    public String decorate(String title, String message) {
        Element body = createGlasspane(createHeader(null, title, null, false));
        if (message != null) {
            //don't know, whether 'pluginspage' is an html5 attribute
            appendElement(body, TAG_EMBED, ATTR_SRC, message, ATTR_WIDTH, VAL_100PERCENT,
                ATTR_HEIGHT,
                String.valueOf(700), "pluginspage", "http://www.adobe.com/products/acrobat/readstep2.html");
        }
        createCloseAction(body);
        return HtmlUtil.toString(body.getOwnerDocument());
    }

    @Override
    public String page(String message) {
        return createMessagePage("message.template", message, null);
    }

    public void addRuleListener(String observer, String rule, int type, String... observables) {
        addRuleListener(bean.getAttribute(observer), rule, type, observables);
    }
    /**
     * convenience to add two rule listeners on changing an attribute (observable), to refresh another attribute
     * (observer). The first is {@link WebSocketRuleDependencyListener} on user interaction, the other is the standard
     * listener RuleDependencyListener on object changes.
     * 
     * @param observer refreshing attribute
     * @param rule rule to evaluate the observer refreshing
     * @param type 0: simple dependency listener, 1: websocket dependency listener, 2: both
     * @param observables changing attributes
     */
    public void addRuleListener(IAttributeDefinition<?> observer, String rule, int type, String... observables) {
        IListener<WSEvent> wsListener =
            new WebSocketRuleDependencyListener(observer, observer.getName(), rule);
        IListener<ChangeEvent> listener = new RuleDependencyListener(observer, observer.getName(), rule);
        for (int i = 0; i < observables.length; i++) {
            if (type % 2 == 0)
                bean.getAttribute(observables[i]).changeHandler().addListener(listener, ChangeEvent.class);
            if (type > 0)
                bean.getAttribute(observables[i]).changeHandler().addListener(wsListener, WSEvent.class);
        }
    }
}