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

import static de.tsl2.nano.h5.HtmlUtil.*;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_ACCESSKEY;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_ACTION;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_ALIGN;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_AUTOFOCUS;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_BGCOLOR;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_BORDER;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_CHECKED;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_COLOR;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_DISABLED;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_FORMNOVALIDATE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_FRAME;
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
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SPAN;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SRC;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_TEXT_ALIGN;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_TITLE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_TYPE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_VALUE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_WIDTH;
import static de.tsl2.nano.h5.HtmlUtil.BTN_ASSIGN;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_LIGHT_BLUE;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_LIGHT_GRAY;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_RED;
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
import static de.tsl2.nano.h5.HtmlUtil.VAL_FALSE;
import static de.tsl2.nano.h5.HtmlUtil.appendElement;
import static de.tsl2.nano.h5.HtmlUtil.enable;
import static de.tsl2.nano.util.bean.def.IBeanCollector.MODE_ASSIGNABLE;
import static de.tsl2.nano.util.bean.def.IBeanCollector.MODE_MULTISELECTION;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.tsl2.nano.Environment;
import de.tsl2.nano.Messages;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.format.RegularExpressionFormat;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.util.FileUtil;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.bean.BeanContainer;
import de.tsl2.nano.util.bean.BeanUtil;
import de.tsl2.nano.util.bean.def.Bean;
import de.tsl2.nano.util.bean.def.BeanCollector;
import de.tsl2.nano.util.bean.def.BeanDefinition;
import de.tsl2.nano.util.bean.def.BeanPresentationHelper;
import de.tsl2.nano.util.bean.def.BeanValue;
import de.tsl2.nano.util.bean.def.IBeanCollector;
import de.tsl2.nano.util.bean.def.IColumn;
import de.tsl2.nano.util.bean.def.IPageBuilder;
import de.tsl2.nano.util.bean.def.IPresentable;
import de.tsl2.nano.util.bean.def.IPresentableColumn;
import de.tsl2.nano.util.bean.def.ValueExpressionFormat;

