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
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;

/**
 * To handle a message - not to be thrown. See UncaughtExceptionHandler
 * 
 * @author Tom
 * @version $Revision$
 */
public class Message extends RuntimeException {
    /** (optional) if not a text has to be sent but an object. then the message text is BYTEBUFFER */
    ByteBuffer byteBuffer;

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(Message.class);

    private static final String BYTEBUFFER = "BYTEBUFFER";

    protected Message() {
	}
    
    /**
     * constructor
     * 
     * @param message
     */
    public Message(String message) {
        super(message);
        LOG.trace("creating message: " + message);
    }

    /**
     * constructor
     * 
     * @param byteBuffer
     */
    public Message(ByteBuffer byteBuffer) {
        super(BYTEBUFFER);
        this.byteBuffer = byteBuffer;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return new StackTraceElement[0];
    }

    public static final void send(Throwable msgHolder) {
        //the @ is a prefix to be shown as message on the client
        send("@" + ManagedException.toRuntimeEx(msgHolder, false, false).getMessage());
    }
    
    public static final void send(String message) {
        //TODO: why the current thread doesn't have the right handler? using
        //      the environment will create problems on multi-sessions!
        send(ENV.get(UncaughtExceptionHandler.class), message);
        
        send(Thread.currentThread().getUncaughtExceptionHandler(), message);
    }

    public static final void send(ByteBuffer message) {
        //TODO: why the current thread doesn't have the right handler? using
        //      the environment will create problems on multi-sessions!
        send(ENV.get(UncaughtExceptionHandler.class), message);
        
        send(Thread.currentThread().getUncaughtExceptionHandler(), message);
    }

    /**
     * sends the given message to the current uncaught exception handler
     * 
     * @param message
     */
    public static final void send(UncaughtExceptionHandler exceptionHandler, Object message) {
        if (exceptionHandler != null) {
            if (!(message instanceof Throwable))
                LOG.info(message);
            exceptionHandler.uncaughtException(Thread.currentThread(), message instanceof ByteBuffer ? new Message(
                (ByteBuffer) message) : new Message(String.valueOf(message)));
        } else {
            LOG.info(message);
        }
    }

    @Override
    public String toString() {
        return getMessage();
    }

}