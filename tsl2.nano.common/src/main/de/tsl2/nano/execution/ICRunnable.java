/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Feb 27, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.execution;

import java.io.Serializable;

/**
 * generic interface to start any runnable with a given context and returning the context as result. the standard
 * {@link Runnable} interface has no args and no return value. the extArgs argument is optional extending of the caller
 * arguments.</p> useful for stateless remote calls - then the context must be serializable. like the {@link Runnable}
 * this interface does not throw a checked exception. checked exceptions should be handled by catching them and throwing
 * a forwarding unchecked exception.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface ICRunnable<CONTEXT extends Serializable> {
    /**
     * starts the process
     * 
     * @param context fix run arguments
     * @param extArgs optional extended arguments
     * @return refreshed context as result
     */
    CONTEXT run(CONTEXT context, Object... extArgs);
}
