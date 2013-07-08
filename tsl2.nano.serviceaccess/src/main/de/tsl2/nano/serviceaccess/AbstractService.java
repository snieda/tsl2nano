/*
 * Copyright © 2002-2008 Thomas Schneider
 * Schwanthaler Strasse 69, 80336 München. Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.serviceaccess;

import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Iterator;

import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.exception.ForwardedException;

/**
 * base service class. provides starting privileged actions with a security subject, logging and exception handling.
 * 
 * @author Thomas Schneider
 */
public abstract class AbstractService {
    protected static final Log LOG = LogFactory.getLog(AbstractService.class);

    /**
     * @param <T> result type
     * @param subject subject of logged-in user
     * @param action privileged action
     * @return result of action
     */
    protected <T> T doAs(Subject subject, PrivilegedAction<T> action) {
        try {
            final T t = Subject.doAs(getSubject(), action);
            logRemoteCallReturn(t);
            if (t != null) {
                try {
                    validate(t);
                } catch (final Exception e) {
                    LOG.debug("validation failed: ", e);
                }
            }
            return t;
        } catch (final Exception ejbEx) {
            handleException(ejbEx);
            return null;
        }
    }

    /**
     * Log the value returned from a remote call (if DEBUG is enabled). For returned collections the first 3 contained
     * elements are logged if TRACE is enabled.
     * 
     * @param object the return object that should be logged
     */
    private static void logRemoteCallReturn(Object object) {
        if (!LOG.isTraceEnabled()) {
            return;
        }
        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        final String methodName = stack.length > 4 ? stack[4].getMethodName() : "unknown(no stacktrace)";
        final StringBuilder str = new StringBuilder(methodName + " returned:\n\t");
        if (object != null) {
            if (Collection.class.isAssignableFrom(object.getClass())) {

                final Collection<?> collection = (Collection<?>) object;
                str.append("collection size=" + collection.size());
                if (LOG.isTraceEnabled()) {
                    final Iterator<?> iter = collection.iterator();
                    for (int i = 0; i < 5 && iter.hasNext(); i++) {
                        str.append("\n\tcollection[" + i + "]=" + iter.next());
                    }
                }
            } else if (object.getClass().isPrimitive() || object.getClass().getName().startsWith("java.lang")) {
                str.append(object.getClass().getName() + "::" + object);
            } else {
                str.append(object);
            }
        } else {
            str.append(object);
        }
        LOG.trace(str.toString());
    }

    /**
     * Handle an {@link Exception} thrown by a remote call. This means try to "extract and rethrow" the wrapped
     * exception.
     * 
     * @param e the Exception
     * @return cause of exception (only to have a return value)
     * @throws RuntimeException always
     */
    private Exception handleException(Exception e) {
        return (Exception) ForwardedException.forward(e);
//		Throwable cause = e.getCause();
//		if (cause == null) {
//			cause = e;
//		}
//		if (cause instanceof RuntimeException) {
//			throw (RuntimeException) cause;
//		}
//		throw new FormattedException("swartifex.client.servererror", null,
//				cause);
    }

    /**
     * @return subject of authenticated user
     */
    abstract protected Subject getSubject();

    /**
     * @param <T> object type to validate
     * @param t object to validate
     */
    protected <T> void validate(T t) {
        LOG.trace("no validation activated!");
    }
}
