package de.tsl2.nano.h5.websocket.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.h5.HtmlUtil;

/**creates a simple html5 dialog to be sent through websockets */
public class WSDialog {
    String title;
    String message;
    List<WSField> fields;
    List<WSButton> buttons;

    public WSDialog(String title, String message, WSButton...buttons) {
        this.title = title;
        this.message = message;
        if (buttons != null && buttons.length > 0)
            addButtons(buttons);
    }
    public WSDialog addFields(WSField...fields) {
        if (this.fields == null)
            this.fields = new ArrayList<>(fields.length);
        this.fields.addAll(Arrays.asList(fields));
        return this;
    }

    public WSDialog addButtons(WSButton...buttons) {
        if (this.buttons == null)
            this.buttons = new ArrayList<>(buttons.length);
        this.buttons.addAll(Arrays.asList(buttons));
        return this;
    }

    public String toHtmlDialog() {
        Document doc = HtmlUtil.createDocument("");
        Element e = doc.createElement("dialog");
        doc.appendChild(e);
        HtmlUtil.appendAttributes(e, HtmlUtil.ATTR_ID, "formDialog");
        e = HtmlUtil.appendElement(e, HtmlUtil.TAG_FORM, HtmlUtil.ATTR_METHOD, "dialog");
        HtmlUtil.appendElement(e, HtmlUtil.TAG_H2, HtmlUtil.content(title));
        HtmlUtil.appendElement(e, HtmlUtil.TAG_PARAGRAPH, HtmlUtil.content(message));
        for (WSField f : fields) {
            HtmlUtil.appendElement(e, f.getTag(), MapUtil.asArray(f.getAttributes(), String.class));
        }
        for (WSButton b : buttons) {
            HtmlUtil.appendElement(e, b.getTag(), MapUtil.asArray(b.getAttributes(), String.class));
        }
        return HtmlUtil.toString(doc, true);
    }

    public String toWSMessage() {
        return "/dialog:" + toHtmlDialog();
    }
}
