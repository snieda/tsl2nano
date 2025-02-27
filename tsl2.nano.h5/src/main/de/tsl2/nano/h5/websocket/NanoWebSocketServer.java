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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.Attachment;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IValueDefinition;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.messaging.IListener;
import de.tsl2.nano.core.messaging.IStatefulListener;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.h5.NanoH5Session;
import de.tsl2.nano.h5.configuration.BeanConfigurator;
import de.tsl2.nano.h5.plugin.INanoPlugin;
import de.tsl2.nano.math.vector.Point;
import de.tsl2.nano.plugin.Plugins;

/**
 * Html5 WebSocket Server to provide a rich client gui interaction.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class NanoWebSocketServer extends WebSocketServer {
    private static final Log LOG = LogFactory.getLog(NanoWebSocketServer.class);

    /** parent session working on this websocket-server */
    ISession session;

    /**
     * if an attachment should be tranferred, this file name has to be sent previously to know where to store the file
     * to (inside the environments temp path). this name is volatile and will be reset on next message.
     */
    String attachment_info;

    /** holding a temp change object for each working object - to be injected into websocket listeners. */
    private Map<Class, Object> changeObjects;

    private boolean isConnected;

    public static final String PRE_TARGET = "/";
    public static final String PRE_ID = "@";
    public static final String PRE_POS = "?";
    public static final String PRE_VALUE = ":";

    public static final String TARGET_DEPENDENCY = "dependency";
    public static final String TARGET_INPUTASSIST = "inputassist";
    public static final String TARGET_ATTACHMENT = "attachment";
    public static final String TARGET_DIALOG = "dialog";

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

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose(WebSocket arg0, int arg1, String arg2, boolean arg3) {
        LOG.debug("closing websocket (reason: " + arg2 + "): " + arg0);
        attachment_info = null;
        changeObjects = null;
        arg0.send("websocket closed: reason=" + arg2);
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
        Plugins.process(INanoPlugin.class).handleWebSocketMessage(conn, msg);
        if (session instanceof NanoH5Session)
            ((NanoH5Session)session).assignSessionToCurrentThread(false, null);
        //if we are in configuration mode, do nothing
        if (session.getWorkingObject() != null) {
            Package pck = ((BeanDefinition) session.getWorkingObject()).getDeclaringClass().getPackage();
            if (pck.equals(BeanConfigurator.class.getPackage())) {
                return;
            }
        }
        //to be secure, no file is saved on wrong name
        attachment_info = null;

        // eval msg, find bean in navigation.current, call input assist
        String target = getTarget(msg);
        String id = getId(msg);
        String value = getValue(msg);
        Point pos = getPosition(msg);//mouse-click position
        BeanDefinition<?> beandef = getCurrentBean(id);
        String attr = StringUtil.substring(id, ".", null, true);
        if (beandef != null) {
            switch (target) {
            case TARGET_INPUTASSIST:
                ValueExpression ve =
                    beandef instanceof Bean ? beandef.getAttribute(attr).getValueExpression()
                        : beandef.getValueExpression();
                Collection<?> availableValues = ve.availableValues(value);
                conn.send(createMessage(target, id, availableValues));
                break;
            case TARGET_DEPENDENCY:
                if (beandef instanceof Bean) {
                    IValueDefinition attribute = ((Bean) beandef).getAttribute(attr);
                    WSEvent evt = new WSEvent(attribute, attribute.getValue(), value, (int) pos.x(), (int) pos.y());
                    //let all listeners work on the same 'change' object (a temp copy of the bean, holding all temp changes)
                    injectChangeObject(attribute);
                    //to take effect, use dependency listeners
                    attribute.changeHandler().fireEvent(evt);
                }
                break;
            case TARGET_ATTACHMENT:
                /*
                 * store temporarily the attachments file name to be saved later.
                 * attachments are handled through onMessage(WebSocket, ByteBuffer).
                 */
                attachment_info = msg;
                break;
            case TARGET_DIALOG:
                // ((WebSocketExceptionHandler) session.getExceptionHandler()).setResponseAndNotify(value);
                ConcurrentUtil.notifyWith(session, value);
                break;
            default:
                LOG.error("unexptected message msg: " + msg + ", target: " + target);
                throw new IllegalArgumentException("msg: " + msg + ", target: " + target);
            }
        }
    }

    /**
     * injects a temporary 'change' object, holding all changes to the current
     * parent bean of the given attribute. will only be done on first time - all
     * listeners will get the same change instance!
     * 
     * @param attribute to get the parent bean and the event handler from
     */
    private void injectChangeObject(IValueDefinition attribute) {
        Object changeObject = null;
        Collection<IListener> listeners = attribute.changeHandler().getListeners(WSEvent.class);
        IStatefulListener obs;
        boolean initializing = changeObjects == null;
        for (IListener l : listeners) {
            obs = (IStatefulListener) l;
            if (initializing || obs.getStateObject() == null) {
                changeObject = getChangeObject(attribute);
                obs.setStateObject(changeObject);
            }
        }
    }

    private Object getChangeObject(IValueDefinition attribute) {
        Object o;
        if (changeObjects == null)
            changeObjects = new HashMap<>();
        if ((o = changeObjects.get(attribute.getDeclaringClass())) == null) {
            o = BeanUtil.copy(attribute.getInstance());
            changeObjects.put(attribute.getDeclaringClass(), o);
        }
        return o;
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        super.onMessage(conn, message);
        Plugins.process(INanoPlugin.class).handleWebSocketMessage(conn, message);
        //save the received file blob to environment
//        String attachment_filename = session.getId() + "." + session.getWorkingObject();

        if (attachment_info == null) {
            throw new IllegalStateException(
                "'attachment_filename' is null but should have been sent by client '" + conn.getRemoteSocketAddress() + "' previously!");
        }

        String id = getId(attachment_info);
        String attrName = StringUtil.substring(id, ".", null, true);
        String name = getValue(attachment_info);
        Bean<?> bean = (Bean<?>) getCurrentBean(id);
        String fileName = Attachment.getFilename(bean.getInstance(), attrName, name);
        FileUtil.writeBytes(message.array(),
            fileName,
            false);
        conn.send("attachment '" + attachment_info + "' succefull transferred");
        attachment_info = null;
    }

    private BeanDefinition<?> getCurrentBean(String id) {
//        boolean isLoginPersistence = id.startsWith(Persistence.class.getSimpleName().toLowerCase());
//        BeanDefinition<?> currentBean;
//        if (isLoginPersistence) {
//            currentBean = Bean.getBean(Persistence.current());
//        } else {
        BeanDefinition<?> currentBean = (BeanDefinition<?>) session.getWorkingObject();
//        }
        return currentBean;
    }

    public static String getTarget(String message) {
        return StringUtil.substring(message, PRE_TARGET, PRE_ID);
    }

    public static String getId(String message) {
        return StringUtil.substring(message, PRE_ID, PRE_POS);
    }

    public static Point getPosition(String message) {
        String ps = StringUtil.substring(message, PRE_POS, PRE_VALUE);
        String xs = StringUtil.substring(ps, null, ",");
        String ys = StringUtil.substring(ps, ",", null);

        int x = -1, y = -1;
        try {
            x = Integer.valueOf(xs);
            y = Integer.valueOf(ys);
        } catch (NumberFormatException ex) {

        }
        return new Point(x, y);
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
        isConnected = true;
        arg0.send("OK (websocket connected)");
    }

    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public void onStart() {
        LOG.debug("starting websocket");
    }
    
    @Override
    public void stop() throws InterruptedException {
    	super.stop();
    	session = null;
    }

    @Override
    public Collection<WebSocket> getConnections() {
        try {
            return super.getConnections();
        } catch (Exception e) {
            LOG.error("websocket connection problem", e);
            return new LinkedList<>();
        }
    }
}
