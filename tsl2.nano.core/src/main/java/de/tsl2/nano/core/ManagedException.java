/*
 * Copyright © 2002-2009 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.core;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.SupplierEx;
import de.tsl2.nano.core.util.SupplierExVoid;
import de.tsl2.nano.core.util.Util;

/**
 * For unchecked Exception handling. If you catch an exception and want to forward this exception in a runtime exception
 * - use this class. The message of this exception will show the cause message and can use resource bundles.
 * 
 * Use {@link #getLocalizedMessage()} to show the message. This will use resource bundles.
 * 
 * @author TS 28.01.2009
 * @version $Revision$
 */
public class ManagedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private static final String MESSAGE_FORWARDED = "tsl2nano.forwarded";
    protected String localizedMessage;

    private static final Log LOG = LogFactory.getLog(ManagedException.class);

    protected ManagedException(Throwable cause) {
        this(cause, true);
    }

    public ManagedException(String msg) {
    	this(msg, new Object[0]);
	}
    protected ManagedException(Throwable cause, boolean logNow) {
        super(MESSAGE_FORWARDED, cause);
        if (logNow) {
            LOG.error(MESSAGE_FORWARDED, cause);
        }
    }

    public ManagedException(String message, Object... args) {
        this(message, null, args);
    }
    
    /**
     * @param message text
     * @param cause cause
     */
    public ManagedException(String message, Throwable cause, Object... args) {
        super(message, cause);
        localizedMessage = Messages.getFormattedString(message, args);
        LOG.error(localizedMessage, cause);
    }

    public static ManagedException illegalArgument(Object argument, Object available) {
        return new ManagedException("tsl2nano.unknowntype", null, argument, StringUtil.toFormattedString(available,
            40, false));
    }

    public static ManagedException illegalState(Object state, Object caller) {
        return new ManagedException("tsl2nano.illegalstate", null, state, StringUtil.toFormattedString(caller,
            40, false));
    }

    /**
     * framework message on implementation errors
     * 
     * @param information optional extended implementation information
     * @param unallowedExpression unallowed Object or Type
     * @param allowedExpression allowed Objects or Types
     * @return new exception
     */
    public static final ManagedException implementationError(String information,
            Object unallowedExpression,
            Object... allowedExpressions) {

        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        final List<StackTraceElement> callStack = Arrays.asList(stackTrace[2], stackTrace[3], stackTrace[4]);
        String infText = "";
        if (allowedExpressions.length > 0) {
            infText = "\n\nImplementation Informations:\n\npossible expressions:\n" + StringUtil.toFormattedString(allowedExpressions,
                40);
        }
        if (information != null) {
            infText += "\n" + information;
        }
        return new ManagedException("tsl2nano.implementationerror", new Object[] { unallowedExpression,
            callStack,
            infText });
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
        rootCause = rootCause == this ? null : rootCause;
        if (isForwarded() && (rootCause == null || !Util.isEmpty(rootCause.getLocalizedMessage()))) {
            return rootCause != null ? rootCause.toString() : Messages
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
        int length = LOG.isDebugEnabled() || stackTrace.length < 1 ? stackTrace.length : 1;
        for (int i = 0; i < length; i++) {
            buf.append(stackTrace[i] + "\n");
        }
        return buf.toString();
    }

    public static void assertion(boolean assertion, String message, Object...args) {
        if (!assertion) {
            throw new ManagedException(message, args);
        }
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
            if (cause == null) {
                return ex;
            }
        }
        return getRootCause(cause);
    }

    public static Throwable forward(Throwable ex) {
        return forward(ex, true);
    }
    
    /**
     * will throw ex if it is a RuntimeException, otherwise it will wrap it to a ManagedException (extension of
     * RuntimeException) and throw it. The exception text will be logged once!
     * 
     * @param ex original (cause) exception
     * @return the forwarding exception, on any case an exception will be thrown. the return value is to do a
     *         convenience on calling!
     */
    public static Throwable forward(Throwable ex, boolean logNow) {
        throw toRuntimeEx(ex, false, logNow);
    }

    /**
     * will return ex if it is a RuntimeException, otherwise it will wrap it to a ManagedException.
     * 
     * @param ex original (cause) exception
     * @param wrapToForwardedException if true, an existing runtime exception will be wrapped into a ManagedException
     * @return runtime exception
     */
    public static RuntimeException toRuntimeEx(Throwable ex, boolean wrapToForwardedException, boolean logNow) {
        if (ex instanceof RuntimeException) {
            if (!(ex instanceof ManagedException)) {
                if (wrapToForwardedException) {
                    ex = new ManagedException(ex, logNow);
                } else if (logNow){
                    LOG.error(MESSAGE_FORWARDED, ex);
                }
            }
            return (RuntimeException) ex;
        } else {
            return new ManagedException(ex, logNow);
        }
    }
    
    public static <T> T trY(SupplierEx<T> callback) {
        return trY(callback, true);
    }
    /**let the trY to the standard exception handling  */
    public static <T> T trY(SupplierEx<T> callback, boolean escalate) {
        try {
            return callback.get();
        } catch(Exception ex) {
            LOG.error(ex);
            return escalate ? (T) forward(ex, false) : null;
        }
    }

    public static <T> T trY(SupplierExVoid<T> callback) {
        return trY(callback, true);
    }
    /**let the trY to the standard exception handling  */
    public static <T> T trY(SupplierExVoid<T> callback, boolean escalate) {
        try {
            return callback.get();
        } catch(Exception ex) {
            LOG.error(ex);
            return escalate ? (T) forward(ex, false) : null;
        }
    }

    public static String toString(Throwable ex) {
    	return StringUtil.printToString(c -> ex.printStackTrace(c));
    }

    public static String toStringCause(Throwable ex) {
    	return ex.toString() + " -> Cause: " + getRootCause(ex).toString() + " Code: " + findInStacktrace(getRootCause(ex), ".*" + Util.FRAMEWORK_PACKAGE + ".*"); 
    }
    
	private static String findInStacktrace(Throwable ex, String regex) {
		StackTraceElement[] stackTrace = ex.getStackTrace();
		for (int i = 0; i < stackTrace.length; i++) {
			if (stackTrace[i].toString().matches(regex))
				return stackTrace[i].toString();
		}
		return stackTrace.length == 0 ? "...empty stacktrace" : "...no stacktrace line found for " + regex;
	}

	public static void handleError(Throwable ex) {
    	if (ENV.get("app.mode.strict", false))
    		forward(ex);
    	else
    		LOG.error(toStringCause(ex));
	}
	public static void handleError(String msg, Object...args) {
		for (int i=0; i < args.length; i++) {
			args[i] = StringUtil.toString(args[i], -1);
		}
		msg = String.format(msg, args);
    	if (ENV.get("app.mode.strict", false))
    		throw new IllegalStateException(msg);
    	else
    		LOG.error(msg);
	}
	public static void writeError(Throwable e, final String fileName) {
		Util.trY(() -> {
			File file = FileUtil.userDirFile(fileName);
			file.getParentFile().mkdirs();
			PrintWriter s = new PrintWriter(file);
			e.printStackTrace(s);
			s.flush();
			s.close();
		});
	}
}
