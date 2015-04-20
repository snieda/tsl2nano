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

import java.net.URI;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import de.tsl2.nano.core.log.LogFactory;

/**
 * Html5 WebSocket Client. This implementation does only some logging.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class NanoWebSocketClient extends WebSocketClient {
    private static final Log LOG = LogFactory.getLog(NanoWebSocketClient.class);

    /**
     * constructor
     * 
     * @param serverURI
     */
    public NanoWebSocketClient(URI serverURI) {
        super(serverURI);
    }

    /**
     * constructor
     * 
     * @param serverUri
     * @param draft
     */
    public NanoWebSocketClient(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    /**
     * constructor
     * 
     * @param serverUri
     * @param draft
     * @param headers
     */
    public NanoWebSocketClient(URI serverUri, Draft draft, Map<String, String> headers) {
        super(serverUri, draft, headers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose(int arg0, String arg1, boolean arg2) {
        LOG.debug("closing websocket (reason: " + arg2 + "): " + arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onError(Exception arg0) {
        LOG.error(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(String msg) {
        send(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen(ServerHandshake arg0) {
        LOG.debug("opening websocket: " + arg0);
    }
}
