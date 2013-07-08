/*
 * Copyright © 2002-2009 Thomas Schneider
 * Schwanthaler Strasse 69, 80336 München. Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.exception;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.Messages;
import de.tsl2.nano.util.StringUtil;

/**
 * provides a formatted exception with language bundle and message parameters.
 * 
 * @author TS 28.01.2009
 * @version $Revision$
 */
public class FormattedException extends RuntimeException {
    private static final long serialVersionUID = -2383031050992696074L;

    protected static final Log LOG = LogFactory.getLog(FormattedException.class);
    /** original messageKey */
    protected String messageKey = null;
    /** localised (evaluated) message */
    String myMessage = null;
    /** optional messageParameter */
    protected Object[] messageParameter;

    public static final String MSG_NOT_IMPLEMENTED_YET = "swartifex.notimplementedfailure";
    public static final String MSG_FORMAT_FAILURE = "swartifex.formatfailure";
    public static final String MSG_VALUERANGE_FAILURE = "swartifex.valuerangefailure";
    public static final String MSG_MANDATORY_FAILURE = "swartifex.mandatoryfailure";
    public static final String MSG_UNIQUE_FAILURE = "swartifex.uniqueobjectfailure";
    public static final String MSG_OBJECTINLIST_FAILURE = "swartifex.objectinlistfailure";
    public static final String MSG_UNSUPPORTED_COMMAND_FAILURE = "swartifex.unsupportedcommandfailure";
    public static final String MSG_IMPLEMENTATION_FAILURE = "swartifex.implementationerror";

    public static final CharSequence PART_TIMEOUT = "ocket time";

    /**
     * @param message exception message
     */
    public FormattedException(String message) {
        this(message, new Object[0]);
    }

    /**
     * @param message exception message
     * @param messageParameter optional parameters
     */
    public FormattedException(String message, Object[] messageParameter) {
        this(message, messageParameter, null);
    }

    /**
     * @param message exception message
     * @param messageParameter optional parameters
     * @param cause the initial exception cause
     */
    public FormattedException(String message, Object[] messageParameter, Throwable cause) {
        super(message, cause);
        this.messageKey = message;
        this.messageParameter = messageParameter;
        if (!(cause instanceof FormattedException)) {
            logOnce();
        }
    }

    /**
     * @see java.lang.Throwable#getLocalizedMessage()
     */
    @Override
    public String getLocalizedMessage() {
        if (myMessage == null) {
            myMessage = getText(super.getMessage());
        }
        return myMessage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        if (myMessage == null) {
            myMessage = getText(super.getMessage());
        }
        return myMessage;
    }

    /**
     * @param message message
     * @return localized messaged with formatted parameters
     */
    protected String getText(String message) {
        return Messages.getFormattedString(message, messageParameter);
    }

    /**
     * shows a dialog with its exception
     */
    public void showException() {
        logOnce();
    }

    @Override
    public String toString() {
        return super.toString() + " : " + Arrays.toString(messageParameter);
    }

    /**
     * mechanism to log the error only once for this error.
     */
    public void logOnce() {
        if (myMessage == null) {
            LOG.error(this.getLocalizedMessage(), this);
        }
    }

    /**
     * framework message on implementation errors
     * 
     * @param information optional extended implementation information
     * @param unallowedExpression unallowed Object or Type
     * @param allowedExpression allowed Objects or Types
     * @return new exception
     */
    public static final FormattedException implementationError(String information,
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
        return new FormattedException(MSG_IMPLEMENTATION_FAILURE, new Object[] { unallowedExpression,
            callStack,
            infText });
    }

    /**
     * getMessageKey
     * 
     * @return the original not-translated message key.
     */
    public String getMessageKey() {
        return messageKey;
    }

    /**
     * convenience to check for a special message key
     * 
     * @param messageKey to compare
     * @return true, if the given message key equals the currents exception message key.
     */
    public boolean isMessageKey(String messageKey) {
        return messageKey.equals(this.messageKey);
    }

    /**
     * to be used after service calls to translate the message on the client.
     */
    public void translate() {
        myMessage = getText(messageKey);
    }
}
