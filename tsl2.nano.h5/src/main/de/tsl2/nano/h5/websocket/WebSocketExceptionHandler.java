/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 03.06.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.websocket;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.java_websocket.WebSocket;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.exception.ExceptionHandler;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.h5.BeanModifier;
import de.tsl2.nano.h5.NanoH5Session;
import de.tsl2.nano.h5.websocket.dialog.WSDialog;

/**
 * ExceptionHandler sending all exceptions/message through the given websocket
 * connection.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class WebSocketExceptionHandler extends ExceptionHandler implements Closeable {
    private static final Log LOG = LogFactory.getLog(WebSocketExceptionHandler.class);

    NanoWebSocketServer socket;

    /**
     * constructor
     */
    public WebSocketExceptionHandler(NanoWebSocketServer socket) {
        this(true);
        this.socket = socket;
    }

    /**
     * constructor
     * 
     * @param concurrent
     */
    public WebSocketExceptionHandler(boolean concurrent) {
        super(concurrent);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            Object obj = null;
            boolean waitForResponse = false;
            String msg = e.getMessage();
            if (msg != null && msg.startsWith(NanoH5Session.PREFIX_STATUS_LINE)) {
                super.uncaughtException(t, e);
            } else if (msg != null && msg.contains(Message.PREFIX_DIALOG)) {
                // not very performant to serialize+hex and deserializ+unhex the object, but we
                // can work on pojos!
                String title = StringUtil.substring(msg, null, "@");
                String msg0 = StringUtil.substring(msg, "@", null);
                String data = StringUtil.substring(msg0, Message.PREFIX_DIALOG, null);
                if (StringUtil.isHexString(data))
                    msg = WSDialog.createWSMessageFromBean(title, obj = Message.obj(msg0));
                LOG.info("\n==> sending dialog to websockets:\n\ttitle: " + title + "\n\tmsg  : " + msg0 + "\n\tdata : "
                        + data + "\n\tdialog: " + msg);
                waitForResponse = true;
            }

            Collection<WebSocket> connections = socket.getConnections();
            for (WebSocket webSocket : connections) {
                webSocket.send(msg != null ? msg : e.toString());
            }

            if (waitForResponse) {
                final Object obj0 = obj;
                ConcurrentUtil.waitOn(socket.session, 4000, r -> convertAndProvide(obj0, r));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            ManagedException.forward(ex);
        }
    }

    //TODO: untested yet!
    private void convertAndProvide(Object obj, Object response) {
        Object value = response;
        if (response instanceof String && ((String)response).matches("[.*{].*[:].*[,].*[}].*")) {
            String json = StringUtil.substring((String)response, "{", "}");
            Map parms =  MapUtil.fromJSON(json);
            if ((obj == null || PrimitiveUtil.isPrimitiveOrWrapper(obj.getClass())) && parms.size() == 1)
                value = parms.values().iterator().next();
            else {
                new BeanModifier().refreshValues(Bean.getBean(obj), parms);
                value = obj;
            }
        }
        ConcurrentUtil.setCurrent(Message.createResponse(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        try {
            socket.stop();
            socket = null;
        } catch (InterruptedException e) {
            ManagedException.forward(e);
        }
    }

}
