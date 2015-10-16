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

import java.util.concurrent.TimeUnit;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.util.SchedulerUtil;

/**
 * Attribute dependency listener using websocket to refresh it's value on client-side. the real attribute value wont
 * change! Overwrite method {@link #evaluate(Object)}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class WebSocketServiceListener<T> extends WebSocketDependencyListener<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 5776202030295212325L;

    /** restful service url */
    @Element
    String restfulUrl;
    /** parameter name */
    @Element
    String parameter;
    /** optional timer in seconds to send refreshed data to the attribute presentation */
    @Element
    Long timer;

    /**
     * constructor
     */
    public WebSocketServiceListener() {
    }

    /**
     * constructor
     * 
     * @param attribute
     */
    public WebSocketServiceListener(AttributeDefinition<T> attribute) {
        super(attribute);
    }

    
    @Override
    public void handleEvent(WSEvent source) {
        sendValue(attributeID, propertyName, evaluate(source));
    }

    public static void sendValue(String attributeID, String propertyName, Object value) {
        String msg = NanoWebSocketServer.createMessage(NanoWebSocketServer.TARGET_DEPENDENCY, attributeID, value);
        Message.send(msg);
    }
    
    /**
     * creates a timer to request the given restful service periodically.
     * @param timer period in seconds
     */
    public void createTimer(Long timer) {
        this.timer = timer;
        SchedulerUtil.runAt(0, timer, Long.MAX_VALUE, TimeUnit.SECONDS, new Runnable() {
            @Override
            public void run() {
                handleEvent(null);
            }
        });
    }
    
    /**
     * evaluates a new value for it's attribute through a changed depending value.
     * 
     * @param value source value of another attribute
     * @return new value
     */
    protected T evaluate(WSEvent evt) {
        return (T)NetUtil.getRestful(restfulUrl, String.class, parameter, evt.newValue);
    }

    @Commit
    protected void initDeserialization() {
        if (timer != null) {
            createTimer(timer);
        }
    }
}
