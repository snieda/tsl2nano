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
import java.nio.ByteBuffer;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.IValueDefinition;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.persistence.Persistence;

/**
 * Html5 WebSocket Server to provide a rich client gui interaction.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class NanoWebSocketServer extends WebSocketServer {
    private static final Log LOG = LogFactory.getLog(NanoWebSocketServer.class);

    /** parent session working on this websocket-server */
    ISession session;

    /**
     * if an attachment should be tranferred, this file name has to be sent previously to know where to store the file
     * to (inside the environments temp path). this name is volatile and will be reset on next message.
     */
    String attachment_info;

    public static final String PRE_TARGET = "/";
    public static final String PRE_ID = "@";
    public static final String PRE_VALUE = ":";

    public static final String TARGET_DEPENDENCY = "dependency";
    public static final String TARGET_INPUTASSIST = "inputassist";
    public static final String TARGET_ATTACHMENT = "attachment";

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
     * @param session
     * 
     * @param address
     */
    public NanoWebSocketServer(ISession session, InetSocketAddress address) {
        super(address);
        this.session = session;
        LOG.info("websocket-server created: " + address);
    }

//    /**
//     * constructor
//     * 
//     * @param address
//     * @param decoders
//     */
//    public NanoWebSocketServer(InetSocketAddress address, int decoders) {
//        super(address, decoders);
//        
//        LOG.info("websocket-server created: " + address);
//    }
//
//    /**
//     * constructor
//     * 
//     * @param address
//     * @param drafts
//     */
//    public NanoWebSocketServer(InetSocketAddress address, List<Draft> drafts) {
//        super(address, drafts);
//    }
//
//    /**
//     * constructor
//     * 
//     * @param arg0
//     * @param arg1
//     * @param arg2
//     */
//    public NanoWebSocketServer(InetSocketAddress arg0, int arg1, List<Draft> arg2) {
//        super(arg0, arg1, arg2);
//    }
//
    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose(WebSocket arg0, int arg1, String arg2, boolean arg3) {
        LOG.debug("closing websocket (reason: " + arg2 + "): " + arg0);
        arg0.send("websocket closed");
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
        LOG.debug("receiving message: '" + msg + "' from " + conn);
        //to be secure, no file is saved on wrong name
        attachment_info = null;

        // eval msg, find bean in navigation.current, call input assist
        String target = getTarget(msg);
        String id = getId(msg);
        String value = getValue(msg);
        Bean<?> bean = getCurrentBean(id);
        String attr = StringUtil.substring(id, ".", null, true);
        IValueDefinition attribute = bean.getAttribute(attr);
        if (bean != null) {
            switch (target) {
            case TARGET_INPUTASSIST:
                Collection<?> availableValues = attribute.getPresentation().getInputAssist().availableValues(value);
                conn.send(createMessage(target, id, availableValues));
                break;
            case TARGET_DEPENDENCY:
                //to take effect, use dependency listners
                attribute.changeHandler().fireEvent(value);
                break;
            case TARGET_ATTACHMENT:
                /*
                 * store temporarily the attachments file name to be saved later.
                 * attachments are handled through onMessage(WebSocket, ByteBuffer).
                 */
                attachment_info = msg;
                break;
            default:
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        super.onMessage(conn, message);

        //save the received file blob to environment
//        String attachment_filename = session.getId() + "." + session.getWorkingObject();

        if (attachment_info == null)
            throw new IllegalStateException("'attachment_filename' is null but should be sent by client previously!");

        String id = getId(attachment_info);
        String attrName = StringUtil.substring(id, ".", null, true);
        String name = getValue(attachment_info);
        String fileName = getAttachmentFilename(getCurrentBean(id).getInstance(), attrName, name);
        FileUtil.writeBytes(message.array(),
            fileName,
            false);
        conn.send("attachment '" + attachment_info + "' succefull transferred");
        attachment_info = null;
    }

    /**
     * getAttachmentFilename
     * 
     * @param instance
     * @param attribute
     * @param value
     * @return
     */
    public static String getAttachmentFilename(Object instance, String attribute, String value) {
        return Environment.getTempPath()
            + FileUtil.getValidFileName(BeanValue.getBeanValue(instance, attribute).getValueId() + "."
                + Util.asString(value));
    }

    private Bean<?> getCurrentBean(String id) {
        boolean isLoginPersistence = id.startsWith(Persistence.class.getSimpleName().toLowerCase());
        Bean<?> currentBean;
        if (isLoginPersistence) {
            currentBean = Bean.getBean(Persistence.current());
        } else {
            currentBean = (Bean<?>) session.getWorkingObject();
        }
        return currentBean;
    }

    public static String getTarget(String message) {
        return StringUtil.substring(message, PRE_TARGET, PRE_ID);
    }

    public static String getId(String message) {
        return StringUtil.substring(message, PRE_ID, PRE_VALUE);
    }

    public static String getValue(String message) {
        return StringUtil.substring(message, PRE_VALUE, null);
    }

    public static String createMessage(String target, String id, Object value) {
        return PRE_TARGET + target + PRE_ID + id + PRE_VALUE + String.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen(WebSocket arg0, ClientHandshake arg1) {
        LOG.debug("opening websocket: " + arg0);
        arg0.send("websocket connected");
    }
}
