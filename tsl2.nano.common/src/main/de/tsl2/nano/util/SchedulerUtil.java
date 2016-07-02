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

import de.tsl2.nano.core.util.DateUtil;

/**
 * simple delegator to {@link ScheduledExecutorService}.
 * 
 * @author Tom
 * @version $Revision$
 */
public class SchedulerUtil {
    static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * runs the given command at given time on given unit. example: timeOnUnit=2*60*60*1000, unit=TimeUnit.DAY -->
     * command will be started each day at 2 o'clock.
     * 
     * @param timeOnUnit
     * @param end end time (stopping task). if end = -1, no stopping task will be created
     * @param unit time unit
     * @param command command to execute
     * @return future
     */
    public static final ScheduledFuture<?> runAt(long timeOnUnit, long end, TimeUnit unit, Runnable command) {
        long delay = DateUtil.getDelayToNextTimeUnit(unit);
        long period = DateUtil.getTimeUnitInMillis(unit, 1);
        return runAt(delay, period, end, unit, command);
    }

    /**
     * runAt see {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit).}
     * 
     * @param delay wait time for first start
     * @param period period time
     * @param end end time (stopping task). if end = -1, no stopping task will be created
     * @param unit time unit
     * @param command command to execute
     * @return future
     */
    public static final ScheduledFuture<?> runAt(long delay, long period, long end, TimeUnit unit, Runnable command) {
        //define the command scheduler
        final ScheduledFuture<?> s = scheduler.scheduleAtFixedRate(command, delay, period, unit);
        //define the stopper scheduler
        if (end > -1) {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    s.cancel(true);
                }
            }, end, unit);
        }
        return s;
    }
}
