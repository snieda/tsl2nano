/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Feb 17, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;

import de.tsl2.nano.action.IStatus;

/**
 * extends {@link Status} to be xml-serializable
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class SStatus extends Status implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 4580098695830610902L;

    /**
     * creates an ok-Status, see {@link IStatus#OK}
     */
    public SStatus() {
    }
    
    /**
     * constructor
     * 
     * @param ex error description
     */
    public SStatus(Throwable ex) {
        super(ex);
    }

    /**
     * creates a warning status
     * 
     * @param msg warning message
     */
    public SStatus(String msg) {
        super(msg);
    }

    /**
     * constructor
     * 
     * @param status {@link IStatus#OK}, {@link IStatus#WARN} or {@link IStatus#ERROR}
     * @param msg warning or error message
     * @param ex error exception
     */
    public SStatus(int status, String msg, Throwable ex) {
        super(status, msg, ex);
    }

    /**
     * only to be called by framework or deserialization
     * @return Returns the status.
     */
    int getStatus() {
        return status;
    }

    /**
     * only to be called by framework or deserialization
     * @return Returns the msg.
     */
    String getMsg() {
        return msg;
    }

    /**
     * only to be called by framework or deserialization
     * @return Returns the ex.
     */
    Throwable getEx() {
        return ex;
    }

    /**
     * only to be called by framework or deserialization
     * @param status The status to set.
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * only to be called by framework or deserialization
     * @param msg The msg to set.
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * only to be called by framework or deserialization
     * @param ex The ex to set.
     */
    public void setEx(Throwable ex) {
        this.ex = ex;
    }
}
