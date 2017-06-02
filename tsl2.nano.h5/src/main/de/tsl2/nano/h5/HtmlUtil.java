/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Sep 29, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.h5;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * defines html tag- and attribute-names und helper methods. android doesn't support the full w3c implementation like
 * HtmlElement etc.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class HtmlUtil {
    private static final Log LOG = LogFactory.getLog(HtmlUtil.class);

    public static final String HTML_FORWARD = "<html><head><meta http-equiv=\"refresh\" content=\"0; URL={0}\"></head></html>";
    
    public static final String TAG_HTML = "html";
    public static final String TAG_HEAD = "head";
    public static final String TAG_BODY = "body";

    public static final String TAG_H1 = "h1";
    public static final String TAG_H2 = "h2";
    public static final String TAG_H3 = "h3";
    public static final String TAG_H4 = "h4";
    public static final String TAG_H5 = "h5";
    public static final String TAG_H6 = "h6";

    public static final String TAG_FORM = "form";
    public static final String ATTR_ACTION = "action";
    public static final String ATTR_METHOD = "method";

    public static final String TAG_DIV = "div";
    public static final String TAG_STYLE = "style";

    public static final String TAG_INPUT = "input";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_ID = "id";
    public static final String ATTR_CLASS = "class";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_VALUE = "value";
    public static final String ATTR_PATTERN = "pattern";
    public static final String ATTR_DISABLED = "disabled";
    public static final String ATTR_READONLY = "readonly";
    public static final String ATTR_HIDDEN = "hidden";
    public static final String ATTR_ACCESSKEY = "accesskey";
    public static final String ATTR_STYLE = "style";
    public static final String ATTR_TITLE = "title";
    public static final String ATTR_REQUIRED = "required";
    public static final String ATTR_LIST = "list";
    public static final String ATTR_MAXLENGTH = "maxlength";
    public static final String ATTR_CHECKED = "checked";
    public static final String ATTR_MIN = "min";
    public static final String ATTR_MAX = "max";
    public static final String ATTR_TEXT_ALIGN = "text-align";
    public static final String ATTR_FORMNOVALIDATE = "formnovalidate";
    public static final String ATTR_FORMTARGET = "formtarget";
    public static final String ATTR_AUTOFOCUS = "autofocus";

    public static final String ATTR_TYPE_INPUT = "input";
    public static final String ATTR_TYPE_TEXT = "text";
    public static final String ATTR_TYPE_DATE = "date";
    public static final String ATTR_TYPE_TIME = "time";
    public static final String ATTR_TYPE_CHECKBOX = "checkbox";
    public static final String ATTR_TYPE_RADIO = "radio";
    public static final String ATTR_TYPE_NUMBER = "number";
    public static final String ATTR_TYPE_TEL = "tel";
    public static final String ATTR_TYPE_EMAIL = "email";
    public static final String ATTR_TYPE_URL = "url";
    public static final String ATTR_TYPE_PASSWORD = "password";
    public static final String ATTR_TYPE_SEARCH = "search";

    public static final String TAG_TEXTAREA = "textarea";
    public static final String ATTR_ROWS = "rows";
    public static final String ATTR_COLS = "cols";
    public static final String ATTR_WRAP = "wrap";

    public static final String TAG_SELECT = "select";
    public static final String TAG_DATALIST = "datalist";
    public static final String TAG_OPTGROUP = "optgroup";
    public static final String TAG_OPTION = "option";
    public static final String ATTR_SIZE = "size";
    public static final String ATTR_SELECTED = "selected";
    public static final String ATTR_MULTIPLE = "multiple";

    public static final String TAG_LINK = "a";
    public static final String ATTR_HREF = "href";

    public static final String TAG_IMAGE = "img";
    public static final String ATTR_SRC = "src";
    public static final String ATTR_ALT = "alt";

    public static final String ATTR_DATA = "data";
    public static final String ATTR_SRCDOC = "srcdoc"; //--> iframe

    public static final String TAG_AUDIO = "audio";
    public static final String TAG_VIDEO = "video";
    public static final String TAG_EMBED = "embed";
    public static final String TAG_OBJECT = "object";
    public static final String TAG_CANVAS = "canvas";
    public static final String TAG_DEVICE = "device";
    public static final String TAG_FRAME = "iframe";
    public static final String TAG_SVG = "svg";

    public static final String TAG_BUTTON = "button";

    public static final String TAG_SCRIPT = "script";
    public static final String ATTR_TYPE_JS = "text/javascript";

    /* 
     * Layout
     */
    public static final String TAG_BREAK = "br";
    public static final String TAG_PARAGRAPH = "p";
    public static final String ATTR_ALIGN = "align";
    public static final String ALIGN_CENTER = "center";
    public static final String ALIGN_RIGHT = "end";
    public static final String ALIGN_LEFT = "left";
    public static final String TAG_PRE = "pre";

    public static final String TAG_EXP_DETAILS = "details";
    public static final String TAG_EXP_SUMMARY = "summary";
    public static final String ATTR_EXP_OPEN = "open";

    public static final String TAG_SPAN = "span";
    //the font tag is not supported in html5
//    public static final String TAG_FONT = "font";
    static final String ATTR_COLOR = "color";

    public static final String TAG_TABLE = "table";
    public static final String TAG_CAPTION = "caption";
    public static final String TAG_THEAD = "thead";
    public static final String TAG_TBODY = "tbody";
    public static final String TAG_COLGROUP = "colgroup";
    public static final String TAG_COL = "col";
    public static final String TAG_ROW = "tr";
    public static final String TAG_HEADERCELL = "th";
    public static final String TAG_CELL = "td";
    public static final String ATTR_FRAME = "frame";
    public static final String ATTR_BORDER = "border";
    public static final String ATTR_WIDTH = "width";
    public static final String ATTR_HEIGHT = "height";
    public static final String ATTR_BGCOLOR = "bgcolor";
    public static final String ATTR_SPAN = "span";
    public static final String ATTR_SPANCOL = "colspan";
    public static final String ATTR_SPANROW = "rowspan";
    public static final String ATTR_HEADERS = "header";
    public static final String ATTR_TABINDEX = "tabindex";

    public static final String COLOR_WHITE = "#FFFFFF";
    public static final String COLOR_BLACK = "#000000";
    public static final String COLOR_RED = "#FF0000";
    public static final String COLOR_GREEN = "#00FF00";
    public static final String COLOR_BLUE = "#0000FF";
    public static final String COLOR_GRAY = "#999999";
    public static final String COLOR_LIGHT_RED = "#FFCCCC";
    public static final String COLOR_LIGHT_GREEN = "#CCFFCC";
    public static final String COLOR_LIGHT_BLUE = "#CCCCFF";
    public static final String COLOR_LIGHTER_BLUE = "#DDDDFF";
    public static final String COLOR_LIGHT_GRAY = "#CCCCCC";
    public static final String COLOR_YELLOW = "#CCCC00";

    /** static styles */
    public static final String STYLE_BACKGROUND_COLOR = "background-color";
    public static final String STYLE_BACKGROUND_TRANSPARENT = "background: transparent;";
    public static final String STYLE_BACKGROUND_RADIAL_GRADIENT =
        "background: radial-gradient(#9999FF, #000000);";
    public static final String STYLE_BACKGROUND_FADING_TRANSITION =
        "-webkit-transition: background 2.5s ease-in-out; -moz-transition: background 2.5s ease-in-out; -ms-transition: background 2.5s ease-in-out; -o-transition: background 2.5s ease-in-out; transition: background 2.5s ease-in-out;";
    public static final String STYLE_BACKGROUND_FADING_KEYFRAMES =
        "-webkit-animation: fade 2s; -webkit-animation-fill-mode: both; -moz-animation: fade 2s; -moz-animation-fill-mode: both; -o-animation: fade 2s; -o-animation-fill-mode: both; animation: fade 2s; animation-fill-mode: both;";
    public static final String CSS_BACKGROUND_FADING_KEYFRAMES =
        "@-webkit-keyframes fade {0%{opacity: 0;} 100% {opacity: 1;}} @-moz-keyframes fade {0%{opacity: 0;} 100% {opacity: 1;}} @-o-keyframes fade {0%{opacity: 0;} 100% {opacity: 1;}} @keyframes fade {0%{opacity: 0;} 100% {opacity: 1;}}; ";
    public static final String STYLE_BACKGROUND_LIGHTGRAY = "background-color: rgba(247,247,247,.5);";
    /** dynamic styles. use method {@link #style(String, String)} to set styles! */
    public static final String STYLE_TEXT_ALIGN = "text-align";
    public static final String STYLE_FONT_COLOR = "color";
    public static final String STYLE_COLOR = "color";

    public static final String VAL_25PERCENT = "25%";
    public static final String VAL_100PERCENT = "100%";
    public static final String VAL_FALSE = Boolean.FALSE.toString();
    public static final String VAL_TRUE = Boolean.TRUE.toString();
    public static final String VAL_ALIGN_LEFT = "start";
    public static final String VAL_ALIGN_CENTER = "middle";
    public static final String VAL_ALIGN_RIGHT = "end";

    public static final String BTN_ASSIGN = "tsl2nano.assign";
    public static final String BTN_SUBMIT = "tsl2nano.save";
    public static final String BTN_CANCEL = "tsl2nano.cancel";
    public static final String BTN_SIDENAVCLOSE = "button.sidenav.close";

    public static final String BTN_SELECT_ALL = "tsl2nano.selectall";
    public static final String BTN_DESELECT_ALL = "tsl2nano.deselectall";
    public static final String BTN_PRINT = "tsl2nano.print";
    public static final String BTN_EXPORT = "tsl2nano.export";

    //frame-ids to create a perspective
    public static final String VAL_FRM_SELF = "_self";
    public static final String VAL_FRM_BODY_TOP = "_top";
    public static final String VAL_FRM_NEWTAB = "_blank";
    public static final String VAL_FRM_PARENT = "_parent";
    //unused yet!
    public static final String VAL_FRM_CENTER = "FRAME_CENTER";
    public static final String VAL_FRM_LEFT = "FRAME_LEFT";
    public static final String VAL_FRM_RIGHT = "FRAME_RIGHT";
    public static final String VAL_FRM_TOP = "FRAME_TOP";
    public static final String VAL_FRM_BOTTOM = "FRAME_BOTTOM";

    public static final String VAL_OPAC = "opacity:1.0;";
    public static final String VAL_OPACITY_0_5 = "opacity:0.5;";
    public static final String VAL_OPACITY_0_6 = "opacity:0.6;";
    public static final String VAL_OPACITY_0_7 = "opacity:0.7;";
    public static final String VAL_OPACITY_0_8 = "opacity:0.8;";
    public static final String VAL_OPACITY_0_9 = "opacity:0.9;";
    public static final String VAL_TRANSPARENT_INHERIT = "opacity:0.0;";
    public static final String VAL_TRANSPARENT = "background-color: transparent;";
    public static final String VAL_ROUNDCORNER =
        "padding:6px;-webkit-border-radius:6px;-moz-border-radius:6px;border-radius:6px;";

    public static final String XML_TAG_START = "\\<.*\\>";
    public static final String END_TAG = "/";
    public static final String PRE_ATTRIBUTE_FLAG = "FLAG:";

    public static final String CHAR_SUM = L("sum");
    public static final String CHAR_LE = L("le");
    public static final String CHAR_GE = L("ge");
    public static final String CHAR_DELTA = L("Delta");
    public static final String CHAR_DEG = L("deg");
    public static final String CHAR_ARROW_DOWN = L("dArr");
    public static final String CHAR_ARROW_UP = L("uArr");
    public static final String CHAR_ARROW_RIGHT = L("rArr");
    public static final String CHAR_ARROW_LEFT = L("lArr");
    public static final String CHAR_COPYRIGHT = L("copy");
    public static final String CHAR_POINT = L("bull");

    protected static final StringBuilder EMPTY_CONTENT = new StringBuilder();

    private static final char CSS_ID_SEPARATOR = '§';

    static String tableDivStyle;

    public static Element appendElements(Element parent, String... tagNames) {
        Document doc = parent.getOwnerDocument();
        for (int i = 0; i < tagNames.length; i++) {
            parent.appendChild(doc.createElement(tagNames[i]));
        }
        return parent;
    }

    public static Element embedElements(Element parent, String... tagNames) {
        Document doc = parent.getOwnerDocument();
        Element p = parent;
        for (int i = 0; i < tagNames.length; i++) {
            p = (Element) p.appendChild(doc.createElement(tagNames[i]));
        }
        return p;
    }

    /**
     * convenience delegating to {@link #appendElement(Element, String, String...)}, using tagAndContentAndattributes[0]
     * as tag name, tagAndContentAndattributes[1] as content.
     * 
     * @param parent parent element to put the new element into
     * @param tagAndContentAndattributes tag (index=0), content (index=1) and attributes with their values
     * @return new element
     */
    static Element appendTag(Element parent, String... tagAndContentAndattributes) {
        return appendElement(parent, tagAndContentAndattributes[0], content(tagAndContentAndattributes[1]),
            CollectionUtil.copyOfRange(tagAndContentAndattributes, 2, tagAndContentAndattributes.length));
    }

    public static Element appendElement(Element parent, String tagName, String... attributes) {
        return appendElement(parent, tagName, null, attributes);
    }

    public static Element appendElement(Element parent, String tagName, StringBuilder content, String... attributes) {
        Document doc = parent.getOwnerDocument();
        Element e = doc.createElement(tagName);
        if (content != null) {
            String c = content.toString();
            if (isHtml(c)) {
                appendNodesFromText(e, c);
            } else if (c.matches("\\&.+[;]")) {
                e.setNodeValue(c);
            } else {
                e.setTextContent(c);
            }
        }
        appendAttributes(e, attributes);
        parent.appendChild(e);
        return e;
    }

    /**
     * appendNodesFromText
     * 
     * @param e element to add new nodes
     * @param text to be parsed into nodes to be appended to the given element
     */
    public static void appendNodesFromText(Element e, String text) {
        try {
            LOG.info("trying to parse text into html:" + text);
            /*
             * 1. if the text contains literals outside of its tags, they have to
             *    be wrapped into symbolic tags.
             * 2. if there are more than one tag, create a root tag
             */
            int xmlPrefixEnd = text.indexOf("?>");
            if (xmlPrefixEnd != -1) {
                text = text.substring(xmlPrefixEnd + 3);
            }

            final String BEG = begin(TAG_SPAN), END = end(TAG_SPAN);
            String prefix = StringUtil.substring(text, null, "<");
            if (!Util.isEmpty(prefix)) {
                text = text.replace(prefix, BEG + prefix + END);
            }
            String postfix = StringUtil.substring(text, ">", null, true);
            if (!Util.isEmpty(postfix)) {
                text = text.replace(postfix, BEG + postfix + END);
            }

            text = BEG + text + END;

            /*
             * now, parse the text
             */
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(text));
            Document d = builder.parse(is);

            /*
             * fill the parsed nodes into our document
             */
            Document doc = e.getOwnerDocument();
            NodeList childNodes = d.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                e.appendChild(doc.adoptNode(childNodes.item(i)));
            }
        } catch (Exception e1) {
            //don't interrupt the html response but show the error message.
            e.setTextContent(e1.getLocalizedMessage());
        }
    }

    /**
     * appends all given not-null attributes to the element.<br/>
     * the order of attributes has to follow key1, value1, key2, value2, ...
     * 
     * @param e element to add the attributes
     * @param attributes key/value pairs
     * @return the given element
     */
    public static Element appendAttributes(Element e, Object... attributes) {
        Document doc = e.getOwnerDocument();
        for (int i = 0; i < attributes.length; i++) {
            //disabled flag attribute --> continue
            if (Util.isEmpty(attributes[i])) {
                continue;
            }
            String attrName = (String) attributes[i];
            Attr attr = null;
            if (attrName.startsWith(PRE_ATTRIBUTE_FLAG)) {
                attrName = StringUtil.substring(attrName, PRE_ATTRIBUTE_FLAG, null);
                attr = doc.createAttribute(attrName);
            } else if (i < attributes.length - 1 && !Util.isEmpty(attributes[i + 1])) {
                if (ATTR_ID.equals(attrName))
                    attrName = cssID(attrName);
                attr = doc.createAttribute(attrName);
                attr.setValue(Util.asString(attributes[++i]));
            } else {
                //if it is a flag and it's the last attribute
                attr = doc.createAttribute(attrName);
            }
            e.setAttributeNode(attr);
        }
        return e;
    }

    public static Element appendLink(Element parent, String name, String href) {
        return appendElement(parent, TAG_LINK, new StringBuilder(name), ATTR_HREF, href);
    }

    public static Element appendStyle(Element parent, String... styles) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < styles.length; i++) {
            if (styles[i] == null)
                continue;
            else if (styles[i].contains(":"))
                b.append(styles[i]);
            else if (i < styles.length - 1 && styles[i + 1] != null) {
                b.append(style(styles[i], styles[++i]));
            }
        }
        return b.length() > 0 ? appendAttributes(parent, TAG_STYLE, b) : parent;
    }

    /**
     * usable to add an element attribute of type boolean. e.g.: formnovalidate='formnovalidate' or 'false'.
     * 
     * @param name boolean attribute name
     * @param enable whether to activate the attribute
     * @return attribute name and value
     */
    public static final String enable(String name, boolean enable) {
        return /*"name = " +*/(enable ? name : null);
    }

    public static final String enableFlag(String name, boolean enable) {
        return /*"name = " +*/(enable ? PRE_ATTRIBUTE_FLAG + name : null);
    }

    public static final String enableName(String name, boolean enable) {
        return name + " = " + enable(name, enable);
    }

    public static final String enableBoolean(String name, boolean enable) {
        boolean enabled = enable(name, enable) != null;
        return name + " = " + enabled;
    }

    /**
     * exactly one one-value style can be appended as last style element.
     * 
     * @param styles
     * @return
     */
    public static final String styles(String... styles) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < styles.length; i += 2) {
            b.append(i + 1 < styles.length ? style(styles[i], styles[i + 1]) : styles[i]);
        }
        return b.toString();
    }

    public static final String style(String styleKey, Object styleValue) {
        return styleKey + ": " + styleValue + ";";
    }

    /**
     * creates an utf-8 string with indentation.
     * 
     * @param doc document to transform to a string
     * @return string
     */
    public static String toString(Document doc) {
        //set up a transformer
        try {
            Transformer trans = TransformerFactory.newInstance().newTransformer();
//            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
//            trans.setOutputProperty(OutputKeys.METHOD, "xml");
            trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            //create string from xml tree
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            //on android systems we have problems on null-nodes
            deleteNullNode(doc.getDocumentElement());
            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
            return sw.toString();
        } catch (TransformerException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * workaround for android-problem on empty nodes. deleteNullNode
     * 
     * @param root
     */
    public static void deleteNullNode(Node root) {
        NodeList nl = root.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == Node.TEXT_NODE && nl.item(i).getNodeValue() == null) {
                nl.item(i).getParentNode().removeChild(nl.item(i));
            } else {
                deleteNullNode(nl.item(i));
            }
        }
    }

    public static final boolean isURI(String str) {
        return NetUtil.isURI(str);
    }

    /**
     * containsXml
     * 
     * @param text text to search
     * @return true, if text contains at least one xml tag
     */
    public static boolean containsXml(String text) {
        return text.matches(".*" + XML_TAG_START + ".*");
    }

    public static boolean containsHtml(String text) {
        return text.contains("<html");
    }

    public static String createMessagePage(String title, String msg) {
        return "<html><body background=icons/spe.jpg><div style=\"border: 2px solid; float: middle; text-align: center; color: red; font-weight: bold;\">"
            + title + "</div>" + createMessage(msg) + "</body></html>";
    }

    public static String createMessage(String msg) {
        return "<pre>" + msg + "</pre>";
    }

    public static boolean isHtml(String asString) {
        return asString != null && (asString.contains("</") || asString.contains("/>"));
    }

    public static String cdata(String data) {
        return "<![CDATA[" + data + "]]>";
    }

    public static String begin(String tagName) {
        return "<" + tagName + ">";
    }

    public static String end(String tagName) {
        return "</" + tagName + ">";
    }

    public static String percent(int value) {
        return value + "%";
    }

    public static String L(String content) {
        return "&" + content + ";";
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
        } else if (name.equals(ATTR_COLOR) || name.equals(ATTR_BGCOLOR) || name.equals(STYLE_BACKGROUND_COLOR)) {
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

    /**
     * delegates to {@link #TABLE(String, StringBuilder, String...)} with null content
     */
    public static final String[] TABLE(String tableTag, String... attrs) {
        return TABLE(tableTag, null, attrs);
    }

    /**
     * decides whether to use the old table tags or its div equivalent. tries to use given attributes as styles. a style
     * attribute can only be provided at the end!
     * 
     * @param tableTag
     * @param content the tags content as stringbuilder to avoid call parameter problems
     * @param styles additional style attributes
     * @return div class = tag + attrs
     */
    public static final String[] TABLE(String tableTag, StringBuilder content, String... attrs) {
        String cont = content != null ? content.toString() : null;
        if (ENV.get("layout.grid.oldtabletags", true)) {
            return CollectionUtil.concat(new String[] { tableTag, cont }, attrs);
        } else {
            if (attrs.length > 1 && ATTR_STYLE.equals(attrs[attrs.length - 2]))
                attrs[attrs.length - 2] = "nostyle";
            String styles = styles(attrs);
            return CollectionUtil.concat(new String[] { "div", cont },
                Util.isEmpty(styles) ? new String[] {} : new String[] { "style", styles },
                new String[] { "class", tableTag });
        }
    }

    public static String tableDivStyles() {
        if (tableDivStyle == null) {
            InputStream stream = ENV.getResource("style.template");
            tableDivStyle = StringUtil.removeFormatChars(String.valueOf(FileUtil.getFileData(stream, "UTF-8")));
        }
        return tableDivStyle;
    }

    public static Element createSidebarNavMenuButton(Element parent, Element sidenav) {
        // see style.template and websocket.client.js.template
        // <a href="javascript:void(0)" class="closebtn" onclick="closeNav()">&times;</a>
        //return "<span style=\"font-size:30px;cursor:pointer\" onclick=\"openNav()\">&#9776; open</span>"; //☰, &#8801; ≡
        //✖ = &#10006; or &times;
        appendElement(parent, TAG_SPAN, content("☰"), ATTR_CLASS, "openbtn", "onclick", "openNav()", ATTR_ACCESSKEY, "!");
        if (sidenav == null) {
            sidenav = appendElement(parent, TAG_DIV, ATTR_ID, "tslSidenav", ATTR_CLASS, "sidenav");
        } else { // the last parent is the most important one
            sidenav.getParentNode().removeChild(sidenav);
            parent.appendChild(sidenav);
        }
        appendElement(sidenav, TAG_LINK, content("✖"), ATTR_HREF, "javascript:void(0)", ATTR_ID, "button.sidenav.close", ATTR_CLASS, "closebtn", "onclick", "closeNav()", ATTR_ACCESSKEY, "<");
        return sidenav;
    }
    
    public static String cssID(String id) {
        return id.replace('.', CSS_ID_SEPARATOR);
    }
    public static String beanID(String id) {
        return id != null ? id.replace(CSS_ID_SEPARATOR, '.') : id;
    }

}
