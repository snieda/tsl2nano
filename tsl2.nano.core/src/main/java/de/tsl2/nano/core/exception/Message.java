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

import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * To handle a message - not to be thrown. See UncaughtExceptionHandler. can be handled anywhere in the own thread.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Message extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(Message.class);

    public static final String PREFIX_DIALOG = "/dialog:";

    protected Message() {
    }

    public Message(String message) {
        super(message);
        LOG.trace("creating message: " + message);
    }

    public Message(ByteBuffer byteBuffer) {
        super(StringUtil.toHexString(byteBuffer.array()));
        LOG.trace("creating message from bytebuffer ");
    }

    public Message(Serializable beanInstance) {
        super(hex(beanInstance));
        LOG.trace("creating message from serialize object ");
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return new StackTraceElement[0];
    }

    public static final void send(Throwable msgHolder) {
        // the @ is a prefix to be shown as message on the client
        send("@" + ManagedException.toRuntimeEx(msgHolder, false, false).getMessage());
    }

    public static final void send(String message) {
        // TODO: why the current thread doesn't have the right handler? using
        // the environment will create problems on multi-sessions!
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
            exceptionHandler.uncaughtException(Thread.currentThread(), message instanceof ByteBuffer 
                ? new Message((ByteBuffer) message) : new Message(String.valueOf(message)));
        } else {
            LOG.info(message);
        }
    }

    public static final <T> T ask(String message, T askInstance) {
        T result = sendAndWaitForResponse(message + "@" + hex(askInstance), (Class<T>)askInstance.getClass());
        return result != null ? result : askInstance;
    }

    public static final <T> T sendAndWaitForResponse(String message, Class<T> responseType) {
        // on dialog message, the exceptionhandler has to wait for a response in another thread
        send(message);
        Response response = ConcurrentUtil.getCurrent(Response.class);
        ConcurrentUtil.removeCurrent(Response.class);
        T value = response != null ? (T)response.value : null;
        if (value instanceof String && PrimitiveUtil.isPrimitiveOrWrapper(responseType))
            value = (T) FormatUtil.parse(responseType, (String)value);
        LOG.info("message response: " + response + " ==> converting to (" + responseType + ") " + value);
        return value;
    }

	public static Response createResponse(Object value) {
		return new Response(value);
    }
    
    public static Object obj(String hex) {
        byte[] obj = ByteUtil.fromHex(hex.substring(Message.PREFIX_DIALOG.length()));
        return ObjectUtil.convertToObject(obj);
    }
    
    public static <T> String hex(T askInstance) {
        return PREFIX_DIALOG + StringUtil.toHexString(ObjectUtil.serialize(askInstance));
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
/** used for asking messages */
class Response {
    Object value;

    Response(Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        return value != null && value.equals(((Response) obj).value)
                || value == null && ((Response) obj).value == null;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}