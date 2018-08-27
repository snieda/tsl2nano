/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 16.07.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.execution;

import java.io.Serializable;
import java.util.Map;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.historize.Volatile;

/**
 * A Runner (see {@link IPRunnable}) that checks, if it is already running - or (see {@link Volatile} the result already
 * expired.
 * 
 * @author Tom
 * @version $Revision$
 */
public class VolatileResult<T> extends Volatile<T> {
    IPRunnable<T, Map<String, Object>> runner;
    boolean isRunning;

    public VolatileResult(long period, IPRunnable<T, Map<String, Object>> runner) {
        super(period, null);
        this.runner = runner;
    }

    public T get(Map<String, Object> context, Object... extArgs) {
        return get(runner, context, extArgs);
    }
    
    public T get(IPRunnable<T, Map<String, Object>> runner, Map<String, Object> context, Object... extArgs) {
        if (expired() && !isRunning)
            try {
                set(runner.run(context, extArgs));
            } catch(Exception ex) {
                //on errors , the expired should be set, too
                activate();
                ManagedException.forward(ex);
            } finally {
                isRunning = false;
            }
        return get();
    }
    
    public String getName() {
        return runner.getName();
    }
    public Map<String, ? extends Serializable> getParameter() {
        return runner.getParameter();
    }
}
