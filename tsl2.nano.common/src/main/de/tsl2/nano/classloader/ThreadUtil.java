/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 06.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.classloader;

import java.util.concurrent.Executors;

/**
 * standard usage to create a daemon thread
 * 
 * @author Tom
 * @version $Revision$
 */
public class ThreadUtil {
    /**
     * starts the given runtime as daemon thread
     * 
     * @param name
     * @param runtime
     * @param lowPriority
     */
    public static Thread startDaemon(String name, Runnable runtime, boolean lowPriority) {
        Thread thread = Executors.defaultThreadFactory().newThread(runtime);
        thread.setName(name);
        if (lowPriority)
            thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}
