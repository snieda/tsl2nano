package de.tsl2.nano.h5.websocket.dialog;

import java.util.HashMap;
import java.util.Map;

import de.tsl2.nano.core.util.MapUtil;

public class WSItem implements IHtmlItem {

    private String tag;
    private String name;
    private Object value;
    private Map<String, Object> attributes;

    public WSItem(String tag, String name, Object value, Object...attrKeyValues) {
        this(tag, name, value, MapUtil.asMap(attrKeyValues));
    }

    public WSItem(String tag, String name, Object value, Map<String, Object> attributes) {
        this.tag = tag;
        this.name = name;
        this.value = value;
        this.attributes = attributes;

    }
    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public Map<String, Object> getSpecificAttributes() {
        if (attributes == null)
            attributes = new HashMap<>();
        return attributes;
    }

    @Override
    public String getId() {
        return WSDialog.PREFIX_NAME + getTag() + "." + getName() + ".id";
    }

}
