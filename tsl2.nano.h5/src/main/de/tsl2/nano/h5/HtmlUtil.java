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

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.tsl2.nano.exception.ForwardedException;

/**
 * defines html tag- and attribute-names und helper methods. android doesn't support the full w3c implementation like
 * HtmlElement etc.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class HtmlUtil {
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
    
    public static final String TAG_INPUT = "input";
    public static final String ATTR_TYPE = "type";
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

    public static final String TAG_SELECT = "select";
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
    
    public static final String TAG_BUTTON = "button";
    
    /* 
     * Layout
     */
    public static final String TAG_BREAK = "br";
    public static final String TAG_PARAGRAPH = "p";
    public static final String ATTR_ALIGN = "align";
    public static final String ALIGN_CENTER = "center";
    public static final String ALIGN_RIGHT = "end";
    public static final String TAG_PRE = "pre";

    public static final String TAG_SPAN = "span";
    public static final String TAG_FONT = "font";
    public static final String ATTR_COLOR = "color";

    public static final String TAG_TABLE = "table";
    public static final String TAG_TBODY = "tbody";
    public static final String TAG_COLGROUP = "colgroup";
    public static final String TAG_COL = "col";
    public static final String TAG_ROW = "tr";
    public static final String TAG_HEADERCELL = "th";
    public static final String TAG_CELL = "td";
    public static final String ATTR_FRAME = "frame";
    public static final String ATTR_BORDER = "border";
    public static final String ATTR_WIDTH = "width";
    public static final String ATTR_BGCOLOR = "bgcolor";
    public static final String ATTR_SPAN = "span";

    public static final String COLOR_WHITE = "#FFFFFF";
    public static final String COLOR_BLACK = "#000000";
    public static final String COLOR_RED = "#FF0000";
    public static final String COLOR_GREEN = "#00FF00";
    public static final String COLOR_BLUE = "#0000FF";
    public static final String COLOR_GRAY = "#999999";
    public static final String COLOR_LIGHT_RED = "#FFCCCC";
    public static final String COLOR_LIGHT_GREEN = "#CCFFCC";
    public static final String COLOR_LIGHT_BLUE = "#CCCCFF";
    public static final String COLOR_LIGHT_GRAY = "#CCCCCC";

    public static final String VAL_100PERCENT = "100%";
    public static final String VAL_FALSE = Boolean.FALSE.toString();
    public static final String VAL_TRUE = Boolean.TRUE.toString();
    public static final String VAL_ALIGN_LEFT = "start";
    public static final String VAL_ALIGN_CENTER = "middle";
    public static final String VAL_ALIGN_RIGHT = "end";

    public static final String BTN_ASSIGN = "swartifex.assign";
    public static final String BTN_SUBMIT = "swartifex.save";
    public static final String BTN_CANCEL = "swartifex.cancel";
    
    public static final String BTN_SELECT_ALL = "swartifex.selectall";
    public static final String BTN_DESELECT_ALL = "swartifex.deselectall";
    public static final String BTN_PRINT = "swartifex.print";
    public static final String BTN_EXPORT = "swartifex.export";

    public static final String XML_TAG_START = "\\<.*\\>";
    
    public static Element appendElements(Element parent, String... tagNames) {
        Document doc = parent.getOwnerDocument();
        for (int i = 0; i < tagNames.length; i++) {
            parent.appendChild(doc.createElement(tagNames[i]));
        }
        return parent;
    }

    public static Element appendElement(Element parent, String tagName, String... attributes) {
        return appendElement(parent, tagName, null, attributes);
    }

    public static Element appendElement(Element parent, String tagName, StringBuilder content, String... attributes) {
        Document doc = parent.getOwnerDocument();
        Element e = doc.createElement(tagName);
        if (content != null)
            e.setTextContent(content.toString());
        for (int i = 0; i < attributes.length; i += 2) {
            //disabled flag attribute --> continue
            if (attributes[i] == null)
                continue;
            Attr attr = doc.createAttribute(attributes[i]);
            if (i < attributes.length - 1)
                attr.setValue(attributes[i + 1]);
            e.setAttributeNode(attr);
        }
        parent.appendChild(e);
        return e;
    }

    public static Element appendLink(Element parent, String name, String href) {
        return appendElement(parent, TAG_LINK, new StringBuilder(name), ATTR_HREF, href);
    }

    /**
     * usable to add an element attribute of type boolean. e.g.: formnovalidate='formnovalidate' or 'false'.
     * @param name boolean attribute name
     * @param enable whether to activate the attribute
     * @return attribute name and value
     */
    public static final String enable(String name, boolean enable) {
        return /*"name = " +*/ (enable ? name : null);
    }
    
    public static String toString(Document doc) {
        //set up a transformer
        try {
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            //create string from xml tree
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
            return sw.toString();
        } catch (TransformerException e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    /**
     * containsXml
     * @param text text to search
     * @return true, if text contains at least one xml tag
     */
    public static boolean containsXml(String text) {
        return text.matches(".*" + XML_TAG_START + ".*");
    }
}
