/*
 * Copyright © 2002-2009 Thomas Schneider
 * Schwanthaler Strasse 69, 80336 München. Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.exception;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import org.apache.commons.logging.Log;

import de.tsl2.nano.Messages;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.Util;

/**
 * For unchecked Exception handling. If you catch an exception and want to forward this exception in a runtime exception
 * - use this class. The message of this exception will show the cause message and can use resource bundles.
 * 
 * Use {@link #getLocalizedMessage()} to show the message. This will use resource bundles.
 * 
 * @author TS 28.01.2009
 * @version $Revision$
 */
public class ForwardedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private static final String MESSAGE_FORWARDED = "tsl2nano.forwarded";
    protected String localizedMessage;

    private static final Log LOG = LogFactory.getLog(ForwardedException.class);

    /**
     * @param cause cause
     */
    protected ForwardedException(Throwable cause) {
        super(MESSAGE_FORWARDED, cause);
        LOG.error(MESSAGE_FORWARDED, cause);
    }

    /**
     * @param message text
     * @param cause cause
     */
    public ForwardedException(String message, Throwable cause, Object... args) {
        super(message, cause);
        localizedMessage = Messages.getFormattedString(message, args);
        LOG.error(localizedMessage, cause);
    }

    public static ForwardedException illegalArgument(Object unknown, Object available) {
        return new ForwardedException("tsl2nano.unknowntype", null, unknown, StringUtil.toFormattedString(available,
            40, false));
    }

    public static ForwardedException illegalState(Object state, Object caller) {
        return new ForwardedException("tsl2nano.illegalstate", null, state, StringUtil.toFormattedString(caller,
            40, false));
    }

    @Override
    public String getMessage() {
        return getLocalizedMessage();
    }
    /**
     * @see java.lang.Throwable#getLocalizedMessage()
     */
    @Override
    public String getLocalizedMessage() {
        Throwable rootCause = getRootCause(this);
        if (isForwarded() && (rootCause == null || !Util.isEmpty(rootCause.getLocalizedMessage()))) {
            return rootCause != null ? rootCause.getLocalizedMessage() : Messages
                .getFormattedString("tsl2.nano.unknownerror", this.getClass(), getStackTracePart(this));
        } else if (rootCause != null) {
            return Messages.getFormattedString("tsl2nano.exception.text.with.cause", localizedMessage,
                StringUtil.toString(rootCause), getStackTracePart(rootCause));
        } else {
            return Messages.getFormattedString("tsl2nano.exception.text", localizedMessage, getStackTracePart(this));
        }
    }

    protected String getStackTracePart(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        StringBuffer buf = new StringBuffer();
        int length = LOG.isDebugEnabled() ? stackTrace.length : 1;
        for (int i = 0; i < length; i++) {
            buf.append(stackTrace[i] + "\n");
        }
        return buf.toString();
    }

    /**
     * isForwarded
     * 
     * @return true, if this exception is only forwarded
     */
    public boolean isForwarded() {
        return MESSAGE_FORWARDED.equals(super.getMessage());
    }

    /**
     * @return root cause (recursive)
     */
    public static Throwable getRootCause(Throwable ex) {
        Throwable cause = ex.getCause();
        if (cause == null) {
            if (ex instanceof UndeclaredThrowableException) {
                cause = ((UndeclaredThrowableException)ex).getUndeclaredThrowable();
            } else if (ex instanceof InvocationTargetException) {
                cause = ((InvocationTargetException)ex).getTargetException();
            }
            if (cause == null)
                return ex;
        }
        return getRootCause(cause);
    }

    /**
     * will throw ex if it is a RuntimeException, otherwise it will wrap it to a ForwardedException (extension of
     * RuntimeException) and throw it. The exception text will be logged once!
     * 
     * @param ex original (cause) exception
     * @return the forwarding exception, on any case an exception will be thrown. the return value is to do a
     *         convenience on calling!
     */
    public static Throwable forward(Throwable ex) {
        throw toRuntimeEx(ex, false);
    }

    /**
     * will return ex if it is a RuntimeException, otherwise it will wrap it to a ForwardedException.
     * 
     * @param ex original (cause) exception
     * @param wrapToForwardedException if true, an existing runtime exception will be wrapped into a ForwardedException
     * @return runtime exception
     */
    public static RuntimeException toRuntimeEx(Throwable ex, boolean wrapToForwardedException) {
        if (ex instanceof RuntimeException) {
            if (!(ex instanceof ForwardedException)) {
                if (wrapToForwardedException) {
                    ex = new ForwardedException(ex);
                } else {
                    LOG.error(MESSAGE_FORWARDED, ex);
                }
            }
            return (RuntimeException) ex;
        } else {
            return new ForwardedException(ex);
        }
    }
}
