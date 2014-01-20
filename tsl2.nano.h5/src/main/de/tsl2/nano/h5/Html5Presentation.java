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
import static de.tsl2.nano.h5.HtmlUtil.ALIGN_CENTER;
import static de.tsl2.nano.h5.HtmlUtil.ALIGN_RIGHT;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_ACCESSKEY;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_ACTION;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_ALIGN;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_ALT;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_AUTOFOCUS;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_BGCOLOR;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_BORDER;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_CHECKED;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_COLOR;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_DISABLED;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_FORMNOVALIDATE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_FRAME;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_HEIGHT;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_HIDDEN;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_HREF;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_MAX;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_MAXLENGTH;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_METHOD;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_MIN;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_NAME;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_PATTERN;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_READONLY;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_REQUIRED;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SELECTED;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SIZE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SPANCOL;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SRC;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_STYLE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_TITLE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_TYPE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_VALUE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_WIDTH;
import static de.tsl2.nano.h5.HtmlUtil.BTN_ASSIGN;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_BLACK;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_LIGHT_BLUE;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_LIGHT_GRAY;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_RED;
import static de.tsl2.nano.h5.HtmlUtil.STYLE_BACKGROUND_RADIAL_GRADIENT;
import static de.tsl2.nano.h5.HtmlUtil.STYLE_TEXT_ALIGN;
import static de.tsl2.nano.h5.HtmlUtil.TAG_BODY;
import static de.tsl2.nano.h5.HtmlUtil.TAG_BUTTON;
import static de.tsl2.nano.h5.HtmlUtil.TAG_CELL;
import static de.tsl2.nano.h5.HtmlUtil.TAG_FONT;
import static de.tsl2.nano.h5.HtmlUtil.TAG_FORM;
import static de.tsl2.nano.h5.HtmlUtil.TAG_H3;
import static de.tsl2.nano.h5.HtmlUtil.TAG_HEADERCELL;
import static de.tsl2.nano.h5.HtmlUtil.TAG_HTML;
import static de.tsl2.nano.h5.HtmlUtil.TAG_IMAGE;
import static de.tsl2.nano.h5.HtmlUtil.TAG_INPUT;
import static de.tsl2.nano.h5.HtmlUtil.TAG_LINK;
import static de.tsl2.nano.h5.HtmlUtil.TAG_OPTION;
import static de.tsl2.nano.h5.HtmlUtil.TAG_PRE;
import static de.tsl2.nano.h5.HtmlUtil.TAG_ROW;
import static de.tsl2.nano.h5.HtmlUtil.TAG_SELECT;
import static de.tsl2.nano.h5.HtmlUtil.TAG_SPAN;
import static de.tsl2.nano.h5.HtmlUtil.TAG_TABLE;
import static de.tsl2.nano.h5.HtmlUtil.TAG_TBODY;
import static de.tsl2.nano.h5.HtmlUtil.VAL_100PERCENT;
import static de.tsl2.nano.h5.HtmlUtil.VAL_ALIGN_CENTER;
import static de.tsl2.nano.h5.HtmlUtil.VAL_ALIGN_LEFT;
import static de.tsl2.nano.h5.HtmlUtil.VAL_ALIGN_RIGHT;
import static de.tsl2.nano.h5.HtmlUtil.appendElement;
import static de.tsl2.nano.h5.HtmlUtil.enable;
import static de.tsl2.nano.h5.HtmlUtil.style;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.tsl2.nano.Environment;
import de.tsl2.nano.Messages;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanAttribute;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.IBeanCollector;
import de.tsl2.nano.bean.def.IColumn;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.IPresentableColumn;
import de.tsl2.nano.bean.def.Presentable;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.bean.def.ValueExpressionFormat;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.format.GenericParser;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.util.DateUtil;
import de.tsl2.nano.util.FileUtil;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.StringUtil;

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
    protected transient int currentTabIndex;
    Log LOG = LogFactory.getLog(Html5Presentation.class);

    protected static final StringBuilder EMPTY_CONTENT = new StringBuilder();

    public static final String L_GRIDWIDTH = "layout.gridwidth";

    /** indicator for server to handle a link, that was got as link (method=GET) not as a file */
    public static final String PREFIX_ACTION = "!!!";

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
    public Collection<IAction> getApplicationActions() {
        super.getApplicationActions();

        appActions.add(new SecureAction(bean.getClazz(),
            "configure",
            IAction.MODE_UNDEFINED,
            false,
            "icons/compose.png") {
            @Override
            public Object action() throws Exception {
                return BeanConfigurator.create((Class<Serializable>) bean.getClazz());
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        return appActions;
    }

    @Override
    public void reset() {
        super.reset();
        
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public String build(BeanDefinition<?> model, String message, boolean interactive, BeanDefinition<?>... navigation) {
        try {
            currentTabIndex = 0;
            Element form = model != null ? model.setPresentationHelper(new Html5Presentation(model)).createPage(null,
                message,
                interactive,
                navigation) : createPage(null, "Leaving Application!<br/>Restart", false, navigation);

            String html = HtmlUtil.toString(form.getOwnerDocument());
            if (LOG.isDebugEnabled())
                FileUtil.writeBytes(html.getBytes(), Environment.getConfigPath() + "html-server-response.html", false);
            return html;
        } catch (Exception ex) {
            return ForwardedException.toRuntimeEx(ex, true).getMessage();
        }
    }

    Element createFormDocument(String name, boolean interactive) {
        Element body = createHeader(name, interactive);
        return appendElement(body,
            TAG_FORM,
            ATTR_ACTION,
            "?",
            ATTR_METHOD,
            Environment.get("html5.http.method", "post"));
    }

    Element createHeader(String title, boolean interactive) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            /*
             * try to read html-page from a template. if not existing, create header and
             * body programatically.
             * TODO: cache the page
             */
            Document doc;
            Element body = null;
            File metaFrame = new File(Environment.getConfigPath() + "css/meta-frame.html");
            boolean useCSS = metaFrame.canRead();
            if (useCSS) {
                try {
                    doc = factory.newDocumentBuilder().parse(metaFrame);
                    NodeList childs = doc.getFirstChild().getChildNodes();
                    for (int i = 0; i < childs.getLength(); i++) {
                        if (childs.item(i).getNodeName().equals("body")) {
                            body = (Element) childs.item(i);
                            break;
                        }
                    }
                    if (body == null)
                        throw new IllegalStateException("error on loading file " + metaFrame.getAbsolutePath()
                            + ": missing body tag!");
                } catch (Exception e) {
                    LOG.error("error on loading file " + metaFrame.getAbsolutePath());
                    ForwardedException.forward(e);
                    return null;
                }
            } else {
                doc = factory.newDocumentBuilder().newDocument();
                Element html = doc.createElement(TAG_HTML);
                doc.appendChild(html);
                body = createMetaAndBody(html, interactive);
            }

            Element row = appendElement(createGrid(body, null, 3), TAG_ROW);
            Element c1 = appendElement(row, TAG_CELL);
            Element c2 = appendElement(row, TAG_CELL);
            c1 = appendElement(c1, TAG_LINK, ATTR_HREF, "../nano.h5.html");
            appendElement(c1,
                TAG_IMAGE,
                content(Environment.getBuildInformations()),
                ATTR_SRC,
                "icons/beanex-logo-micro.jpg");
            appendElement(c2, TAG_H3, content(title), ATTR_ALIGN, ALIGN_CENTER);
            Element c3 = appendElement(row, TAG_CELL, ATTR_ALIGN, ALIGN_RIGHT);
            if (interactive && bean != null) {
                if (useCSS) {
                    Element menu = createMenu(c3, "Menu");
                    createSubMenu(menu, "Application", "iconic home", getApplicationActions());
                    createSubMenu(menu, "Session", "iconic map-pin", getSessionActions());
                    createSubMenu(menu, "Page", "iconic magnifying-glass", getPageActions());
                } else {
                    c3 = appendElement(c3,
                        TAG_FORM,
                        ATTR_ACTION,
                        "?",
                        ATTR_METHOD,
                        Environment.get("html5.http.method", "post"));
                    Collection<IAction> actions = getPageActions();
                    actions.addAll(getApplicationActions());
                    actions.addAll(getSessionActions());
                    createActionPanel(c3, actions,
                        Environment.get("html.show.header.button.text", true),
                        ATTR_ALIGN, ALIGN_RIGHT);
                }
            }
            return body;
        } catch (ParserConfigurationException e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    private Element createMetaAndBody(Element html, boolean interactive) {
        Element head = appendElement(html, "head");
//        appendElement(head, "title", "nano h5 application");

//      String template = "<link href='http://fonts.googleapis.com/css?family=Droid+Sans' rel='stylesheet' type='text/css'>"
//  + "<meta charset=\"utf-8\">"
//  + "<title>Pure CSS3 Menu</title>"
//  + "<link href=\"style.css\" media=\"screen\" rel=\"stylesheet\" type=\"text/css\" />"
//  + "<link href=\"iconic.css\" media=\"screen\" rel=\"stylesheet\" type=\"text/css\" />"
//  + "<script src=\"prefix-free.js\"></script>";
//      head.setTextContent(template);

//        appendElement(head, "link", "href", "http://fonts.googleapis.com/css?family=Droid+Sans", "rel", "stylesheet",
//            "type", "text/css");
//        appendElement(head, "link", "href", "css/style.css", "media", "screen", "rel", "stylesheet", "type", "text/css");
//        appendElement(head, "link", "href", "css/iconic.css", "media", "screen", "rel", "stylesheet", "type",
//            "text/css");
//        appendElement(head, "script", "src", "prefix-free.js");
//        appendElement(head, "meta", "charset", "utf-8");

        Element body =
            appendElement(html, TAG_BODY);
        if (!interactive)
            HtmlUtil.appendAttributes(body, "background", "icons/spe.jpg", ATTR_STYLE,
                STYLE_BACKGROUND_RADIAL_GRADIENT);
        return body;
    }

    /**
     * builds a full html document
     * 
     * @param parent (optional) parent element to place itself into
     * @param message (optional) status message to be presented at bottom
     * @param interactive if false, no buttons and edit fields are shown
     * @return html document
     */
    public Element createPage(Element parent, String message, boolean interactive, BeanDefinition<?>... navigation) {
        boolean isRoot = parent == null;
        if (isRoot) {
            if (bean == null) {
                return createFormDocument(message, interactive);
            } else {
                parent = createFormDocument(bean.getName(), interactive);
            }
        }

        if (navigation.length > 0) {
            for (BeanDefinition<?> bean : navigation) {
                appendElement((Element) parent, TAG_LINK, content("->" + bean.toString()), ATTR_HREF, bean.getName(),
                    ATTR_STYLE, "color: #FFFFFF;");
            }
        }

        if (bean instanceof BeanCollector) {
            createCollector(parent, (BeanCollector) bean, interactive);
        } else {
            createBean(parent, (Bean<?>) bean, interactive);
        }
        if (isRoot) {
            if (interactive)
                createBeanActions((Element) parent, bean);
            createFooter(((Element) parent).getOwnerDocument(), message);
        }
        return parent;
    }

    public String createMessagePage(String message) {
        return createMessagePage("message.template", message, null);
    }
    public static String createMessagePage(String templateName, String message, URL serviceURL) {
        InputStream stream = Environment.getResource(templateName);
        String startPage = String.valueOf(FileUtil.getFileData(stream, null));
        return StringUtil.insertProperties(startPage,
            MapUtil.asMap("url", serviceURL, "name", message));
    }

    /**
     * createBean
     * 
     * @param parent
     */
    private void createBean(Element parent, Bean<?> bean, boolean interactive) {
        int columns = bean.getPresentable().layout(L_GRIDWIDTH, Environment.get("layout.default.columncount", 3));
        Element panel = createGrid(parent, Environment.translate("tsl2nano.input", false), columns);
        appendAttributes((Element) panel.getParentNode(), bean.getPresentable());
        createLayout((Element) panel.getParentNode(), bean.getPresentable());
        Bean<T> vbean = (Bean<T>) bean;
        List<BeanValue<?>> beanValues = vbean.getBeanValues();
        boolean firstFocused = false;
        int count = 0;
        Element field = null;
        for (BeanValue<?> beanValue : beanValues) {
            if (beanValue.isBean()) {
                Bean<?> bv = (Bean<?>) beanValue.getInstance();
                bv.setPresentationHelper(new Html5Presentation()).createPage(parent, null, interactive);
                if (vbean.getActions() != null)
                    vbean.getActions().addAll(bv.getActions());
                else
                    vbean.setActions(bv.getActions());
            } else {
                Element fparent = (Element) (field == null || (++count % (columns / 3) == 0) ? panel
                    : field.getParentNode().getParentNode());
                field = createField(fparent, beanValue);
                if (!firstFocused) {
                    field.setAttribute(ATTR_AUTOFOCUS, ATTR_AUTOFOCUS);
                    firstFocused = true;
                }
            }
        }
    }

    /**
     * createCollector
     * 
     * @param parent parent
     * @param bean collector to create a table for
     * @param interactive if false, no buttons and edit fields are shown
     * @return html table tag
     */
    Element createCollector(Element parent, BeanCollector<Collection<T>, T> bean, boolean interactive) {
        /*
         * workaround to enable buttons
         */
        if (bean.getSelectionProvider().isEmpty())
            bean.selectFirstElement();

        /*
         * create the column header
         */
        Element grid;
        if (interactive)
            grid = createGrid(parent, bean.toString(), false, bean);
        else
            grid = createGrid(parent, bean.toString(), false, getColumnNames(bean.getColumnDefinitions()));
        appendAttributes(grid, bean.getPresentable());

        bean.addMode(MODE_MULTISELECTION);
        if (interactive && bean.hasMode(IBeanCollector.MODE_SEARCHABLE)) {
            Collection<T> data = new LinkedList<T>(bean.getSearchPanelBeans());
            //this looks complicated, but if currentdata is a collection with a FilteringIterator, we need a copy of the filtered items!
            data.addAll(CollectionUtil.getList(bean.getCurrentData().iterator()));
            createTableContent(grid, bean, data, interactive, 0, 1);
        } else
            createTableContent(grid, bean, bean.getBeanFinder().getData(), interactive);

        return grid;
    }

    /**
     * getColumnNames
     * 
     * @param colDefs
     * @return
     */
    private String[] getColumnNames(Collection<IPresentableColumn> colDefs) {
        String[] cnames = new String[colDefs.size() + 1];

        cnames[0] = Messages.getString("tsl2nano.row");
        for (IColumn c : colDefs) {
            int i = c.getIndex() + 1;
            if (i > -1 && i < cnames.length)
                cnames[i] = c.getDescription();
            else
                LOG.warn("the index " + i + " of column " + c + " is impossible");
        }
        return cnames;
    }

    private void appendAttributes(Element grid, IPresentable p) {
        HtmlUtil.appendAttributes(grid/*, ATTR_NAME, p.getLabel(), p.getDescription(), p.getType(), p.getEnabler(), p.getWidth(), p.getHeight()*/
            ,
            ATTR_ALIGN,
            getTextAlignment(p.getStyle()),
            ATTR_BGCOLOR,
            convert(ATTR_BGCOLOR, p.getBackground(), COLOR_LIGHT_BLUE),
            ATTR_COLOR,
            convert(ATTR_COLOR, p.getForeground(), COLOR_BLACK)/*, p.getIcon()*/);

        if (p.getLayout() instanceof Map) {
            HtmlUtil.appendAttributes(grid, MapUtil.asArray((Map<String, Object>) p.getLayout()));
        }
        if (p.getLayoutConstraints() instanceof Map) {
            HtmlUtil.appendAttributes(grid, MapUtil.asArray((Map<String, String>) p.getLayoutConstraints()));
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
    void createTableContent(Element grid,
            IBeanCollector<?, T> tableDescriptor,
            Collection<T> data,
            boolean interactive,
            Integer... editableRowNumbers) {
        Collection<Integer> editableRows = Arrays.asList(editableRowNumbers);
        ValueExpressionFormat<T> vef = null;
        if (data.size() > 0 && editableRows.size() > 0) {
            vef = new ValueExpressionFormat(data.iterator().next().getClass());
        }
        int i = 0;
        boolean hasSearchFilter = tableDescriptor.getBeanFinder().getFilterRange() != null;
        currentTabIndex = data.size() > editableRowNumbers.length ? editableRowNumbers.length * -1 : 0;
        for (T item : data) {
            if (hasSearchFilter && editableRows.contains(i++))
                addEditableRow(grid, tableDescriptor, item, i == 1 ? prop(KEY_FILTER_FROM_LABEL)
                    : i == 2 ? prop(KEY_FILTER_TO_LABEL) : toString(item, vef));
            else {
                addRow(grid, tableDescriptor.hasMode(MODE_MULTISELECTION) && interactive, tableDescriptor, item);
            }
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
        Element div = appendElement(parent, "div", "class", "wrap");
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
        Element list = appendElement(menu, "li");
        Element alink = appendElement(list, TAG_LINK, content(name), ATTR_HREF, name);
        Element span = appendElement(alink, "span", "class", icon);
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
        Element panel = createGrid(parent, "Actions", actions != null ? 1 + actions.size() : 1);
        Element row = appendElement(panel, TAG_ROW);
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
            null,
            (a.getImagePath() != null ? a.getImagePath() : "icons/" + a.getShortDescription() + ".gif"),
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
        String closeLabel = Messages.getStringOpt("tsl2nano.close", true);
        if (model.isMultiValue() && ((BeanCollector) model).hasMode(MODE_ASSIGNABLE)) {
            String assignLabel = Messages.getStringOpt("tsl2nano.assign", true);
            createAction(panel, BTN_ASSIGN, assignLabel, assignLabel, "submit", "icons/links.png", true, true, false);
        }
        createAction(panel, IAction.CANCELED, closeLabel, closeLabel, null, "icons/stop.png", true, false, true);
        return panel;
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
        String label = Environment.translate(id, true);
        return createAction(cell, id, label, label, type, image, true, false, false);
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
            String image,
            boolean enabled,
            boolean asDefault,
            boolean formnovalidate) {
        String name = label != null ? label : tooltip;
        int isc = name.indexOf('&') + 1;
        String shortCut = name.substring(isc, isc + 1).toLowerCase();
        label = Messages.stripMnemonics(label);
        Element action = appendElement(cell,
            TAG_BUTTON,
            content(label),
            ATTR_NAME,
            id,
            ATTR_TITLE,
            tooltip,
            ATTR_TYPE,
            type,
            ATTR_ACCESSKEY,
            shortCut,
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

    Element createGrid(Element parent, String title, int columns) {
        return createGrid(parent, title, columns, false);
    }

    Element createGrid(Element parent, String title, int columns, boolean border) {
        String c[] = new String[columns];
        Arrays.fill(c, "");
        return createGrid(parent, title, border, c);
    }

    Element createGrid(Element parent, String title, boolean border, String... columns) {
        Element table = appendElement(parent,
            TAG_TABLE,
            ATTR_BORDER,
            border ? "1" : "0",
            ATTR_FRAME,
            "box",
            ATTR_WIDTH,
            VAL_100PERCENT,
            ATTR_BGCOLOR,
            COLOR_LIGHT_BLUE,
            "sortable");
        if (Environment.get("html5.table.show.caption", false))
            appendElement(table, "caption", content(title));
        Element colgroup = appendElement(table, TAG_ROW);
        for (int i = 0; i < columns.length; i++) {
            appendElement(colgroup, TAG_HEADERCELL, content(columns[i]), ATTR_BGCOLOR, COLOR_LIGHT_GRAY);
        }
        return appendElement(table, TAG_TBODY);
    }

    Element createGrid(Element parent, String title, boolean border, BeanCollector<?, ?> collector) {
        Element table = appendElement(parent,
            TAG_TABLE,
            ATTR_BORDER,
            border ? "1" : "0",
            ATTR_FRAME,
            "box",
            ATTR_WIDTH,
            VAL_100PERCENT,
            ATTR_BGCOLOR,
            COLOR_LIGHT_BLUE,
            "sortable");
        if (Environment.get("html5.table.show.caption", false))
            appendElement(table, "caption", content(title));
        if (collector.getPresentable() != null)
            appendAttributes(table, collector.getPresentable());
        Element colgroup = appendElement(table, TAG_ROW, ATTR_BORDER, "1");
        if (collector.hasMode(MODE_MULTISELECTION))
            appendElement(colgroup, TAG_HEADERCELL, content());
        Collection<IPresentableColumn> columns = collector.getColumnDefinitions();
        for (IPresentableColumn c : columns) {
            Element th = appendElement(colgroup,
                TAG_HEADERCELL,
                ATTR_BORDER,
                "1",
                "style",
                "-webkit-transform: scale(1.2);",
                ATTR_BGCOLOR,
                COLOR_LIGHT_GRAY);
            createAction(th, c.getSortingAction(collector));
        }
        return appendElement(table, TAG_TBODY);
    }

    protected Element addRow(Element grid, boolean multiSelection, IBeanCollector<?, T> tableDescriptor, T item) {
        boolean isSelected = tableDescriptor.getSelectionProvider() != null ? tableDescriptor.getSelectionProvider()
            .getValue()
            .contains(item) : false;

        Element row = appendElement(grid,
            TAG_ROW,
            "onclick",
            "this.getElementsByTagName('input')[0].onclick()",
            "ondblclick",
            "location=this.getElementsByTagName('a')[0]",
            "tabindex",
            ++currentTabIndex + "");

        //first cell: bean reference as link
        String length = String.valueOf(grid.getChildNodes().getLength() - 1);
        Element firstCell = appendElement(row, TAG_CELL);
        appendElement(firstCell, TAG_LINK, content(), ATTR_HREF, length);
        if (multiSelection)
            appendElement(firstCell,
                TAG_INPUT,
                content(),
                ATTR_NAME,
                length,
                ATTR_TYPE,
                "checkbox",
                enable(ATTR_CHECKED, isSelected));

        Collection<IPresentableColumn> colDefs = tableDescriptor.getColumnDefinitions();
        Element cell;
        String value;
        for (IPresentableColumn c : colDefs) {
            value = tableDescriptor.getColumnText((T) item, c.getIndex());
            cell = appendElement(row, TAG_CELL, content(value));
            if (c.getPresentable() != null)
                appendAttributes(cell, c.getPresentable());
            if (Messages.isMarkedAsProblem(value))
                HtmlUtil.appendAttributes(cell, ATTR_COLOR, COLOR_RED);
        }
        return row;
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
    Element addEditableRow(Element table, IBeanCollector<?, T> tableDescriptor, T element, String rowName) {
        Element row = appendElement(table, TAG_ROW, ATTR_BGCOLOR, COLOR_LIGHT_GRAY, ATTR_ALIGN, ALIGN_CENTER);
        if (rowName != null) {
            appendElement(row, TAG_CELL, content(rowName));
        }

        boolean focusSet = false;
        Collection<IPresentableColumn> colDefs = tableDescriptor.getColumnDefinitions();
        for (IPresentableColumn c : colDefs) {
            String value = tableDescriptor.getColumnText(element, c.getIndex());
            Element cell0 = appendElement(row, TAG_CELL);
            Element cell = appendElement(cell0, TAG_SPAN);
            appendElement(cell,
                TAG_INPUT,
                content(),
                ATTR_NAME,
                rowName + "." + c.getName(),
                ATTR_TYPE,
                "text",
                "tabindex",
                ++currentTabIndex + "",
                ATTR_VALUE,
                value,
                enable(ATTR_AUTOFOCUS, !focusSet));
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

    Element createField(Element parent, BeanValue<?> beanValue) {
        Element row = parent.getNodeName().equals(TAG_ROW) ? parent : appendElement(parent, TAG_ROW);
        //first the label
        Element cellLabel = appendElement(row, TAG_CELL);
        appendElement(cellLabel,
            TAG_FONT,
            content(beanValue.getDescription() + (beanValue.nullable() ? "" : " (*)")),
            ATTR_COLOR,
            (String) BeanUtil.valueOf(beanValue.getPresentation().getBackground(),
                Environment.get("default.attribute.label.color", "#0000cc")),
            enable(ATTR_HIDDEN, !beanValue.getPresentation().isVisible()),
            enable(ATTR_REQUIRED, !beanValue.nullable()));
        //create the layout and layout-constraints
        Element cell = appendElement(row, TAG_CELL);
        cell = createLayoutConstraints(cell, beanValue.getPresentation());
        cell = createLayout(cell, beanValue.getPresentation());
        //now the field itself
        Element input;
        if (beanValue.getAllowedValues() == null) {
            RegExpFormat regexpFormat =
                beanValue.getFormat() instanceof RegExpFormat ? (RegExpFormat) beanValue.getFormat()
                    : null;
            String type = getType(beanValue);
            boolean isOption = "checkbox".equals(type);
            input = appendElement(cell,
                TAG_INPUT,
                /*content(getSuffix(regexpFormat)),*/
                ATTR_TYPE,
                type,
                ATTR_NAME,
                beanValue.getName(),
                ATTR_PATTERN,
                regexpFormat != null ? regexpFormat.getPattern() : Environment.get("default.pattern.regexp", ".*"),
                ATTR_STYLE,
                getTextAlignmentAsStyle(beanValue.getPresentation().getStyle()),
                ATTR_SIZE,/* 'width' doesn't work, so we set the displaying char-size */
                "50",
                ATTR_WIDTH,
                "250",
                ATTR_MIN,
                StringUtil.toString(beanValue.getMininum()),
                ATTR_MAX,
                StringUtil.toString(beanValue.getMaxinum()),
                ATTR_MAXLENGTH,
                (beanValue.length() > 0 ? String.valueOf(beanValue.length()) : String.valueOf(Integer.MAX_VALUE)),
                (isOption ? enable(ATTR_CHECKED, (Boolean) beanValue.getValue()) : ATTR_VALUE),
                (isOption ? null : getValue(beanValue, type)),
                ATTR_TITLE,
                beanValue.getDescription(),
                "tabindex",
                beanValue.getPresentation().layout("tabindex", ++currentTabIndex).toString(),
                enable(ATTR_HIDDEN, !beanValue.getPresentation().isVisible()),
                enable(ATTR_DISABLED, !beanValue.getPresentation().getEnabler().isActive()),
                enable(ATTR_READONLY, !beanValue.getPresentation().getEnabler().isActive()),
                enable(ATTR_REQUIRED, !beanValue.nullable()));

            if (beanValue.getPresentation().getEnabler().isActive()) {
                //create a finder button
                if (beanValue.isSelectable()) {
                    Element a = createAction(cell,
                        beanValue.getName() + IPresentable.POSTFIX_SELECTOR,
                        Environment.translate("tsl2nano.finder.action.label", false),
                        Environment.translate("tsl2nano.selection", true),
                        null,
                        null,
                        beanValue.hasWriteAccess(),
                        false,
                        true);
                    HtmlUtil.appendAttributes(a, "tabindex", String.valueOf(++currentTabIndex));

                }
            }
        } else {
            input = createSelectorField(cell, beanValue);
        }
        appendAttributes(input, beanValue.getPresentation());

        if (beanValue.hasStatusError()) {
            row = appendElement(parent, TAG_ROW);
            appendElement(row, TAG_FONT, ATTR_SIZE, "-1", ATTR_COLOR, COLOR_RED);
//            appendElement(row, TAG_HEADERCELL, content(null));
            appendElement(row, TAG_CELL, content(beanValue.getStatus().message()), ATTR_SPANCOL, "3");
        }
        return input;
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
        return type.startsWith("date") ? StringUtil.toString(DateUtil.toSqlDateString((Date) beanValue.getValue()))
            : beanValue.getValueText();
    }

    private String getSuffix(RegExpFormat regexpFormat) {
        return regexpFormat != null && regexpFormat.getDefaultFormatter() != null ? ((GenericParser) regexpFormat
            .getDefaultFormatter()).getPostfix()
            : null;
    }

    private String getTextWithoutSuffix(String valueText, RegExpFormat regExpFormat) {
        String suffix = getSuffix(regExpFormat);
        if (suffix == null)
            return valueText;
        return StringUtil.substring(valueText, null, getSuffix(regExpFormat)).trim();
    }

    private String getTextAlignment(int style) {
        return NumberUtil.hasBit(style, IPresentable.ALIGN_RIGHT) ? VAL_ALIGN_RIGHT : NumberUtil.hasBit(style,
            IPresentable.ALIGN_CENTER) ? VAL_ALIGN_CENTER : VAL_ALIGN_LEFT;
    }

    private String getTextAlignmentAsStyle(int style) {
        return NumberUtil.hasBit(style, IPresentable.ALIGN_RIGHT) ? style(STYLE_TEXT_ALIGN, VAL_ALIGN_RIGHT)
            : NumberUtil.hasBit(style, IPresentable.ALIGN_CENTER) ? style(STYLE_TEXT_ALIGN, VAL_ALIGN_CENTER)
                : style(STYLE_TEXT_ALIGN, VAL_ALIGN_LEFT);
    }

    Element createSelectorField(Element cell, BeanValue<?> beanValue) {
        Element select =
            appendElement(cell,
                TAG_SELECT,
                ATTR_WIDTH,
                VAL_100PERCENT,
                ATTR_NAME,
                beanValue.getName(),
                ATTR_PATTERN,
                (beanValue.getFormat() instanceof RegExpFormat ? ((RegExpFormat) beanValue.getFormat()).getPattern()
                    : null),
                "tabindex",
                beanValue.getPresentation().layout("tabindex", ++currentTabIndex).toString(),
                enable(ATTR_HIDDEN, !beanValue.getPresentation().isVisible()),
                enable(ATTR_READONLY, !beanValue.getPresentation().getEnabler().isActive()),
                enable(ATTR_REQUIRED, !beanValue.nullable()));
        Collection<?> values = beanValue.getAllowedValues();
        Object selected = beanValue.getValue();
        for (Object v : values) {
            appendElement(select, TAG_OPTION, content(v.toString()), enable(ATTR_SELECTED, v.equals(selected)));
        }
        return select;
    }

    private String getType(BeanValue<?> beanValue) {
        String type;
        int t = beanValue.getPresentation().getType();
        //let the specialized input types work
        t = NumberUtil.filterBits(t, IPresentable.TYPE_INPUT);
        switch (t) {
        case IPresentable.TYPE_SELECTION:
            type = "text";
            break;
        case IPresentable.TYPE_OPTION_RADIO:
            type = "radio";
            break;
        case IPresentable.TYPE_OPTION:
            type = "checkbox";
            break;
        case IPresentable.TYPE_DATE | IPresentable.TYPE_TIME:
            type = "datetime";
            break;
        case IPresentable.TYPE_TIME:
            type = "time";
            break;
        case IPresentable.TYPE_DATE:
            type = "date";
            break;
        case IPresentable.TYPE_INPUT_NUMBER:
            //floatings are not supported in html input type=number
            if (NumberUtil.isInteger(beanValue.getType())) {
                type = "number";
                break;
            }
        case IPresentable.TYPE_INPUT_TEL:
            type = "tel";
            break;
        case IPresentable.TYPE_INPUT_EMAIL:
            type = "email";
            break;
        case IPresentable.TYPE_INPUT_URL:
            type = "url";
            break;
        case IPresentable.TYPE_INPUT_PASSWORD:
            type = "password";
            break;
        case IPresentable.TYPE_INPUT_SEARCH:
            type = "search";
            break;
        case IPresentable.TYPE_ATTACHMENT:
            //on type = 'file' only the file-name is given (no path!)
//            type = "file";
//            break;
        default:
            type = "text";
            break;
        }
        return type;
    }

    Element createFooter(Document doc, String footer) {
        Element body = (Element) ((NodeList) doc.getElementsByTagName("body")).item(0);
        Element table = createGrid(body, "Status", 1);

        Element preFooter = doc.createElement(TAG_PRE);
        preFooter.setTextContent(footer);
        return addRow(table, preFooter);
    }

    @Override
    public boolean isDefaultAttribute(BeanAttribute attribute) {
        if (bean instanceof Bean && ((Bean) bean).getInstance() instanceof BeanConfigurator)
            return true;
        return super.isDefaultAttribute(attribute);
    }

    @Override
    public IPresentable createPresentable() {
        return new Html5Presentable();
    }

    @Override
    public IPresentable createPresentable(AttributeDefinition<?> attr) {
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

    public String decorate(String message) {
        Element body = createHeader(message, false);
        createAction(body, IAction.CANCELED, "submit", "icons/back.png");
        return body.toString();
    }
}

/**
 * Hmtl5-specialized {@link de.tsl2.nano.bean.def.Presentable}. Not possible to be handled as inner class, because of
 * xml-serialization.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
class Html5Presentable extends Presentable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    protected Html5Presentable() {
    }

    public Html5Presentable(AttributeDefinition<?> attr) {
        super(attr);
    }

    @Override
    public int getWidth() {
        String w = layout(ATTR_WIDTH);
        return w != null ? Integer.valueOf(w) : UNDEFINED;
    }

    @Override
    public int getHeight() {
        String h = layout(ATTR_HEIGHT);
        return h != null ? Integer.valueOf(h) : UNDEFINED;
    }

    /**
     * @return Returns the layout.
     */
    public HashMap<String, String> getLayout() {
        if (layout == null) {
            layout = Environment.get("default.layout", new LinkedHashMap<String, String>());
//            ((HashMap) layout).put("testKey", "testValue");
        }
        return (HashMap<String, String>) layout;
    }

    //to have write-access, we need this setter
    public <T extends IPresentable> T setLayout(HashMap<String, String> l) {
        this.layout = l;
        return (T) this;
    }

    /**
     * @return Returns the layoutConstraints.
     */
    public HashMap<String, String> getLayoutConstraints() {
        if (layoutConstraints == null) {
            layoutConstraints = Environment.get("default.layoutconstaints", new LinkedHashMap<String, String>());
        }
        return (HashMap<String, String>) layoutConstraints;
    }

    //to have write-access, we need this setter
    public <T extends IPresentable> T setLayoutConstraints(HashMap<String, String> lc) {
        this.layoutConstraints = lc;
        return (T) this;
    }
}
