/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Mar 13, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.exception;

import org.apache.commons.logging.Log;
import de.tsl2.nano.log.LogFactory;

import de.tsl2.nano.Messages;

/**
 * caller check to constrain the use of some internal public methods.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class AccessChecker {
    private static final Log LOG = LogFactory.getLog(AccessChecker.class);

    /**
     * caller check to constrain the use of some internal public methods. not the direct caller but the caller of the
     * caller will be checked!
     * <p/>
     * this method should only be called in a develop-mode - because evaluating the stacktrace is inefficient. it may be
     * used to check, whether calling conventions are ok - on develop time.
     * 
     * @param packageExpression package expression to fulfill
     * @param callerPosition position in stacktrace to check
     * @param resultCallerName (out-var) if not null, the method at callerPosition will be filled into.
     * @return true, if callers caller package fulfills the given package expression
     */
    public static final boolean checkCaller(String packageExpression, int callerPosition, StringBuffer resultCallerName) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length <= callerPosition)
            return false;
        else {
            boolean matched = stackTrace[callerPosition].getClassName().matches(packageExpression);
            if (!matched && resultCallerName != null) {
                resultCallerName.append(stackTrace[callerPosition]);
            }
            return matched;
        }
    }

    /**
     * see {@link #checkCaller(String)}. used for this framework. will only be evaluated, if debug logging was enabled.
     */
    public static final void checkInternalCall() {
        if (LOG.isDebugEnabled()) {
            String packageName = Messages.class.getPackage().getName();
            StringBuffer callerName = new StringBuffer();
            if (!checkCaller(packageName + ".*", 4, callerName))
                throw FormattedException.implementationError("This method should only be called by framework classes!",
                    "method " + callerName + ": <method call outside of package: " + packageName + ">");
        }
    }
}
