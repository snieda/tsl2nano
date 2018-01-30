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

import org.java_websocket.WebSocket;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.exception.ExceptionHandler;
import de.tsl2.nano.h5.NanoH5Session;

/**
 * ExceptionHandler sending all exceptions/message through the given websocket connection.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class WebSocketExceptionHandler extends ExceptionHandler implements Closeable{
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
        String msg = e.getMessage(); 
        if (msg != null && msg.startsWith(NanoH5Session.PREFIX_STATUS_LINE)) {
            super.uncaughtException(t, e);
        }
//        else {
            Collection<WebSocket> connections = socket.connections();
            for (WebSocket webSocket : connections) {
                webSocket.send(msg != null ? msg : e.toString());
            }
//        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        try {
            socket.stop();
        } catch (InterruptedException e) {
            ManagedException.forward(e);
        }
    }
}
