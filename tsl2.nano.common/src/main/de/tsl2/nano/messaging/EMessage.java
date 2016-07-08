/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 03.07.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.messaging;

import java.util.EventObject;

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

}
