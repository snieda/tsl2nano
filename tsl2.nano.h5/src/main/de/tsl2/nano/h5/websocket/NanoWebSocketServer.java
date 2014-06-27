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

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import de.tsl2.nano.core.log.LogFactory;

/**
 * Html5 WebSocket Server
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class NanoWebSocketServer extends WebSocketServer {
    private static final Log LOG = LogFactory.getLog(NanoWebSocketServer.class);
    /**
     * constructor
     * 
     * @throws UnknownHostException
     */
    public NanoWebSocketServer() throws UnknownHostException {
    }

    /**
     * constructor
     * 
     * @param address
     */
    public NanoWebSocketServer(InetSocketAddress address) {
        super(address);
        LOG.info("websocket-server created: " + address);
    }

    /**
     * constructor
     * 
     * @param address
     * @param decoders
     */
    public NanoWebSocketServer(InetSocketAddress address, int decoders) {
        super(address, decoders);
        LOG.info("websocket-server created: " + address);
    }

    /**
     * constructor
     * 
     * @param address
     * @param drafts
     */
    public NanoWebSocketServer(InetSocketAddress address, List<Draft> drafts) {
        super(address, drafts);
    }

    /**
     * constructor
     * 
     * @param arg0
     * @param arg1
     * @param arg2
     */
    public NanoWebSocketServer(InetSocketAddress arg0, int arg1, List<Draft> arg2) {
        super(arg0, arg1, arg2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose(WebSocket arg0, int arg1, String arg2, boolean arg3) {
        LOG.debug("closing websocket (reason: " + arg2 + "): " + arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onError(WebSocket arg0, Exception arg1) {
        LOG.error(arg1);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(WebSocket conn, String msg) {
        LOG.debug("sending message: " + msg);
        conn.send(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen(WebSocket arg0, ClientHandshake arg1) {
        LOG.debug("opening websocket: " + arg0);
    }
}
