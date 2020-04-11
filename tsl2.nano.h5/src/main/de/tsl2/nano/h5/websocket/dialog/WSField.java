package de.tsl2.nano.h5.websocket.dialog;

import java.util.HashMap;
import java.util.Map;

public class WSField extends WSItem {
    public WSField(String name) {
        this(name, null, new HashMap<>());
    }
    public WSField(String name, Object value, Map<String, Object> attributes) {
        super("input", name, value, attributes);
    }

}
