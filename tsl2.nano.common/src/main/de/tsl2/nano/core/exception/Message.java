/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 26.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core.exception;

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.log.LogFactory;

/**
 * To handle a message - not to be thrown. See UncaughtExceptionHandler
 * 
 * @author Tom
 * @version $Revision$
 */
public class Message extends RuntimeException {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(Message.class);

    /**
     * constructor
     * 
     * @param message
     */
    public Message(String message) {
        super(message);
        LOG.info(message);
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return new StackTraceElement[0];
    }

    public static final void send(String message) {
//        send(Thread.getDefaultUncaughtExceptionHandler(), message);
        send(Environment.get(UncaughtExceptionHandler.class), message);
    }
    
    /**
     * sends the given message to the current uncaught exception handler
     * @param message
     */
    public static final void send(UncaughtExceptionHandler exceptionHandler, String message) {
        if (exceptionHandler != null)
            exceptionHandler.uncaughtException(Thread.currentThread(), new Message(message));
        else
            LOG.info(message);
    }
    
    @Override
    public String toString() {
        return getMessage();
    }
}
