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
 * {@link Runnable} interface has no args. the extArgs argument is optional extending of the caller arguements.</p>
 * useful for stateless remote calls - then the context must be serializable.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IRunnable<CONTEXT extends Serializable> {
    /**
     * starts the process
     * 
     * @param context fix run arguments
     * @param extArgs optional extended arguments
     * @return refreshed context as result
     */
    CONTEXT run(CONTEXT context, Object... extArgs);
}
