package de.tsl2.nano.h5.websocket.dialog;

import static de.tsl2.nano.h5.HtmlUtil.ATTR_TYPE;

import java.util.HashMap;
import java.util.Map;

import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.util.MapUtil;

public class WSButton extends WSItem {
    private static final String TAG = "button";
    /** the client side socket script will add click listeners on this id */
    private static final String DEFAULT_ID = WSDialog.PREFIX_NAME + TAG;

    public static final WSButton CLOSE = new WSButton("Close", null);
    public static final WSButton SUBMIT = new WSButton("Submit", null, ATTR_TYPE, "submit");

    public WSButton(String name) {
        this(name, name, new HashMap<>());
    }
    public WSButton(String name, Object value, String...attrKeyValues) {
        this(name, value, MapUtil.asMap(attrKeyValues));
    }

    public WSButton(String name, Object value, Map<String, Object> attributes) {
        super(TAG, name, value, attributes);
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
