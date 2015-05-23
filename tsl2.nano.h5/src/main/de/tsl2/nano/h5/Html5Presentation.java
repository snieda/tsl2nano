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
import static de.tsl2.nano.h5.HtmlUtil.ALIGN_CENTER;
import static de.tsl2.nano.h5.HtmlUtil.ALIGN_LEFT;
import static de.tsl2.nano.h5.HtmlUtil.ALIGN_RIGHT;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_ACCESSKEY;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_ACTION;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_ALIGN;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_ALT;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_AUTOFOCUS;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_BGCOLOR;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_BORDER;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_CHECKED;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_CLASS;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_COLOR;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_COLS;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_DATA;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_DISABLED;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_FORMNOVALIDATE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_FORMTARGET;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_FRAME;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_HEADERS;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_HIDDEN;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_HREF;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_ID;
import static de.tsl2.nano.h5.HtmlUtil.*;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_MAXLENGTH;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_METHOD;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_MIN;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_NAME;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_PATTERN;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_READONLY;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_REQUIRED;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_ROWS;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SELECTED;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SIZE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SPANCOL;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SRC;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_STYLE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_TABINDEX;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_TITLE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_TYPE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_TYPE_JS;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_TYPE_SEARCH;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_VALUE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_WIDTH;
import static de.tsl2.nano.h5.HtmlUtil.BTN_ASSIGN;
import static de.tsl2.nano.h5.HtmlUtil.CHAR_SUM;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_BLACK;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_BLUE;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_LIGHTER_BLUE;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_LIGHT_BLUE;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_LIGHT_GRAY;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_RED;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_WHITE;
import static de.tsl2.nano.h5.HtmlUtil.CSS_BACKGROUND_FADING_KEYFRAMES;
import static de.tsl2.nano.h5.HtmlUtil.STYLE_BACKGROUND_FADING_KEYFRAMES;
import static de.tsl2.nano.h5.HtmlUtil.STYLE_BACKGROUND_LIGHTGRAY;
import static de.tsl2.nano.h5.HtmlUtil.STYLE_BACKGROUND_RADIAL_GRADIENT;
import static de.tsl2.nano.h5.HtmlUtil.STYLE_FONT_COLOR;
import static de.tsl2.nano.h5.HtmlUtil.STYLE_TEXT_ALIGN;
import static de.tsl2.nano.h5.HtmlUtil.TAG_BODY;
import static de.tsl2.nano.h5.HtmlUtil.TAG_BUTTON;
import static de.tsl2.nano.h5.HtmlUtil.TAG_CELL;
import static de.tsl2.nano.h5.HtmlUtil.TAG_DIV;
import static de.tsl2.nano.h5.HtmlUtil.TAG_EMBED;
import static de.tsl2.nano.h5.HtmlUtil.TAG_FORM;
import static de.tsl2.nano.h5.HtmlUtil.TAG_H3;
import static de.tsl2.nano.h5.HtmlUtil.TAG_HEAD;
import static de.tsl2.nano.h5.HtmlUtil.TAG_HEADERCELL;
import static de.tsl2.nano.h5.HtmlUtil.TAG_HTML;
import static de.tsl2.nano.h5.HtmlUtil.TAG_IMAGE;
import static de.tsl2.nano.h5.HtmlUtil.TAG_INPUT;
import static de.tsl2.nano.h5.HtmlUtil.TAG_LINK;
import static de.tsl2.nano.h5.HtmlUtil.TAG_OBJECT;
import static de.tsl2.nano.h5.HtmlUtil.TAG_OPTION;
import static de.tsl2.nano.h5.HtmlUtil.TAG_PARAGRAPH;
import static de.tsl2.nano.h5.HtmlUtil.TAG_PRE;
import static de.tsl2.nano.h5.HtmlUtil.TAG_ROW;
import static de.tsl2.nano.h5.HtmlUtil.TAG_SCRIPT;
import static de.tsl2.nano.h5.HtmlUtil.TAG_SELECT;
import static de.tsl2.nano.h5.HtmlUtil.TAG_SPAN;
import static de.tsl2.nano.h5.HtmlUtil.TAG_TABLE;
import static de.tsl2.nano.h5.HtmlUtil.TAG_TBODY;
import static de.tsl2.nano.h5.HtmlUtil.TAG_TEXTAREA;
import static de.tsl2.nano.h5.HtmlUtil.VAL_100PERCENT;
import static de.tsl2.nano.h5.HtmlUtil.VAL_ALIGN_CENTER;
import static de.tsl2.nano.h5.HtmlUtil.VAL_ALIGN_LEFT;
import static de.tsl2.nano.h5.HtmlUtil.VAL_ALIGN_RIGHT;
import static de.tsl2.nano.h5.HtmlUtil.VAL_FRM_SELF;
import static de.tsl2.nano.h5.HtmlUtil.VAL_ROUNDCORNER;
import static de.tsl2.nano.h5.HtmlUtil.VAL_TRANSPARENT;
import static de.tsl2.nano.h5.HtmlUtil.appendElement;
import static de.tsl2.nano.h5.HtmlUtil.enable;
import static de.tsl2.nano.h5.HtmlUtil.enableFlag;
import static de.tsl2.nano.h5.HtmlUtil.style;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.IBeanCollector;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.IPresentableColumn;
import de.tsl2.nano.bean.def.IValueDefinition;
import de.tsl2.nano.bean.def.Presentable;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.bean.def.ValueExpressionFormat;
import de.tsl2.nano.bean.def.ValueGroup;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.core.AppLoader;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.BitUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.format.GenericParser;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.h5.configuration.BeanConfigurator;
import de.tsl2.nano.h5.expression.Query;
import de.tsl2.nano.h5.expression.QueryPool;
import de.tsl2.nano.script.ScriptTool;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.PrivateAccessor;

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

    Log LOG = LogFactory.getLog(Html5Presentation.class);

    protected static final StringBuilder EMPTY_CONTENT = new StringBuilder();

    public static final String L_GRIDWIDTH = "layout.gridwidth";

    public static final String PREFIX_BEANREQUEST = "~~~";
    /** indicator for server to handle a link, that was got as link (method=GET) not as a file */
    public static final String PREFIX_ACTION = PREFIX_BEANREQUEST + "!!!";
    /** indicator for server to handle a link, that was got as link (method=GET) not as a file */
    public static final String PREFIX_BEANLINK = PREFIX_BEANREQUEST + "--)";

    public static final String ID_QUICKSEARCH_FIELD = "field.quicksearch";

    static final String MSG_FOOTER = "progress";

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
            if (isRootBean()) {
                appActions.add(new SecureAction(bean.getClazz(),
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
                            beanTool.setAttributeFilter("sourceFile", "selectedAction", "text"/*, "result"*/);
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
                                    String name =
                                        tool.getSourceFile() != null ? tool.getSourceFile().toLowerCase() : FileUtil
                                            .getValidFileName(tool.getText().replace('.', '_'));
                                    //some file-systems may have problems on longer file names!
                                    name = StringUtil.cut(name, 64);
                                    Query query =
                                        new Query(name, tool.getText(), tool.getSelectedAction().getId()
                                            .equals("scripttool.sql.id"),
                                            null);
                                    ENV.get(QueryPool.class).add(query.getName(), query);
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
                        }
                        return beanTool;
                    }
                });

            }
        }
        return appActions;
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
                "icons/full_screen.png") {
                @Override
                public Object action() throws Exception {
                    return new Statistic<>(bean.getDeclaringClass());
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
        super.reset();
        //TODO: clear template cache
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
                    message instanceof String ? ENV.translate(message, true) : message,
                    interactive,
                    navigation);
            } else {
                form = createPage(session, null, "Leaving Application!<br/>Restart", false, navigation);
            }

            String html = HtmlUtil.toString(form.getOwnerDocument());
            if (LOG.isDebugEnabled()) {
                FileUtil.writeBytes(html.getBytes(), ENV.getConfigPath() + "html-server-response.html", false);
            }
            return html;
        } catch (Exception ex) {
            return HtmlUtil.createMessagePage(ManagedException.toRuntimeEx(ex, true, true).getMessage());
        }
    }

    Element createFormDocument(ISession session, String name, String image, boolean interactive) {
        Element body = createHeader(session, name, image, interactive);
        return appendElement(body,
            TAG_FORM,
            ATTR_ID,
            "page.form",
            ATTR_ACTION,
            "?",
            ATTR_METHOD,
            ENV.get("html5.http.method", "post"),
            enable("autocomplete", ENV.get("html5.form.autocomplete", true)), null);
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

            /*
             * create the header elements:
             * c1: left: App-Info with image
             * c2: center: Page-Title with image
             * c3: Page-Buttons
             */
            Element row = appendElement(createGrid(body, "page.header.table", "page.header.table", 3), TAG_ROW);
            Element c1 = appendElement(row, TAG_CELL);
            Element c2 = appendElement(row, TAG_CELL);
            String localDoc = ENV.getConfigPath() + "nano.h5.html";
            String docLink =
                new File(localDoc).canRead() ? localDoc : "https://sourceforge.net/p/tsl2nano/wiki/Home/";
            c1 = appendElement(c1, TAG_LINK, ATTR_HREF, docLink);
            appendElement(c1,
                TAG_IMAGE,
                content(ENV.getBuildInformations()),
                ATTR_SRC,
                "icons/beanex-logo-micro.jpg",
                ATTR_TITLE,
                "Framework Version and Documentation/Help");

            if (image != null) {
                c2 = appendElement(c2, TAG_H3, content(), ATTR_ALIGN, ALIGN_CENTER);
                appendElement(c2,
                    TAG_IMAGE,
                    content(title),
                    ATTR_SRC,
                    image);
            } else {
                String docURL;
                if (ENV.class.isAssignableFrom(bean.getClazz()))
                    docURL = new File("./").getAbsolutePath();
                else
                    docURL = ENV.getConfigPath() + "doc/" + StringUtil.toFirstLower(title) + "/index.html";
                if (new File(docURL).canRead()) {
                    c2 = appendElement(c2, TAG_H3, ATTR_ALIGN, ALIGN_CENTER);
                    appendElement(c2, TAG_LINK, content(title), ATTR_HREF, docURL);
                } else {
                    c2 = appendElement(c2, TAG_H3, content(title), ATTR_ALIGN, ALIGN_CENTER);
                }
            }
            Element c3 = appendElement(row, TAG_CELL, ATTR_ALIGN, ALIGN_RIGHT);
            if (interactive && bean != null) {
                if (useCSS) {
                    Element menu = createMenu(c3, "Menu");
                    createSubMenu(menu, ENV.translate("tsl2nano.application", true), "iconic home",
                        getApplicationActions(session));
                    createSubMenu(menu, ENV.translate("tsl2nano.session", true), "iconic map-pin",
                        getSessionActions(session));
                    createSubMenu(menu, ENV.translate("tsl2nano.page", true), "iconic magnifying-glass",
                        getPageActions(session));
                } else {
                    //fallback: setting style from environment-properties
                    HtmlUtil.appendAttributes((Element) c3.getParentNode(), ATTR_STYLE,
                        ENV.get("header.grid.style", "background-image: url(icons/spe.jpg)"));
                    c3 = appendElement(c3,
                        TAG_FORM,
                        ATTR_ACTION,
                        "?",
                        ATTR_METHOD,
                        ENV.get("html5.http.method", "post"));
                    c3 = createExpandable(c3, "Menu", true);
                    Collection<IAction> actions = new ArrayList<IAction>(getPageActions(session));
                    actions.addAll(getApplicationActions(session));
                    actions.addAll(getSessionActions(session));
                    createActionPanel(c3, actions,
                        ENV.get("html.show.header.button.text", true),
                        ATTR_ALIGN, ALIGN_RIGHT);
                }
            }
            return body;
        } catch (ParserConfigurationException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    private Element createMetaAndBody(ISession session, Element html, String title, boolean interactive) {
        appendElement(html, ATTR_STYLE, content(CSS_BACKGROUND_FADING_KEYFRAMES));
        HtmlUtil.appendAttributes(html, "manifest", ENV.get("html.manifest.file", "tsl2nano-appcache.mf"));
        Element head = appendElement(html, TAG_HEAD, ATTR_TITLE, "Nano-H5 Application: " + title);

        appendElement(head, "meta", "name", "author", "content", "tsl2.nano.h5 (by Thomas Schneider/2013)");
//        appendElement(head, "link", "rel", "stylesheet", "href", "css/style.css");

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
            appendElement(html, TAG_BODY, ATTR_ID, (!Util.isEmpty(title, true) ? title : TAG_BODY));
        if (interactive) {
            String style =
                ENV.get("application.page.style", STYLE_BACKGROUND_RADIAL_GRADIENT
                    + STYLE_BACKGROUND_FADING_KEYFRAMES);
            HtmlUtil.appendAttributes(body, /*"background", "icons/spe.jpg", */ATTR_STYLE,
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
        if (ENV.get("use.websocket", true)) {
            InputStream jsStream = ENV.getResource("websocket.client.js.template");
            String js = String.valueOf(FileUtil.getFileData(jsStream, "UTF-8"));

            Element script = appendElement(parent, TAG_SCRIPT, ATTR_TYPE, ATTR_TYPE_JS);

            Properties p = new Properties();
            p.putAll(ENV.getProperties());
            URL url = NanoH5.getServiceURL(null);
            p.put("websocket.server.ip", url.getHost());
            p.put("websocket.server.port", session.getWebsocketPort());
            p.put("websocket.element.id", elementId);
            script.appendChild(script.getOwnerDocument().createTextNode(
                StringUtil.insertProperties(js, p)));
        }
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
            availableshortCuts = new ArrayList(Arrays.asList(SHORTCUTS));
            tabIndex = -1;
            row1style = ENV.get("beancollector.grid.row1.style", "background-color: #CCCCFF;");
            row2style = ENV.get("beancollector.grid.row2.style", "background-color: #DDDDFF;");

            if (bean == null) {
                return createFormDocument(session, message.toString(), null, interactive);
            } else {
                parent =
                    createFormDocument(session, ENV.translate(bean.getName(), true), bean.getPresentable()
                        .getIcon(), interactive);
            }
        }

        //navigation bar
        if (interactive && navigation.length > 0) {
            Element link;
            Element nav = appendElement(parent, "nav", ATTR_ID, "navigation");
            for (BeanDefinition<?> bean : navigation) {
                link = appendElement(nav, TAG_LINK, ATTR_HREF, PREFIX_BEANLINK
                    + bean.getName(),
                    ATTR_STYLE, ENV.get("page.navigation.section.style", "color: #AAAAAA;"));
                appendElement(link, TAG_IMAGE, content(ENV.translate(bean.toString(), true)), ATTR_SRC,
                    "icons/forward.png");
            }
        }

        Element panel =
                appendElement(parent, TAG_DIV, ATTR_STYLE,
                    (interactive ? ENV.get("page.data.style", "overflow: auto; height: 700px;") : null));
        createContentPanel(session, panel, bean, interactive, ENV.get("page.data.fullwidth", false));

        if (isRoot) {
            if (interactive) {
                createBeanActions(parent, bean);
                createFooter(parent.getOwnerDocument(), message);
            }
        }
        return parent;
    }

    /**
     * createContentPanel
     * 
     * @param session
     * @param parent
     * @param bean
     * @param interactive
     */
    private Element createContentPanel(ISession session, Element panel, BeanDefinition bean, boolean interactive, boolean fullwidth) {
//        Element frame = appendElement(parent, "iframe", ATTR_SRC, "#data", ATTR_NAME, "dataframe");
//        panel = appendElement(parent, TAG_LINK, ATTR_NAME, "data", "target", "_blank");
        if (bean instanceof Controller) {
            panel = createController(session, panel, (Controller) bean, interactive, fullwidth);
        } else if (bean instanceof BeanCollector) {
            panel = createCollector(session, panel, (BeanCollector) bean, interactive, fullwidth);
        } else {
            //prefill a new bean with the current navigation stack objects
            if (bean.getId() == null) {
                addSessionValues(session);
            }
            panel = createBean(session, panel, (Bean<?>) bean, interactive, fullwidth);
        }
        return panel;
    }

    private Element createController(ISession session,
            Element parent,
            Controller controller,
            boolean interactive,
            boolean fullwidth) {
        Element table = appendElement(parent,
            TAG_TABLE,
            /*            ATTR_BORDER,
            border ? "1" : "0",*/
            ATTR_FRAME,
            "box",
            fullwidth ? ATTR_WIDTH : ATTR_ALIGN,
            fullwidth ? VAL_100PERCENT : ALIGN_CENTER,
            ATTR_BGCOLOR,
            COLOR_LIGHT_BLUE,
            enable("sortable", true));
//        if (Environment.get("html5.table.show.caption", false))
//            appendElement(table, "caption", content(title));
        //fallback: setting style from environment-properties
        HtmlUtil.appendAttributes(table, ATTR_STYLE,
            ENV.get("beancollector.grid.style", "background: transparent, border: 10"));

        if (controller.getPresentable() != null) {
            appendAttributes(table, controller.getPresentable(), true);
        }
        createActionTableContent(table, controller, controller.getBeanFinder().getData());
        return table;
    }

    @Override
    protected void addSessionValues(ISession session) {
        List<BeanDefinition> v = new ArrayList<>();
        //do the Object-casting trick to cast from List<Object> to List<BeanDefinition>
        Object navigation = Arrays.asList(session.getNavigationStack());
        v.addAll((List<BeanDefinition>) navigation);
        v.addAll((Collection<BeanDefinition>) session.getContext());
        addSessionValues(v);
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
    private Element createBean(ISession session, Element parent, Bean<?> bean, boolean interactive, boolean fullwidth) {
        Collection<ValueGroup> valueGroups = bean.getValueGroups();
        if (Util.isEmpty(valueGroups)) {
            return createFieldPanel(session, parent, bean.getPresentable(), bean.getBeanValues(), bean.getActions(),
                interactive, fullwidth);
        } else {//work on value groups
//            parent = appendElement(parent, TAG_DIV);
            Collection<IAction> noActions = new LinkedList<IAction>();
            for (ValueGroup valueGroup : valueGroups) {
                if (!valueGroup.isVisible()) {
                    continue;
                }
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

                    if (valueGroup.isDetail(name) && v != null) {
                        if (attr.isMultiValue()) {
                            bv =
                                BeanValue.getBeanValue(
                                    BeanCollector.createBeanCollectorHolder((Collection) bean.getValue(name),
                                        IBeanCollector.MODE_ALL), ValueHolder.ATTR_VALUE);
                            bv.setDescription(name);
                        } else {
                            bv =
                                BeanValue.getBeanValue(
                                    new ValueHolder(Bean.getBean((Serializable) bean.getValue(name))),
                                    ValueHolder.ATTR_VALUE);
                        }
                    } else {
                        bv = BeanValue.getBeanValue(bean.getInstance(), name);
                    }
                    beanValues.add(bv);
                }
                createFieldPanel(session, parent, valueGroup, beanValues, noActions, interactive, fullwidth);
            }
            return parent;
        }
    }

    private Element createFieldPanel(ISession session, Element parent,
            IPresentable p,
            Collection<BeanValue<?>> beanValues,
            Collection<IAction> actions,
            boolean interactive, boolean fullwidth) {
        int maxrows = ENV.get("layout.default.maxrowcount", 25);
        int maxcols =
            p.layout(L_GRIDWIDTH, ENV.get("layout.default.columncount", (AppLoader.isDalvik() ? 3 : 9)));

        int columns = (int) Math.ceil(beanValues.size() / (float) maxrows) * 3;
        columns = columns > maxcols ? maxcols : columns;

        parent = interactive ? createExpandable(parent, p.getDescription(), p.getEnabler().isActive()) : parent;
        Element panel =
            createGrid(parent, ENV.translate("tsl2nano.input", false), "field.panel", fullwidth, columns);
        //fallback: setting style from environment-properties
        if (isBeanConfiguration()) {
            HtmlUtil.appendAttributes((Element) panel.getParentNode(), "class", "fieldpanel", ATTR_STYLE,
                ENV.get("beanconfigurator.grid.style", "background-image: url(icons/art029.jpg)"));
        } else {
            HtmlUtil.appendAttributes((Element) panel.getParentNode(), "class", "fieldpanel", ATTR_STYLE,
                ENV.get("bean.grid.style", "background-image: url(icons/spe.jpg)"));
        }
        //set layout and constraints into the grid
        appendAttributes((Element) panel.getParentNode(), p, true);
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
                        : Bean.getBean((Serializable) beanValue.getValue());
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
        parent = appendElement(parent, "details", enable("open", open));
        String key = shortCut(++tabIndex);
        appendElement(parent, "summary", content(title), ATTR_ACCESSKEY, key, ATTR_TITLE, "ALT+" + key, ATTR_ALIGN,
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
        if (bean.getSelectionProvider().isEmpty()) {
            bean.selectFirstElement();
        }

        /*
         * append a quick search panel
         */
        if (interactive) {
            createQuickSearchPanel(parent, bean.getValueExpression().getExpression(), bean.getQuickSearchAction());
        }

        /*
         * create the column header
         */
        bean.addMode(MODE_MULTISELECTION);
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
        HtmlUtil.appendAttributes(grid, "class", "beancollector", ATTR_STYLE,
            ENV.get("beancollector.grid.style", "background: transparent, border: 10"));

        appendAttributes(grid, bean.getPresentable(), true);

        if (interactive && bean.hasMode(IBeanCollector.MODE_SEARCHABLE)) {
            Collection<T> data = new LinkedList<T>(bean.getSearchPanelBeans());
            //this looks complicated, but if currentdata is a collection with a FilteringIterator, we need a copy of the filtered items!
            data.addAll(CollectionUtil.getList(bean.getCurrentData().iterator()));
            createTableContent(session, grid, bean, data, interactive, 0, 1);
        } else {
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
            HtmlUtil.appendAttributes(input, "onkeypress",
                ENV.get("websocket.inputassist.function", "inputassist(event)"));
        }
        createAction(parent, action);
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

    private void appendAttributes(Element grid, IPresentable p, boolean isContainer) {
        appendAttributes(grid, null, p, isContainer);
    }

    private void appendAttributes(Element grid, String parentBGColor, IPresentable p, boolean isContainer) {
        HtmlUtil.appendAttributes(grid/*, ATTR_NAME, p.getLabel(), p.getDescription(), p.getType(), p.getEnabler(), p.getWidth(), p.getHeight()*/
            ,
            ATTR_BGCOLOR,
            convert(ATTR_BGCOLOR, p.getBackground(), COLOR_LIGHT_BLUE),
            enable(ATTR_STYLE, parentBGColor != null),
            enable(parentBGColor, parentBGColor != null),
            ATTR_COLOR,
            convert(ATTR_COLOR, p.getForeground(), COLOR_BLACK)/*, p.getIcon()*/);

        if (!isContainer) {
            HtmlUtil.appendAttributes(grid,
                ATTR_ALIGN,
                getTextAlignment(p.getStyle()));
        }
        if (p.getLayout() instanceof Map) {
            HtmlUtil.appendAttributes(grid, MapUtil.asArray((Map<String, Object>) p.getLayout()));
        }
        //TODO: only layout-constraints should be set
        if (p.getLayoutConstraints() instanceof Map) {
            HtmlUtil.appendAttributes(grid, MapUtil.asArray((Map<String, Object>) p.getLayoutConstraints()));
        }
    }

    /**
     * fill the given data to the grid
     * 
     * @param grid table grid to add rows to
     * @param columnDefinitions columns
     * @param data collection holding data
     * @param editableRowNumbers 0-based row numbers to be editable
     */
    void createTableContent(ISession session, Element grid,
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
        boolean hasSearchFilter = tableDescriptor.getBeanFinder().getFilterRange() != null;
        tabIndex = data.size() > editableRowNumbers.length ? editableRowNumbers.length * -1 : 0;
        for (T item : data) {
            if (hasSearchFilter && editableRows.contains(i++)) {
                addEditableRow(grid, tableDescriptor, item, i == 1 ? prop(KEY_FILTER_FROM_LABEL)
                    : i == 2 ? prop(KEY_FILTER_TO_LABEL) : toString(item, vef));
            } else {
                addRow(session, grid, tableDescriptor.hasMode(MODE_MULTISELECTION) && interactive, tableDescriptor,
                    item, interactive);
            }
        }
        Element footer = appendElement(grid, "tfoot", ATTR_BGCOLOR, COLOR_LIGHT_GRAY);
        Element footerRow = appendElement(footer, TAG_ROW);

        //summary
        List<IPresentableColumn> columns = tableDescriptor.getColumnDefinitionsIndexSorted();
        Element sum = appendElement(footerRow, TAG_CELL, content(""));
        ((Collection<BeanDefinition>) session.getContext()).add(tableDescriptor);
        Map contextParameter = new PrivateAccessor<>(session).call("getContextParameter", Map.class);
        boolean hasSummary = false;
        for (IPresentableColumn c : columns) {
            String text = tableDescriptor.getSummaryText(contextParameter, c.getIndex());
            appendElement(footerRow, TAG_CELL, content(text));
            if (text.length() > 0) {
                hasSummary = true;
            }
        }
        if (hasSummary) {
            appendElement(sum, TAG_PARAGRAPH, content(CHAR_SUM), ATTR_ALIGN, VAL_ALIGN_CENTER);
        }

        //search-count
        footerRow = appendElement(footer, TAG_ROW);
        appendElement(footerRow,
            TAG_CELL,
            content(tableDescriptor.getSummary()),
            "colspan",
            String.valueOf(tableDescriptor.getColumnDefinitions().size() + 1));
    }

    /**
     * fill the given data to the grid
     * 
     * @param grid table grid to add rows to
     * @param columnDefinitions columns
     * @param data collection holding data
     */
    void createActionTableContent(Element grid,
            Controller<?, T> tableDescriptor,
            Collection<T> data) {
        tabIndex = 0;
        for (T item : data) {
            addActionRow(grid, tableDescriptor, item);
        }
        Element footer = appendElement(grid, "tfoot");
        Element footerRow = appendElement(footer, TAG_ROW);
        appendElement(footerRow,
            TAG_CELL,
            content(tableDescriptor.getSummary()),
            "colspan",
            String.valueOf(tableDescriptor.getColumnDefinitions().size() + 1));
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
        Element nav = appendElement(div, "nav");
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
        /*
         * the main sub menu item will use the first action to link to...
         */
        Element list = appendElement(menu, "li");
        Element alink =
            appendElement(list, TAG_LINK, content(name), ATTR_HREF, PREFIX_ACTION + actions.iterator().next().getId());
        appendElement(alink, "span", "class", icon);
        Element sub = appendElement(list, "ul");
        if (actions != null) {
            for (IAction a : actions) {
                Element li = appendElement(sub, "li");
                appendElement(li, TAG_LINK, content(Messages.stripMnemonics(a.getShortDescription())),
                    ATTR_HREF,
                    PREFIX_ACTION + a.getId());
            }
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
        Element panel = createGrid(parent, "Actions", "action.panel", actions != null ? 1 + actions.size() : 1);
        Element row = appendElement(panel, TAG_ROW, ATTR_CLASS, "action.panel");
        Element cell = appendElement(row, TAG_CELL, attributes);
        if (actions != null) {
            for (IAction a : actions) {
                createAction(cell, a, showText);
            }
        }
        return cell;
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
        return createAction(cell,
            a.getId(),
            showText ? a.getShortDescription() : null,
            a.getLongDescription(),
            "submit",
            a.getKeyStroke(),
            (a.getImagePath() != null ? a.getImagePath() : "icons/" + StringUtil.substring(a.getId(), ".", null, true)
                + ".png"),
            a.isEnabled(),
            a.isDefault(),
            a.getActionMode() != IAction.MODE_DLG_OK);
    }

    /**
     * creates html buttons
     * 
     * @param form
     * @param model
     * @return html table containing the buttons
     */
    Element createBeanActions(Element form, BeanDefinition<?> model) {
        Element panel = createActionPanel(form, model.getActions(), true, ATTR_ALIGN, ALIGN_CENTER);
        if (model.isMultiValue() && ((BeanCollector) model).hasMode(MODE_ASSIGNABLE)) {
            String assignLabel = Messages.getStringOpt("tsl2nano.assign", true);
            createAction(panel, BTN_ASSIGN, assignLabel, assignLabel, "submit", null, "icons/links.png", true, true,
                false);
        }
        createCloseAction(panel);
        return panel;
    }

    void createCloseAction(Element panel) {
        String closeLabel = Messages.getStringOpt("tsl2nano.close", true);
        createAction(panel, IAction.CANCELED, closeLabel, closeLabel, null, null, "icons/stop.png", true, false, true);
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
        return createAction(cell, id, label, label, type, null, image, true, false, false);
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
            String label,
            String tooltip,
            String type,
            Object shortcut,
            String image,
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
        Element action = appendElement(cell,
            TAG_BUTTON,
            content(label),
            ATTR_ID,
            id,
            ATTR_NAME,
            id,
            ATTR_TITLE,
            tooltip,
            enable(ATTR_TYPE, type != null),
            type,
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
        Element table = appendElement(parent,
            TAG_TABLE,
            ATTR_BORDER,
            border ? "10" : "0",
            ATTR_ID,
            id,
            ATTR_FRAME,
            "box",
            fullwidth ? ATTR_WIDTH : ATTR_ALIGN,
            fullwidth ? VAL_100PERCENT : ALIGN_CENTER,
            /*ATTR^_BGCOLOR,
            COLOR_LIGHT_BLUE,
            ATTR_STYLE,
            VAL_OPACITY_0_5,*/
            enable("sortable", true));
        if (ENV.get("html5.table.show.caption", false)) {
            appendElement(table, "caption", content(title));
        }
        Element head = appendElement(table, "thead");
        Element colgroup = appendElement(head, TAG_ROW);
        for (int i = 0; i < columns.length; i++) {
            appendElement(colgroup, TAG_HEADERCELL, content(columns[i]), ATTR_BGCOLOR, COLOR_LIGHT_GRAY);
        }
        return appendElement(table, TAG_TBODY);
    }

    Element createGrid(Element parent, String title, boolean border, BeanCollector<?, ?> collector, boolean fullwidth) {
        Element table = appendElement(parent,
            TAG_TABLE,
            ATTR_BORDER,
            border ? "10" : "0",
            ATTR_FRAME,
            "box", /*"contenteditable", "true",*/
            fullwidth ? ATTR_WIDTH : ATTR_ALIGN,
            fullwidth ? VAL_100PERCENT : ALIGN_CENTER,
            /*ATTR_BGCOLOR,
            COLOR_LIGHT_BLUE,*/
            ATTR_STYLE,
            VAL_TRANSPARENT + VAL_ROUNDCORNER,
            enable("sortable", true));
        if (ENV.get("html5.table.show.caption", false)) {
            appendElement(table, "caption", content(title));
        }
        Element head = appendElement(table, "thead");
        if (collector.getPresentable() != null) {
            appendAttributes(table, collector.getPresentable(), true);
        }
        Element colgroup = appendElement(head, TAG_ROW, ATTR_BORDER, "1");
        if (collector.hasMode(MODE_MULTISELECTION)) {
            appendElement(colgroup, TAG_HEADERCELL, content());
        }
        Collection<IPresentableColumn> columns = collector.getColumnDefinitionsIndexSorted();
        for (IPresentableColumn c : columns) {
            Element th = appendElement(colgroup,
                TAG_HEADERCELL,
                ATTR_ID,
                c.getIndex() + ":" + c.getName(),
                ATTR_BORDER,
                "1",
                ATTR_WIDTH,
                Presentable.asText(c.getWidth()),
                //                "style",
//                "-webkit-transform: scale(1.2);",
                ATTR_BGCOLOR,
                COLOR_LIGHT_GRAY);
            if (c.getPresentable() != null) {
                appendAttributes(th, c.getPresentable(), true);
            }
            createAction(th, c.getSortingAction(collector));
        }
        return appendElement(table, TAG_TBODY);
    }

    protected Element addRow(ISession session,
            Element grid,
            boolean multiSelection,
            BeanCollector<?, T> tableDescriptor,
            T item,
            boolean interactive) {
        boolean isSelected = tableDescriptor.getSelectionProvider() != null ? tableDescriptor.getSelectionProvider()
            .getValue()
            .contains(item) : false;

        Bean<Serializable> itemBean = Bean.getBean((Serializable) item);
        String beanStyle = itemBean.getPresentable().layout(ATTR_STYLE);
        String rowBackground;
        if (beanStyle != null) {
            rowBackground = beanStyle;
        } else {
            rowBackground = grid.getChildNodes().getLength() % 2 == 0 ? row1style : row2style;
        }

        String tab = String.valueOf(++tabIndex);
        String shortCut = shortCut(tabIndex);
        Element row =
            appendElement(grid,
                TAG_ROW,
                ATTR_ID,
                Util.asString(itemBean.getId()),
                ATTR_CLASS,
                "beancollector.row",
                "onclick",
                tableDescriptor.hasMode(IBeanCollector.MODE_EDITABLE)
                    ? "this.getElementsByTagName('input')[0].onclick()" : null,
                "ondblclick",
                "location=this.getElementsByTagName('a')[0]",
                ATTR_TABINDEX,
                tab,
                ATTR_ACCESSKEY,
                shortCut,
                ATTR_TITLE,
                "ALT+" + shortCut,
                ATTR_FORMTARGET,
                VAL_FRM_SELF,
                ATTR_STYLE, row1style
            /*ATTR_BGCOLOR, rowBGColor*/);

        //first cell: bean reference as link
        String length = String.valueOf(grid.getChildNodes().getLength() - 1);
        Element firstCell = appendElement(row, TAG_CELL);
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

        String icon =
            (icon = itemBean.getPresentable().getIcon()) != null ? icon : item instanceof BeanDefinition
                && (icon = ((BeanDefinition<?>) item).getPresentable().getIcon()) != null ? icon : null;
        if (icon != null) {
            appendElement(firstCell, TAG_IMAGE, ATTR_SRC, icon);
        }
        Collection<IPresentableColumn> colDefs = tableDescriptor.getColumnDefinitionsIndexSorted();
        Element cell;
        String value;
        if (colDefs.size() > 0) {
            for (IPresentableColumn c : colDefs) {
                //on byte[] show an image through attached file
                if (BitUtil.hasBit(itemBean.getAttribute(c.getName()).getPresentation().getType(), TYPE_DATA,
                    TYPE_ATTACHMENT)) {
                    BeanValue<?> beanValue = (BeanValue<?>) itemBean.getAttribute(c.getName());
                    value = beanValue.getValueFile().getPath();
                    cell =
                        appendElement(row, TAG_CELL, ATTR_TITLE,
                            itemBean.toString() + ": " + ENV.translate(c.getName(), true),
                            ATTR_HEADERS, c.getIndex() + ":" + c.getName(), ATTR_ID,
                            tableDescriptor.getId() + "[" + tabIndex
                                + ", " + c.getIndex() + "]");
                    Element data = createDataTag(cell, beanValue);
                    if (c.getPresentable() != null) {
                        appendAttributes(data, rowBackground, c.getPresentable(), false);
                        appendAttributes(cell, rowBackground, c.getPresentable(), false);
                    }
                    if (Messages.isMarkedAsProblem(value)) {
                        HtmlUtil.appendAttributes(cell, ATTR_COLOR, COLOR_RED);
                    }
                } else if (tableDescriptor.hasMode(IBeanCollector.MODE_SHOW_NESTINGDETAILS)
                    && !BeanUtil.isStandardType(itemBean.getAttribute(c.getName()).getType())) {//nesting panels
                    cell =
                        appendElement(row, TAG_CELL, ATTR_TITLE,
                            itemBean.toString() + ": " + ENV.translate(c.getName(), true),
                            ATTR_HEADERS, c.getIndex() + ":" + c.getName(), ATTR_ID,
                            tableDescriptor.getId() + "[" + tabIndex
                                + ", " + c.getIndex() + "]");
//                    cell = appendElement(cell, TAG_EMBED);
                    createContentPanel(session, cell,
                        Bean.getBean((Serializable) itemBean.getAttribute(c.getName()).getValue()), interactive, false);
                } else {//standard --> text
                    value = tableDescriptor.getColumnText(item, c.getIndex());
                    cell =
                        appendElement(row, TAG_CELL, content(value), ATTR_TITLE,
                            itemBean.toString() + ": " + ENV.translate(c.getName(), true),
                            ATTR_HEADERS, c.getIndex() + ":" + c.getName(), ATTR_ID,
                            tableDescriptor.getId() + "[" + tabIndex
                                + ", " + c.getIndex() + "]");
                    if (c.getPresentable() != null) {
                        appendAttributes(cell, rowBackground, c.getPresentable(), false);
                    }
                    if (Messages.isMarkedAsProblem(value)) {
                        HtmlUtil.appendAttributes(cell, ATTR_COLOR, COLOR_RED);
                    }
                }
            }
        } else {//don't show only an empty entry!
            value = StringUtil.toString(item, Integer.MAX_VALUE);
            cell =
                appendElement(row, TAG_CELL, content(value), ATTR_TITLE,
                    itemBean.hashCode() + ": " + ENV.translate("toString", true),
                    ATTR_HEADERS, "0: toString", ATTR_ID,
                    tableDescriptor.getId() + "[" + tabIndex
                        + ", 0" + "]");
            if (Messages.isMarkedAsProblem(value)) {
                HtmlUtil.appendAttributes(cell, ATTR_COLOR, COLOR_RED);
            }
        }
        return row;
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
        if (!availableshortCuts.isEmpty() && !availableshortCuts.remove(c)) {
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
        Element row = appendElement(table, TAG_ROW, ATTR_BGCOLOR, COLOR_LIGHT_GRAY, ATTR_ALIGN, ALIGN_CENTER);
        if (rowName != null) {
            Element r = appendElement(row, TAG_CELL, ATTR_CLASS, "beancollector.search.row");
            appendElement(r, TAG_PARAGRAPH, content(rowName), ATTR_ALIGN, VAL_ALIGN_CENTER);
        }

        boolean focusSet = false;
        Collection<IPresentableColumn> colDefs = tableDescriptor.getColumnDefinitionsIndexSorted();
        for (IPresentableColumn c : colDefs) {
            String value = tableDescriptor.getColumnText(element, c.getIndex());
            Element cell0 = appendElement(row, TAG_CELL);
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
        Element row = appendElement(table, TAG_ROW);
        Element rowColumn = appendElement(row, TAG_CELL);
        for (int i = 0; i < cells.length; i++) {
            rowColumn.appendChild(cells[i]);
        }
        return row;
    }

    protected Element addActionRow(Element grid, Controller<?, T> controller, T item) {
        Bean<T> itemBean = controller.getBean(item);
        Element row =
            appendElement(grid,
                TAG_ROW,
                ATTR_ID,
                itemBean.getId().toString(),
                ATTR_TABINDEX,
                ++tabIndex + "",
                ATTR_FORMTARGET,
                VAL_FRM_SELF,
                ATTR_BGCOLOR, itemBean.getPresentable().layout(ATTR_BGCOLOR));

        //first cell: bean reference
        Element cell = appendElement(row, TAG_CELL, content(itemBean.toString()));

        Collection<IAction> actions = itemBean.getActions();
        for (IAction a : actions) {
            cell = appendElement(row, TAG_CELL);
            Element btn = createAction(cell, a);
            btn.setAttribute(ATTR_NAME, Controller.PREFIX_CTRLACTION + tabIndex + controller.POSTFIX_CTRLACTION
                + a.getId());
        }
        return row;
    }

    Element createField(Element parent, BeanValue<?> beanValue, boolean interactive) {
        IPresentable p = beanValue.getPresentation();
        boolean multiLineText = isMultiline(p);
        Element row =
            parent.getNodeName().equals(TAG_ROW) ? parent : appendElement(parent, TAG_ROW/*, ATTR_STYLE, VAL_OPAC*/);
        //first the label
        if (p.getLabel() != null) {
            Element cellLabel = appendElement(row, TAG_CELL, content(p.getLabel()
                + (beanValue.nullable() ? "" : isGeneratedValue(beanValue) ? " (!)" : " (*)")), ATTR_ID,
                beanValue.getId() + ".label",
                ATTR_CLASS,
                "bean.field.label",
                ATTR_WIDTH,
                ENV.get("default.attribute.label.width", "250"), ATTR_STYLE, style(STYLE_FONT_COLOR,
                    (String) BeanUtil.valueOf(p.getForeground(),
                        ENV.get("default.attribute.label.color", "#0000cc"))),
                enableFlag(ATTR_HIDDEN, !p.isVisible()),
                enableFlag(ATTR_REQUIRED, !beanValue.nullable() && !isGeneratedValue(beanValue)));
            if (p.getIcon() != null) {
                appendElement(cellLabel, TAG_IMAGE, ATTR_SRC, p.getIcon());
            }
        } else {//create an empty label
            appendElement(row, TAG_CELL);
        }
        //create the layout and layout-constraints
        Element cell = appendElement(row, TAG_CELL);
        cell = createLayoutConstraints(cell, p);
        cell = createLayout(cell, p);
        //now the field itself
        Element input = null;
        if (isData(beanValue)) {
            input = createDataTag(cell, beanValue);
            appendAttributes(input, p, false);
        }
        if (beanValue.getConstraint().getAllowedValues() == null) {
            if (input == null || BitUtil.hasBit(p.getType(), TYPE_ATTACHMENT)) {
                RegExpFormat regexpFormat =
                    beanValue.getFormat() instanceof RegExpFormat ? (RegExpFormat) beanValue.getFormat()
                        : null;
                String type = getType(beanValue);
                String width = Presentable.asText(p.getWidth());
                if (width == null) {
                    width = "50";
                }
                boolean isOption = "checkbox".equals(type);
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
                        "bean.field.input",
                        ATTR_NAME,
                        beanValue.getName(),
                        ATTR_PATTERN,
                        regexpFormat != null ? regexpFormat.getPattern() : ENV.get("default.pattern.regexp", ".*"),
                        ATTR_STYLE,
                        getTextAlignmentAsStyle(p.getStyle()),
                        ATTR_SIZE,/* 'width' doesn't work, so we set the displaying char-size */
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
                        (isOption ? enable(ATTR_CHECKED, (Boolean) beanValue.getValue()) : ATTR_VALUE),
                        (isOption ? ((Boolean) beanValue.getValue() ? "checked" : null) : getValue(beanValue, type)),
                        ATTR_TITLE,
                        p.getDescription()
                            + (LOG.isDebugEnabled() ? "\n\n" + "<![CDATA[" + beanValue.toDebugString() + "]]>" : ""),
                        ATTR_TABINDEX,
                        p.layout("tabindex", ++tabIndex).toString(),
                        enableFlag(ATTR_HIDDEN, !p.isVisible()),
                        enableFlag(ATTR_DISABLED, !interactive || !p.getEnabler().isActive()),
                        enableFlag(ATTR_READONLY, !interactive || !p.getEnabler().isActive() || beanValue.composition()),
                        enableFlag(ATTR_REQUIRED, !beanValue.nullable() && !beanValue.generatedValue()));

                if (multiLineText) {
                    HtmlUtil.appendAttributes(input, ATTR_ROWS, p.layout("rows", "5"), ATTR_COLS,
                        p.layout("cols", "50"),
                        enable("wrap", p.layout("wrap", true)));
                    input.setTextContent(beanValue.getValueText());
                }
                //some input assist - completion values?
                if (p.getItemList() != null) {
                    String dataListID = beanValue.getId() + "." + TAG_DATALIST;
                    HtmlUtil.appendAttributes(input, ATTR_LIST, dataListID);
                    Element datalist = appendElement(cell, TAG_DATALIST, ATTR_ID, dataListID);
                    createOptions(datalist, false, null, p.getItemList());
                }
                if (interactive) {
                    //create a finder button - on disabled items, show a view with details!
                    if (beanValue.isSelectable()) {
                        String shortcut = shortCut(++tabIndex);
                        Element a = createAction(cell,
                            beanValue.getName() + POSTFIX_SELECTOR,
                            ENV.translate("tsl2nano.finder.action.label", false),
                            ENV.translate("tsl2nano.selection", true),
                            null,
                            shortcut,
                            null,
                            beanValue.hasWriteAccess(),
                            false,
                            true);
                        HtmlUtil.appendAttributes(a, "tabindex", shortcut);

                    }
                    if (p.getEnabler().isActive()) {
                        //perhaps create an input assist listener
                        if (beanValue.getValueExpression() != null && ENV.get("websocket.use.inputassist", true)) {
                            HtmlUtil.appendAttributes(input, "onkeypress",
                                ENV.get("websocket.inputassist.function", "inputassist(event)"));
                        }
                        //on focus gained, preselect text
                        if (ENV.get("websocket.autoselect", true)) {
                            HtmlUtil.appendAttributes(input, "onfocus", "this.select();");
                        }

                        //perhaps create an dependency listener
                        if (beanValue.hasListeners()) {
                            HtmlUtil.appendAttributes(input, "onblur",
                                ENV.get("websocket.dependency.function", "evaluatedependencies(event)"));
                            if (true/*(isData(beanValue)*/) {//provide mouseclicks on pictures
                                HtmlUtil.appendAttributes(input, "onclick",
                                    ENV.get("websocket.dependency.function", "evaluatedependencies(event)"));
                            }
                        }
                        //handle attachments
                        if (BitUtil.hasBit(beanValue.getPresentation().getType(), TYPE_ATTACHMENT)) {
                            HtmlUtil.appendAttributes(input, "onchange",
                                ENV.get("websocket.attachment.function", "transferattachment(this)"));
                            /*
                             * save the attachment to file system to be transferred by http-server,
                             * using bean-id and attribute name
                             */
                            Object v = beanValue.getValue();
//                        if (beanValue instanceof Attachment) {
//                            FileUtil.writeBytes(((Attachment)beanValue).getValue(), FileUtil.getValidFileName(beanValue.getName()), false);
//                        } else {
                            if (v != null) {
                                byte[] bytes;
                                if (v instanceof byte[]) {
                                    bytes = (byte[]) v;
                                } else if (v instanceof ByteBuffer) {
                                    bytes = ((ByteBuffer) v).array();
                                } else if (v instanceof String) {
                                    bytes = ((String) v).getBytes();
                                } else {
                                    throw new IllegalStateException("attachment of attribute '"
                                        + beanValue.getValueId()
                                        + "' has to be of type byte[], ByteBuffer or String!");
                                }
                                FileUtil.writeBytes(bytes, ENV.getTempPath() + beanValue.getValueId(), false);
                            }
                        }
//                    }
                    } else {//gray background on disabled
                        HtmlUtil.appendAttributes(input, ATTR_STYLE, STYLE_BACKGROUND_LIGHTGRAY);
                    }
                }
            }
        } else {
            input = createSelectorField(cell, beanValue);
        }
        if (!isData(beanValue)) {
            appendAttributes(input, p, false);
        }

        if (beanValue.hasStatusError()) {
            row = appendElement(parent, TAG_ROW);
//            appendElement(row, TAG_HEADERCELL, content(null));
            appendElement(row, TAG_CELL, content(beanValue.getStatus().message()), ATTR_SPANCOL, "3", ATTR_SIZE, "-1",
                ATTR_STYLE, style(STYLE_FONT_COLOR, COLOR_RED));
        }
        return input;
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
        File file = beanValue.getValueFile();
        //embed the file content
        String content = null;
        if (tagName.equals(TAG_EMBED) && file != null && getLayout(beanValue, "pluginspage") == null) {
            content = new String(FileUtil.getFileBytes(file.getPath(), null));
        }
        data =
            appendElement(
                cell,
                tagName,
                content(content),
                ATTR_ID,
                beanValue.getId() + ".data",
                getDataAttribute(tagName),
                file != null ? FileUtil.getRelativePath(file,
                    ENV.getConfigPath()) : "",
                ATTR_CLASS,
                "bean.field.data",
                ATTR_TITLE,//fallback to show an info text, if data couldn't be shown
                "If no Plugin is available to show the content,\n klick on the downloaded item in your browser."
            );

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
        style = style >> baseBit;
        final String[] tags = { "img", "embed", "object", "canvas", "audio", "video", "device", "iframe" };
        return style == 0 ? tags[0] : BitUtil.description(style, Arrays.asList(tags));
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
        if (presentable.getLayout() instanceof Map) {
            parent = appendElement(parent, TAG_TABLE, MapUtil.asStringArray((Map) presentable.getLayout()));
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
        if (p.getLayoutConstraints() instanceof Map) {
            HtmlUtil.appendAttributes(parent, MapUtil.asArray((Map<String, String>) p.getLayoutConstraints()));
        }
        return parent;
    }

    private String getValue(BeanValue<?> beanValue, String type) {
        IPresentable p = beanValue.getPresentation();
        //multi-line text will provide the text in the tags content - not the value (text may be to long)
        if (isMultiline(p)) {
            return "";
        }
        return type.startsWith("date") ? StringUtil.toString(DateUtil.toSqlDateString((Date) beanValue.getValue()))
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
                Bean<Serializable> bv = Bean.getBean((Serializable) v);
                content = bv.toString();
                id = bv.getId().toString();
                description = bv.getPresentable().getDescription();
            }
            appendElement(parent, TAG_OPTION, content(content), ATTR_ID, id, ATTR_TITLE, description,
                enable(ATTR_SELECTED, selected != null && v.equals(selected)));
        }
    }

    private String getType(BeanValue<?> beanValue) {
        String type;
        int t = beanValue.getPresentation().getType();
        //let the specialized input types work
        t = NumberUtil.filterBits(t, TYPE_INPUT, TYPE_INPUT_MULTILINE);
        switch (t) {
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
            type = "datetime";
            break;
        case TYPE_TIME:
            type = "time";
            break;
        case TYPE_DATE:
            type = "date";
            break;
        case TYPE_INPUT_NUMBER:
            //floatings are not supported in html input type=number
            if (NumberUtil.isInteger(beanValue.getType())) {
                type = "number";
                break;
            }
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
        case TYPE_ATTACHMENT:
            //on type = 'file' only the file-name is given (no path!)
            //will provide an upload button for the client system
            type = "file";
            break;
        default:
            type = "text";
            break;
        }
        //is it a text field and a hashed value?
        if (type.equals("text") && beanValue.getSecure() != null && !beanValue.getSecure().canDecrypt())
            type = "password";
        return type;
    }

    Element createFooter(Document doc, Object footer) {
        Element body = (Element) doc.getElementsByTagName(TAG_BODY).item(0);
        Element table = createGrid(body, "Status", "page.footer.table", 1);

        //fallback: setting style from environment-properties
        HtmlUtil.appendAttributes((Element) table.getParentNode(), ATTR_STYLE,
            ENV.get("footer.grid.style", "background-image: url(icons/spe.jpg)"));

        Element preFooter;
        if (footer instanceof Throwable) {
            Element details = doc.createElement(TAG_LINK);
            details.setAttribute(ATTR_HREF, new File(LogFactory.getLogFileName()).getAbsolutePath());
            details.setTextContent(ENV.translate("tsl2nano.exception", true));
            addRow(table, details);
            preFooter = doc.createElement(TAG_PRE);
            preFooter.setTextContent(((Throwable) footer).getMessage());
        } else {
            String strFooter = Util.asString(footer);
            if (strFooter != null && !HtmlUtil.isHtml(strFooter)) {
                String[] split = strFooter.split("([:,=] )|[\t\n]");
                preFooter = doc.createElement(TAG_SPAN);
                preFooter.setAttribute(ATTR_ID, "footer");
                boolean isKey;
                for (int i = 0; i < split.length; i++) {
                    isKey = i % 2 == 0;
                    if (isKey) {
                        appendElement(preFooter, TAG_IMAGE, ATTR_SRC, "icons/properties.png");
                    }
                    appendElement(preFooter, isKey ? "b" : "i", content(split[i] + "  "), ATTR_COLOR, isKey
                        ? COLOR_BLUE
                        : COLOR_BLACK);
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
        appendElement(preFooter, TAG_SPAN, content(" \tinitializing..."), ATTR_ID, MSG_FOOTER);
//            appendElement(progress, TAG_SPAN, content("0"), ATTR_STYLE, "position:relative");
//            HtmlUtil.appendAttributes(progress, "max", "100", "value", "0%", ATTR_STYLE, "position:relative");

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

    protected static final StringBuilder content() {
        return EMPTY_CONTENT;
    }

    protected static final StringBuilder content(String str) {
        return str != null ? new StringBuilder(str) : EMPTY_CONTENT;
    }

    protected static String convert(String name, Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        } else if (name.equals(ATTR_COLOR) || name.equals(ATTR_BGCOLOR)) {
            int[] c = (int[]) value;
            StringBuilder s = new StringBuilder(6);
            for (int i = 0; i < c.length; i++) {
                s.append(StringUtil.toHexString(c.toString().getBytes()));
            }
            return s.toString();
        } else {
            return value.toString();
        }
    }

    @Override
    public String decorate(String title, String message) {
        Element body = createHeader(null, title, null, false);
        if (message != null) {
            //don't know, whether 'pluginspage' is an html5 attribute
            appendElement(body, HtmlUtil.TAG_EMBED, ATTR_SRC, message, ATTR_WIDTH, VAL_100PERCENT,
                HtmlUtil.ATTR_HEIGHT,
                String.valueOf(700), "pluginspage", "http://www.adobe.com/products/acrobat/readstep2.html");
        }
        createCloseAction(body);
        return HtmlUtil.toString(body.getOwnerDocument());
    }

    @Override
    public String page(String message) {
        return createMessagePage("message.template", message, null);
    }

}