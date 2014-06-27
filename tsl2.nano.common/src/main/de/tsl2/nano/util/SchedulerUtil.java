/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 23.06.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * simple delegator to {@link ScheduledExecutorService}.
 * 
 * @author Tom
 * @version $Revision$
 */
public class SchedulerUtil {
    static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * runAt
     * 
     * @param delay wait time for first start
     * @param period period time
     * @param end end time (stopping task)
     * @param unit time unit
     * @param command command to execute
     * @return future
     */
    public static final ScheduledFuture<?> runAt(long delay, long period, long end, TimeUnit unit, Runnable command) {
        //define the command scheduler
        final ScheduledFuture<?> s = scheduler.scheduleAtFixedRate(command, delay, period, unit);
        //define the stopper scheduler
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                s.cancel(true);
            }
        }, end, unit);
        return s;
    }
}
