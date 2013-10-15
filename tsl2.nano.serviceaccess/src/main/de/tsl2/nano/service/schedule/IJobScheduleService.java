/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jan 11, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.service.schedule;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import javax.ejb.Remote;
import javax.ejb.ScheduleExpression;
import javax.ejb.TimerHandle;

/**
 * see {@link IJobScheduleLocalService}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@Remote
public interface IJobScheduleService<RUNNABLE> {
    /**
     * creates a standard timer or a single action timer (expires=true)
     * 
     * @param name job identifier
     * @param time when to start the job
     * @param scheduleExpression timer expression
     * @param context (optional) context to be given to the callback - if possible
     * @param user (optional) user to be given as job-info - if possible
     * @param stopOnError whether to stop on any error.
     * @param stopOnConcurrent whether to cancel the new job, if another job is running
     * @param expire whether the job will be started once.
     * @param callbacks actions to be run syncron.
     */
    void createJob(String name, Date time,
            Serializable context,
            Serializable user,
            boolean stopOnError,
            boolean stopOnConcurrent,
            boolean expire,
            Collection<RUNNABLE> callbacks);

    /**
     * creates an interval timer
     * 
     * @param name job identifier
     * @param time interval to define starts
     * @param stopOnError whether to stop on any error.
     * @param expire whether the job will be started once.
     * @param callbacks actions to be run syncron.
     */
    void createJob(String name, long time, boolean stopOnError, RUNNABLE... callbacks);

    /**
     * createScheduleJob
     * 
     * @param name job identifier
     * @param scheduleExpression timer expression
     * @param stopOnError whether to stop on any error.
     * @param callbacks actions to be run syncron.
     */
    void createJob(String name, ScheduleExpression scheduleExpression,
            boolean stopOnError, RUNNABLE... callbacks
            );

    /**
     * createScheduleJob
     * 
     * @param name job identifier
     * @param scheduleExpression timer expression
     * @param context (optional) context to be given to the callback - if possible
     * @param user (optional) user to be given as job-info - if possible
     * @param stopOnError whether to stop on any error.
     * @param stopOnConcurrent whether to cancel the new job, if another job is running
     * @param callbacks callbacks to be run syncron.
     * @return timer handle
     */
    TimerHandle createScheduleJob(String name,
            ScheduleExpression scheduleExpression,
            Serializable context,
            Serializable user,
            boolean stopOnError,
            boolean stopOnConcurrent,
            RUNNABLE... callbacks);

    /**
     * disposes the timed job, that was created by {@link #createScheduleJob(ScheduleExpression, Collection, boolean)}.
     * 
     * @param name identifier of job to stop.
     */
    void disposeScheduleJob(String name);

    /**
     * disposeAllScheduledJobs
     */
    void disposeAllScheduledJobs();

    /**
     * getCurrentJobs
     * @return all job names - given to the timer service
     */
    Collection<String> getCurrentJobNames();
}
