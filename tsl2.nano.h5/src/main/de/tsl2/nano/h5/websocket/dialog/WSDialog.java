package de.tsl2.nano.h5.websocket.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.util.AdapterProxy;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.Html5Presentation;
import de.tsl2.nano.h5.HtmlUtil;

/** creates a simple html5 dialog to be sent through websockets */
public class WSDialog {
    private static final String TAG_ROOT = "dialog";
    public static final String PREFIX_DIALOG = Message.PREFIX_DIALOG;
    static final String PREFIX_NAME = "wsdialog.";

    String title;
    String message;
    List<WSField> fields;
    List<WSButton> buttons;

    public static String createHtmlFromBean(String title, Object beanInstance) {
        return createHtmlFromBean(title, beanInstance, TAG_ROOT);
    }

    @SuppressWarnings("unchecked")
    public static String createHtmlFromBean(String title, Object beanInstance, String rootTag) {
        // TODO: embed the title
        Bean<?> b = Bean.getBean(beanInstance);
        if (!(b.getPresentationHelper() instanceof Html5Presentation)) {
            throw new IllegalStateException("The presentationhelper of given bean must be an Html5Presentation!");
        }
        Element parentDlg = createFormDialog(rootTag);
        Element dlg =  ((Html5Presentation) b.getPresentationHelper()).createPage(AdapterProxy.create(ISession.class), parentDlg,
                b.getPresentable().getDescription(), true);
        return HtmlUtil.toString(dlg.getOwnerDocument(), true);
    }

    public static String createWSMessageFromBean(String title, Object beanInstance) {
        Class responseType = beanInstance != null ? beanInstance.getClass() : null;
        if ("null".equals(beanInstance)) 
            return new WSDialog(title, "", getOkButton()).toWSMessage();
        else if (PrimitiveUtil.isPrimitiveOrWrapper(responseType)) {
            if (PrimitiveUtil.isAssignableFrom(Boolean.class, responseType))
                return new WSDialog(title, "", getYesNoButtons()).toWSMessage();
            else
                return new WSDialog(title, "", getDefaultButtons()).addFields(new WSField("value", beanInstance, null)).toWSMessage();
        }
        return PREFIX_DIALOG + createHtmlFromBean(title, beanInstance);
    }

    public WSDialog(String title, String message, WSButton... buttons) {
        this.title = title;
        this.message = message;
        if (buttons != null && buttons.length > 0)
            addButtons(buttons);
        else
            this.buttons = new LinkedList<>();
    }

    public WSDialog addFields(WSField... fields) {
        getFields().addAll(Arrays.asList(fields));
        return this;
    }

    private List<WSField> getFields() {
        if (this.fields == null)
            this.fields = new ArrayList<>();
        return fields;
    }

    public WSDialog addButtons(WSButton... buttons) {
        if (this.buttons == null)
            this.buttons = new ArrayList<>(buttons.length);
        this.buttons.addAll(Arrays.asList(buttons));
        return this;
    }
    public String toHtmlDialog() {
        return toHtmlDialog(TAG_ROOT);
    }

    public String toHtmlDialog(String rootTag) {
        if (Util.isEmpty(buttons))
            addButtons(getDefaultButtons());

        Element dlg = createFormDialog(rootTag);
        HtmlUtil.appendElement(dlg, HtmlUtil.TAG_H3, HtmlUtil.content(title));
        HtmlUtil.appendElement(dlg, HtmlUtil.TAG_PARAGRAPH, HtmlUtil.content(message));
        for (WSField f : getFields()) {
            Element e = dlg;
            if (f.hasLabel()) {
                e = HtmlUtil.appendElement(dlg, HtmlUtil.TAG_DIV, HtmlUtil.content(Messages.getStringOpt(f.getName())));
            }
            HtmlUtil.appendElement(e, f.getTag(), HtmlUtil.content(f.getContent()), MapUtil.asArray(f.getAttributes(), String.class));
        }

        for (WSButton b : buttons) {
            HtmlUtil.appendElement(dlg, b.getTag(), HtmlUtil.content(b.getContent()), MapUtil.asArray(b.getAttributes(), String.class));
        }
        return HtmlUtil.toString(dlg.getOwnerDocument(), true);
    }

    private static Element createFormDialog(String rootTag) {
        Document doc = HtmlUtil.createDocument("");
        Element e = doc.createElement(rootTag);
        // e.setAttribute("open", "true");
        doc.appendChild(e);
        HtmlUtil.appendAttributes(e, HtmlUtil.ATTR_ID, PREFIX_NAME + "formDialog");
        return HtmlUtil.appendElement(e, HtmlUtil.TAG_FORM, HtmlUtil.ATTR_METHOD, TAG_ROOT);
    }

    public String toWSMessage() {
        return PREFIX_DIALOG + toHtmlDialog();
    }

    static WSButton[] getDefaultButtons() {
        return new WSButton[]{new WSButton("Ok"), new WSButton("Cancel")};
    }

    static WSButton[] getOkButton() {
        return new WSButton[]{new WSButton("Ok")};
    }

    static WSButton[] getYesNoButtons() {
        return new WSButton[]{new WSButton("Yes"), new WSButton("No")};
    }

}
