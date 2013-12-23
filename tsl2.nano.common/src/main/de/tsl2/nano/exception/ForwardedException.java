/*
 * Copyright © 2002-2009 Thomas Schneider
 * Schwanthaler Strasse 69, 80336 München. Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.exception;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import de.tsl2.nano.Environment;
import de.tsl2.nano.Messages;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.util.StringUtil;

/**
 * For unchecked Exception handling. If you catch an exception and want to forward this exception in a runtime exception
 * - use this class. The message of this exception will show the cause message and can use resource bundles.
 * 
 * Use {@link #getLocalizedMessage()} to show the message. This will use resource bundles.
 * 
 * @author TS 28.01.2009
 * @version $Revision$
 */
public class ForwardedException extends FormattedException {
    private static final long serialVersionUID = 1L;
    private static final String MESSAGE_FORWARDED = "tsl2nano.forwarded";
    protected boolean isInsideGetLocalizedMessage = false;

//    private static final Log LOG = LogFactory.getLog(ForwardedException.class);

    /**
     * @param cause cause
     */
    public ForwardedException(Throwable cause) {
        super(MESSAGE_FORWARDED, null, cause);
    }

    /**
     * @param message text
     * @param cause cause
     */
    public ForwardedException(String message, Throwable cause) {
        super(message, null, cause);
    }

    /**
     * @see java.lang.Throwable#getLocalizedMessage()
     */
    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }

    /**
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    public String getMessage() {
        if (myMessage == null) {
            if (getCause() == null) {
                throw new FormattedException("tsl2nano.implementationerror",
                    new Object[] { "cause=null",
                        "ForwardedException must have a cause! Please use <FormattedException> to create a new RuntimeException" });
            }
            Throwable current = this;
            final int MAX_MSG_LINECOUNT = 12;
            Throwable cause = getCause();
            String message = super.getMessage();
            //on java exceptions append the class-name to the message
            String causeMsg = isJavaException(cause) || cause.getMessage() == null ? cause.toString()
                : cause.getMessage();
            boolean msgContainsCause = false;
            if (MESSAGE_FORWARDED.equals(messageKey)) {
                current = cause;
                cause = getRootCause();
                message = StringUtil.toFormattedString(causeMsg, MAX_MSG_LINECOUNT, false);
                causeMsg = getRootCause().getMessage();
                if (message != null && !(cause == current)) {
                    msgContainsCause = causeMsg != null && message.contains(causeMsg);
                }
                if (causeMsg != null && causeMsg.startsWith("!")) {
                    causeMsg = getText(StringUtil.substring(causeMsg, "!", "!"));
                }
            }
            /*
             * don't show the message twice
             */
            if (cause instanceof FormattedException && ((FormattedException)cause).myMessage.trim().equals(this.myMessage.trim())) {
                cause = null;
            }
            if (message == null) {
                final String msg = StringUtil.toFormattedString(current.toString(), MAX_MSG_LINECOUNT, false);
                message = Messages.getFormattedString("tsl2nano.unknownerror", msg, getStackTracePart(current));
            } else if (isJavaException(current)) {
                message = Messages.getFormattedString("tsl2nano.runtimeerror", message, getStackTracePart(current));
            }
            if (cause != null && causeMsg == null) {
                causeMsg = Messages.getFormattedString("tsl2nano.unknownerror", cause, getStackTracePart(current));
            } else if (isJavaException(cause)) {
                causeMsg = Messages.getFormattedString("tsl2nano.runtimeerror", cause, getStackTracePart(current));
            }
            myMessage = getText(message);
            if (!msgContainsCause) {
                myMessage += (message != null && message.length() > 0 && cause != null && causeMsg.length() > 0 ? "\n\n" + getText("tsl2nano.cause")
                    + "\n"
                    : "") + (cause != null ? causeMsg : "");
            }
        }
        return myMessage;
    }

    protected String getStackTracePart(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        StringBuffer buf = new StringBuffer();
        int length = LOG.isDebugEnabled() ? Integer.MAX_VALUE : 1;
        for (int i = 0; i < stackTrace.length; i++) {
            buf.append(stackTrace[i] + "\n");
        }
        return buf.toString();
    }
    
    /**
     * @param current exception
     * @return true, if exception is a standard java exception.
     */
    private boolean isJavaException(Throwable current) {
        return current != null && BeanUtil.isStandardType(current.getClass());
    }

    /**
     * @return root cause (recursive)
     */
    public Throwable getRootCause() {
        Throwable cause = getCause();
        if (cause != null) {
            while (cause.getCause() != null) {
                cause = cause.getCause();
                if (cause.getCause() == null) {
                    break;
                }
            }
            return cause;
        }
        return this;
    }

    /**
     * localizes each word
     * 
     * @param message text
     * @return localized (by each word) message
     */
    protected String getText(String message, ResourceBundle bundle) {
        String text = message;
        if (bundle != null) {
            final StringBuffer buf = new StringBuffer();
            final StringTokenizer tokenizer = new StringTokenizer(message);
            String w = null;
            while (tokenizer.hasMoreTokens()) {
                try {
                    w = tokenizer.nextToken();
                    buf.append(bundle.getString(w) + " ");
                } catch (final MissingResourceException ex) {
                    //Ok, not found, no problem!
                    buf.append(w + " ");
                }
            }
            text = buf.toString();
        }
        return super.getText(text);
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
            if (ex instanceof FormattedException) {
                final FormattedException fe = (FormattedException) ex;
                fe.logOnce();
            } else {
                if (wrapToForwardedException) {
                    ex = new ForwardedException(ex);
                }
            }
            return (RuntimeException) ex;
        } else {
            return new ForwardedException(ex);
        }
    }
}
