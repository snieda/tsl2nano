/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 03.07.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.core.messaging;

import java.util.EventObject;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.Main;

/**
 * sends a message from source to destination
 * @author Tom
 * @version $Revision$ 
 */
public class EMessage<T> extends EventObject {
    private static final long serialVersionUID = -6344841433143131005L;
    
    T msg;
    String destPath;
    /**
     * constructor
     */
    public EMessage(Object src, T msg, String destPath) {
        super(src);
        this.msg = msg;
        this.destPath = destPath;
    }
    /**
     * @return Returns the msg.
     */
    public T getMsg() {
        return msg;
    }
    /**
     * @return Returns the destPath.
     */
    public String getDestPath() {
        return destPath;
    }

    /**
     * sends the given message to the main application - this may send the message to all sessions. 
     * @param src sender
     * @param message the message
     * @param path to evaluate the destinations
     */
    public static void broadcast(Object src, Object message, String path) {
        ENV.get(Main.class).getEventController().fireEvent(new EMessage(src, "/broadcast:" + message, path));
    }

}
