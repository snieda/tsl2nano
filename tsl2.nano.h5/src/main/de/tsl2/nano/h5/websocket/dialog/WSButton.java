package de.tsl2.nano.h5.websocket.dialog;

import java.util.HashMap;
import java.util.Map;

public class WSButton extends WSItem {
    public WSButton(String name) {
        this(name, name, new HashMap<>());
    }
    public WSButton(String name, Object value, Map<String, Object> attributes) {
        super("button", name, value, attributes);
    }
}
