package de.tsl2.nano.h5.websocket.dialog;

import static de.tsl2.nano.h5.HtmlUtil.TAG_INPUT;
import static de.tsl2.nano.h5.HtmlUtil.TAG_TEXTAREA;

import java.util.HashMap;
import java.util.Map;

import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.Html5Presentation;
import de.tsl2.nano.h5.HtmlUtil;

public class WSField extends WSItem {
    public WSField(String name) {
        this(name, null, new HashMap<>());
    }
    public WSField(String name, Object value, Map<String, Object> attributes) {
        this(name, value, false, attributes);
    }
    public WSField(String name, Object value, boolean multiline, Object...attrKeyValues) {
        this(name, value, multiline, MapUtil.asMap(attrKeyValues));
    }

    public WSField(String name, Object value, boolean multiline, Map<String, Object> attributes) {
        super(multiline ? TAG_TEXTAREA : TAG_INPUT, name, value, attributes);
        if (Util.isEmpty(attributes)) {
            addBeanPresentationHelperAttributes(value);
        }
    }

    @Override
    public String getContent() {
        return isMultiline() ? (String) super.getValue() : super.getContent();
    }
    private boolean isMultiline() {
        return getTag().equals(TAG_TEXTAREA);
    }
    @Override
    public Object getValue() {
        return isMultiline() ? "" : super.getValue();
    }
    private void addBeanPresentationHelperAttributes(Object value) {
        String inputType = Html5Presentation.getType(value.getClass(), BeanPresentationHelper.getDefaultType(value.getClass()), false);
        getSpecificAttributes().put(HtmlUtil.ATTR_TYPE_INPUT, inputType);
    }

}
