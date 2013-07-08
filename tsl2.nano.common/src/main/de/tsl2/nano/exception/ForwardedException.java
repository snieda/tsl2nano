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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.Messages;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.bean.BeanUtil;

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
    private static final String MESSAGE_FORWARDED = "swartifex.forwarded";
    protected boolean isInsideGetLocalizedMessage = false;

    private static final Log LOG = LogFactory.getLog(ForwardedException.class);

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
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    public String getMessage() {
        if (myMessage == null) {
            if (getCause() == null) {
                throw new FormattedException("swartifex.implementationerror",
                    new Object[] { "cause=null",
                        "ForwardedException must have a cause! Please use <FormattedException> to create a new RuntimeException" });
            }
            Throwable current = this;
            final int MAX_MSG_LINECOUNT = 12;
            Throwable cause = getCause();
            String message = super.getMessage();
            //on java exceptions append the class-name to the message
            String causeMsg = isJavaException(cause) ? cause.toString() : cause.getMessage();
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
            if (cause != null && cause == current) {
                cause = null;
            }
            if (message == null) {
                final String msg = StringUtil.toFormattedString(current.toString(), MAX_MSG_LINECOUNT, false);
                message = Messages.getFormattedString("swartifex.unknownerror", msg, current.getStackTrace()[0]);
            } else if (isJavaException(current)) {
                message = Messages.getFormattedString("swartifex.runtimeerror", message, current.getStackTrace()[0]);
            }
            if (cause != null && causeMsg == null) {
                causeMsg = Messages.getFormattedString("swartifex.unknownerror", cause, cause.getStackTrace()[0]);
            } else if (isJavaException(cause)) {
                causeMsg = Messages.getFormattedString("swartifex.runtimeerror", cause, cause.getStackTrace()[0]);
            }
            myMessage = getText(message);
            if (!msgContainsCause) {
                myMessage += (message != null && message.length() > 0 && cause != null && causeMsg.length() > 0 ? "\n\n" + getText("swartifex.cause")
                    + "\n"
                    : "") + (cause != null ? causeMsg : "");
            }
        }
        return myMessage;
    }

//    /**
//     * @param current exception
//     * @return true, if exception is a standard java runtime exception.
//     */
//    private boolean isJavaRuntimeException(Throwable current) {
//        return (current instanceof RuntimeException) && isJavaException(current);
//    }

    /**
     * @param current exception
     * @return true, if exception is a standard java exception.
     */
    private boolean isJavaException(Throwable current) {
        return current != null && BeanUtil.isStandardType(current.getClass());
    }

    /**
     * @see java.lang.Throwable#getLocalizedMessage()
     */
    @Override
    public String getLocalizedMessage() {
//        if (LOG.isDebugEnabled()) {
//            StringBuffer causeMessage = new StringBuffer();
//            Throwable cause = getCause();
//            while (cause != null) {
//                //the invocationtargetexception is a forwarded exception with no message --> ignore it
//                if (!(cause instanceof InvocationTargetException)) {
//                    if (cause instanceof FormattedException) {
//                        causeMessage.append(getText(cause.getMessage()));
//                    } else {
//                        causeMessage.append(getText(cause.toString()));
//                    }
//                    causeMessage.append("\n\n");
//                }
//                //the forwardedexception will handle stack trace, too!
////            if (cause instanceof ForwardedException)
////                break;
//                cause = cause.getCause();
//            }
//            return getText(super.getMessage()) + "\n\n" + getText("swartifex.cause") + ":\n" + causeMessage;
//        } else {
        return getMessage();
//        }
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
