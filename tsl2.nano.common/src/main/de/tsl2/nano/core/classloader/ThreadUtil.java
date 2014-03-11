/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 06.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core.classloader;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.log.LogFactory;

/**
 * standard usage to create a daemon thread
 * 
 * @author Tom
 * @version $Revision$
 */
public class ThreadUtil {
    private static final Log LOG = LogFactory.getLog(ThreadUtil.class);

    public static Thread startDaemon(String name,
            Runnable runtime) {
        return startDaemon(name, runtime, true, null);
    }
    
    /**
     * starts the given runtime as daemon thread
     * 
     * @param name
     * @param runtime
     * @param lowPriority
     */
    public static Thread startDaemon(String name,
            Runnable runtime,
            boolean lowPriority,
            UncaughtExceptionHandler handler) {
        LOG.info("starting thread " + name);
        Thread thread = Executors.defaultThreadFactory().newThread(runtime);
        thread.setName(name);
        if (handler != null)
            thread.setUncaughtExceptionHandler(handler);
        if (lowPriority)
            thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}
