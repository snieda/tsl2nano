package de.tsl2.nano.h5.websocket.dialog;

import java.util.HashMap;
import java.util.Map;

import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.Html5Presentation;
import de.tsl2.nano.h5.HtmlUtil;

public class WSField extends WSItem {
    public WSField(String name) {
        this(name, null, new HashMap<>());
    }
    public WSField(String name, Object value, Map<String, Object> attributes) {
        super("input", name, value, attributes);
        if (Util.isEmpty(attributes)) {
            addBeanPresentationHelperAttributes(value);
        }
    }

    private void addBeanPresentationHelperAttributes(Object value) {
        String inputType = Html5Presentation.getType(value.getClass(), BeanPresentationHelper.getDefaultType(value.getClass()), false);
        getSpecificAttributes().put(HtmlUtil.ATTR_TYPE_INPUT, inputType);
    }

}
