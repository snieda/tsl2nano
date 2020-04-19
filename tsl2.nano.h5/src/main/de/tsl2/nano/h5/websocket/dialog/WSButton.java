package de.tsl2.nano.h5.websocket.dialog;

import java.util.HashMap;
import java.util.Map;

import de.tsl2.nano.core.Messages;

public class WSButton extends WSItem {
    private static final String TAG = "button";
    /** the client side socket script will add click listeners on this id */
    private static final String DEFAULT_ID = WSDialog.PREFIX_NAME + TAG;
    public WSButton(String name) {
        this(name, name, new HashMap<>());
    }
    public WSButton(String name, Object value, Map<String, Object> attributes) {
        super(TAG, name, value, attributes);
    }
    @Override
    public String getId() {
        return DEFAULT_ID;
    }

    @Override
    public boolean hasLabel() {
        return false;
    }

    @Override
    public String getContent() {
        return Messages.getStringOpt(getName());
    }
}
