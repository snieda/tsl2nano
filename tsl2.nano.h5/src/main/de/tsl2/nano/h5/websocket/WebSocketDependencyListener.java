/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 09.07.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.websocket;

import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.AbstractDependencyListener;
import de.tsl2.nano.core.exception.Message;

/**
 * Attribute dependency listener using websocket to refresh it's value on client-side. the real attribute value wont
 * change! Overwrite method {@link #evaluate(Object)}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public abstract class WebSocketDependencyListener<T> extends AbstractDependencyListener<T> {
    /**
     * constructor
     */
    public WebSocketDependencyListener() {
    }

    /**
     * constructor
     * 
     * @param attribute
     */
    public WebSocketDependencyListener(AttributeDefinition<T> attribute) {
        super(attribute);
    }

    @Override
    public void handleEvent(Object source) {
        T value = evaluate(source);
        String msg = NanoWebSocketServer.createMessage(NanoWebSocketServer.TARGET_DEPENDENCY, attributeID, value);
        Message.send(msg);
    }

    /**
     * evaluates a new value for it's attribute through a changed depending value.
     * 
     * @param value source value of another attribute
     * @return new value
     */
    abstract protected T evaluate(Object value);
}
