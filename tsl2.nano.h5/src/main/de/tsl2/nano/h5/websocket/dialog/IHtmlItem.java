package de.tsl2.nano.h5.websocket.dialog;

import java.util.HashMap;
import java.util.Map;

public interface IHtmlItem {
    String getTag();
    String getId();
    String getName();
    Object getValue();
    Map<String, Object> getSpecificAttributes();
    
    default Map<String, String> getAttributes() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("id", getId());
        attrs.put("name", getName());
        attrs.put("value", String.valueOf(getValue()));
        getSpecificAttributes().keySet().stream().forEach(k -> attrs.put(k, String.valueOf(getSpecificAttributes().get(k))));
        return attrs;
    }
    
    default String toHtml() {
        return null;
    }
}
