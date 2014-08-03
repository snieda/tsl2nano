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

import java.text.MessageFormat;

import de.tsl2.nano.core.Environment;

/**
 * default implementation of {@link IStatus} for a status of type warning or error. if you want to create a ok-status
 * use {@link IStatus#STATUS_OK}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class Status implements IStatus {
    /** serialVersionUID */
    private static final long serialVersionUID = -7981328274166216957L;

    int status = OK;
    String msg = null;
    Throwable ex = null;

    /**
     * constructor
     */
    protected Status() {
        super();
    }

    /**
     * constructor
     * 
     * @param ex error description
     */
    public Status(Throwable ex) {
        super();
        this.ex = ex;
        this.msg = TAG[ERROR] + ": " + ex.toString();
        this.status = ERROR;
    }

    /**
     * creates a warning status
     * 
     * @param msg warning message
     */
    public Status(String msg) {
        super();
        this.msg = TAG[WARN] + ": " + msg;
        this.status = WARN;
    }

    /**
     * constructor
     * 
     * @param status {@link IStatus#OK}, {@link IStatus#WARN} or {@link IStatus#ERROR}
     * @param msg warning or error message
     * @param ex error exception
     */
    public Status(int status, String msg, Throwable ex) {
        super();
        this.status = status;
        this.msg = msg;
        this.ex = ex;
    }

    /**
     * usable to create an error status with an illegal argument description
     * 
     * @param name attribute name
     * @param value attribute value that involves the error
     * @param assertion an assertion that was not fulfilled by the value.
     * @return new error status instance
     */
    public static Status illegalArgument(String name, Object value, Object assertion) {
        return new Status(new IllegalArgumentException(Environment.translate("tsl2nano.assertion.failed", true,
            value,
            name,
            assertion)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean ok() {
        return status == OK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String message() {
        return msg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Throwable error() {
        return ex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return ok() ? TAG[OK] : msg;
    }
}