/**
 * is able to present a bean as an html page. main method is {@link #build(Element, String)}.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Html5Presentation<T> extends BeanPresentationHelper<T> implements IPageBuilder<Element, String> {
    protected transient int currentTabIndex;
    Log LOG = LogFactory.getLog(Html5Presentation.class);

    protected static final StringBuilder EMPTY_CONTENT = new StringBuilder();

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
                navigation) : createPage(null, "Leaving Application!", false, navigation);

            String html = HtmlUtil.toString(form.getOwnerDocument());
            FileUtil.writeBytes(html.getBytes(), Environment.getConfigPath() + "html-server-response.html", false);
//            LOG.info(html);
            return html;
        } catch (Exception ex) {
            return ForwardedException.toRuntimeEx(ex, true).getMessage();
        }
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
                return getFormDocument(message, interactive);
            } else {
                parent = getFormDocument(bean.getName(), interactive);
            }
        }

        if (navigation.length > 0) {
            for (BeanDefinition<?> bean : navigation) {
                appendElement((Element) parent, TAG_LINK, content("->" + bean.getName()), ATTR_HREF, bean.getName());
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

    /**
     * createBean
     * 
     * @param parent
     */
    private void createBean(Element parent, Bean<?> bean, boolean interactive) {
        Element panel = createGrid(parent, "Eingabe-Dialog", bean.getPresentable().layout("layout.gridwidth", 3));
        Bean<T> vbean = (Bean<T>) bean;
        List<BeanValue<?>> beanValues = vbean.getBeanValues();
        for (BeanValue<?> beanValue : beanValues) {
            if (beanValue.isBean()) {
                Bean<?> bv = (Bean<?>) beanValue.getInstance();
                bv.setPresentationHelper(new Html5Presentation()).createPage(parent, null, interactive);
                if (vbean.getActions() != null)
                    vbean.getActions().addAll(bv.getActions());
                else
                    vbean.setActions(bv.getActions());
            } else {
                createField(panel, beanValue);
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
         * create the column header
         */
        Collection<IPresentableColumn> colDefs = bean.getColumnDefinitions();
        String[] cnames = new String[colDefs.size() + 1];
        cnames[0] = Messages.getString("swartifex.row");
        for (IColumn c : colDefs) {
            int i = c.getIndex() + 1;
            if (i > -1 && i < cnames.length)
                cnames[i] = c.getDescription();
            else
                LOG.warn("the index " + i + " of column " + c + " is impossible");
        }
        /*
         * show a search filter or/and fill the data
         */
        Element grid = createGrid(parent, "Table", false, cnames);
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
        currentTabIndex = data.size() > editableRowNumbers.length ? editableRowNumbers.length * -1 : 0;
        for (T item : data) {
            if (editableRows.contains(i++))
                addEditableRow(grid, tableDescriptor, item, i == 1 ? prop(KEY_FILTER_FROM_LABEL)
                    : i == 2 ? prop(KEY_FILTER_TO_LABEL) : toString(item, vef));
            else {
                Collection<IPresentableColumn> colDefs = tableDescriptor.getColumnDefinitions();
                String[] cells = new String[colDefs.size()];
                int ci = 0;
                for (Object c : colDefs) {
                    cells[ci] = tableDescriptor.getColumnText((T) item, ci);
                    ci++;
                }
                boolean isSelected = tableDescriptor.getSelectionProvider() != null ? tableDescriptor.getSelectionProvider()
                    .getValue()
                    .contains(item)
                    : false;
                addRow(grid, tableDescriptor.hasMode(MODE_MULTISELECTION) && interactive, isSelected, cells);
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

    private Element createActionPanel(Element parent, Collection<IAction> actions, String alignment) {
        Element panel = createGrid(parent, "Actions", actions != null ? 1 + actions.size() : 1);
        Element row = appendElement(panel, TAG_ROW);
        Element cell = appendElement(row, TAG_CELL, ATTR_ALIGN, alignment);
        if (actions != null) {
            for (IAction a : actions) {
                createAction(cell,
                    a.getId(),
                    a.getShortDescription(),
                    null,
                    a.getImagePath(),
                    a.isDefault(),
                    a.isEnabled(),
                    a.getActionMode() != IAction.MODE_DLG_OK);
            }
        }
        return cell;
    }

    /**
     * creates html buttons
     * 
     * @param form
     * @param model
     * @return html table containing the buttons
     */
    Element createBeanActions(Element form, BeanDefinition<?> model) {
        Element panel = createActionPanel(form, model.getActions(), ALIGN_CENTER);
        if (model.isMultiValue() && ((BeanCollector) model).hasMode(MODE_ASSIGNABLE))
            createAction(panel, BTN_ASSIGN, "submit", "icons/links.png");
        createAction(panel, IAction.CANCELED, "swartifex.close", null, "icons/stop.png", true, false, true);
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
        return createAction(cell, id, id, type, image, true, false, false);
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
            String type,
            String image,
            boolean enabled,
            boolean asDefault,
            boolean formnovalidate) {
        label = Messages.getStringOpt(label);
        int isc = label.indexOf('&') + 1;
        String shortCut = label.substring(isc, isc + 1).toLowerCase();
        label = Messages.stripMnemonics(label);
        Element action = appendElement(cell,
            TAG_BUTTON,
            content(label),
            ATTR_NAME,
            id,
            ATTR_TYPE,
            type,
            ATTR_ACCESSKEY,
            shortCut,
            "diabled",
            enabled ? ATTR_DISABLED : VAL_FALSE,
            ATTR_AUTOFOCUS,
            enable(ATTR_AUTOFOCUS, asDefault),
            ATTR_FORMNOVALIDATE,
            formnovalidate ? ATTR_FORMNOVALIDATE : VAL_FALSE);
        if (image != null) {
            appendElement(action, TAG_IMAGE, ATTR_SRC, image, ATTR_ALT, label);
        }
        return action;
    }

    Element getFormDocument(String name, boolean interactive) {
        Element body = createHeader(name, interactive);
        return appendElement(body,
            TAG_FORM,
            ATTR_ACTION,
            "?",
            ATTR_METHOD,
            Environment.get("html5.http.method", "post"));
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
        Element colgroup = appendElement(table, TAG_ROW);
        for (int i = 0; i < columns.length; i++) {
            appendElement(colgroup, TAG_HEADERCELL, content(columns[i]));
        }
        return appendElement(table, TAG_TBODY);
    }

    Element addRow(Element table, boolean multiSelection, boolean isSelected, String... cells) {
        Element row = appendElement(table,
            TAG_ROW,
            "onclick",
            "this.getElementsByTagName('input')[0].onclick()",
            "ondblclick",
            "location=this.getElementsByTagName('a')[0]",
            "tabindex",
            ++currentTabIndex + "");

        //first cell: bean reference as link
        String length = String.valueOf(table.getChildNodes().getLength() - 1);
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

        for (int i = 0; i < cells.length; i++) {
            appendElement(row, TAG_CELL, content(cells[i]));
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
        Element row = appendElement(table, TAG_ROW, ATTR_BGCOLOR, COLOR_LIGHT_GRAY);
        if (rowName != null) {
            appendElement(row, TAG_CELL, content(rowName));
        }

        Collection<IPresentableColumn> colDefs = tableDescriptor.getColumnDefinitions();
        for (IColumn c : colDefs) {
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
                value);
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

    Element createField(Element panel, BeanValue<?> beanValue) {
        Element row = appendElement(panel, TAG_ROW);
        //first the label
        Element cellLabel = appendElement(row, TAG_CELL);
        appendElement(cellLabel,
            TAG_FONT,
            content(beanValue.getDescription() + (beanValue.nullable() ? "" : " (*)")),
            ATTR_COLOR,
            (String) BeanUtil.valueOf(beanValue.getPresentation().getBackground(), "#0000cc"),
            enable(ATTR_HIDDEN, !beanValue.getPresentation().isVisible()),
            enable(ATTR_REQUIRED, !beanValue.nullable()));
        //now the field itself
        Element cell = appendElement(row, TAG_CELL);
        Element input;
        if (beanValue.getAllowedValues() == null) {
            //TODO: create 'appendInput(...)
            RegularExpressionFormat regexpFormat = beanValue.getFormat() instanceof RegularExpressionFormat ? (RegularExpressionFormat) beanValue.getFormat()
                : null;
            input = appendElement(cell,
                TAG_INPUT,
                ATTR_TYPE,
                getType(beanValue),
                ATTR_NAME,
                beanValue.getName(),
                ATTR_PATTERN,
                regexpFormat != null ? regexpFormat.getPattern() : ".*",
                ATTR_TEXT_ALIGN,
                getTextAlignment(beanValue.getPresentation().getStyle()),
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
                ATTR_VALUE,
                beanValue.getValueText(),
                ATTR_TITLE,
                beanValue.getDescription(),
                "tabindex",
                beanValue.getPresentation().layout("tabindex", ++currentTabIndex).toString(),
                enable(ATTR_HIDDEN, !beanValue.getPresentation().isVisible()),
                enable(ATTR_READONLY, !beanValue.getPresentation().getEnabler().isActive()),
                enable(ATTR_REQUIRED, !beanValue.nullable()));
            //create a finder button
            if (beanValue.getPresentation().getEnabler().isActive()) {
                if (BeanContainer.isInitialized() && (BeanContainer.instance().isPersistable(beanValue.getType()) || beanValue.isMultiValue())) {
                    createAction(cell,
                        beanValue.getName() + IPresentable.POSTFIX_SELECTOR,
                        "...",
                        null,
                        null,
                        beanValue.hasWriteAccess(),
                        false,
                        false);
                }
            }
        } else {
            input = createSelectorField(cell, beanValue);
        }
        if (beanValue.hasStatusError()) {
            row = appendElement(panel, TAG_ROW, ATTR_SPAN, "2");
            appendElement(row, TAG_FONT, ATTR_SIZE, "-1", ATTR_COLOR, COLOR_RED);
            appendElement(row, TAG_CELL, content(beanValue.getStatus().message()));
        }
        return input;
    }

    private String getTextAlignment(int style) {
        return NumberUtil.hasBit(style, IPresentable.ALIGN_RIGHT) ? VAL_ALIGN_RIGHT : NumberUtil.hasBit(style,
            IPresentable.ALIGN_CENTER) ? VAL_ALIGN_CENTER : VAL_ALIGN_LEFT;
    }

    Element createSelectorField(Element cell, BeanValue<?> beanValue) {
        Element select = appendElement(cell,
            TAG_SELECT,
            ATTR_WIDTH,
            VAL_100PERCENT,
            ATTR_NAME,
            beanValue.getName(),
            ATTR_PATTERN,
            (beanValue.getFormat() instanceof RegularExpressionFormat ? ((RegularExpressionFormat) beanValue.getFormat()).getPattern()
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
        case IPresentable.TYPE_DATE:
            type = "date";
            break;
        case IPresentable.TYPE_TIME:
            type = "time";
            break;
        case IPresentable.TYPE_INPUT_NUMBER:
            type = "number";
            break;
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
        default:
            type = "text";
            break;
        }
        return type;
    }

    Element createFooter(Document doc, String footer) {
        Element body = (Element) doc.getFirstChild().getFirstChild();
        Element table = createGrid(body, "Status", 1);

        Element preFooter = doc.createElement(TAG_PRE);
        preFooter.setTextContent(footer);
        return addRow(table, preFooter);
    }

    Element createHeader(String title, boolean interactive) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            Document doc = factory.newDocumentBuilder().newDocument();
            Element html = doc.createElement(TAG_HTML);
            doc.appendChild(html);
            Element body = appendElement(html, TAG_BODY, "background", interactive ? "icons/spe.jpg" : null);

            Element row = appendElement(createGrid(body, null, 3), TAG_ROW);
            Element c1 = appendElement(row, TAG_CELL);
            Element c2 = appendElement(row, TAG_CELL);
            c1 = appendElement(c1, TAG_LINK, ATTR_HREF, "tsl2.nano.h5.html");
            appendElement(c1, TAG_IMAGE, ATTR_SRC, "icons/beanex-logo-micro.jpg");
            appendElement(c2, TAG_H3, content(title), ATTR_ALIGN, ALIGN_CENTER);
            if (interactive && bean != null) {
                Element c3 = appendElement(row, TAG_CELL, ATTR_ALIGN, ALIGN_RIGHT);
                c3 = appendElement(c3,
                    TAG_FORM,
                    ATTR_ACTION,
                    "?",
                    ATTR_METHOD,
                    Environment.get("html5.http.method", "post"));
                createActionPanel(c3, getPresentationActions(), ALIGN_RIGHT);
            }
            return body;
        } catch (ParserConfigurationException e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    protected static final StringBuilder content() {
        return EMPTY_CONTENT;
    }

    protected static final StringBuilder content(String str) {
        return new StringBuilder(str);
    }
}
