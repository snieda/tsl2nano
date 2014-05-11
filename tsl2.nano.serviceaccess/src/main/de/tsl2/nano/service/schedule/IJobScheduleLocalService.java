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

import javax.ejb.Local;
import javax.ejb.ScheduleExpression;
import javax.ejb.TimerHandle;

import de.tsl2.nano.core.execution.ICRunnable;

/**
 * Generic ejb 3.1 job scheduler. is able to run (synchronized) a collection of {@link Runnable}s for the given
 * scheduleExpression. RUNNABLE is generic, to provide the use of other Runnable interfaces (like {@link ICRunnable} to
 * have the possibility to provide run arguments and return values. You should override the method calling the callback.
 * <p/>
 * If the ejb-container was stopped, not all informations are available. the jobs, last-start, last-stop, last-status
 * are not provided by the timer-service. This service provides a job-history to show this attributes.
 * <p>
 * WARNING: <br>
 * The {@link TimerHandle} can only be used on local system - clients are not able to send {@link TimerHandle} as
 * argument.
 * <p>
 * This class is intended to start timers programmatically - not automatic. The automatic way would be to use the
 * Schedule(ScheduleExpression) annotation.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@Local
public interface IJobScheduleLocalService<RUNNABLE> {
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
     * @param callbacks callbacks to be run syncron.
     * @return timer handle
     */
    TimerHandle createScheduleJob(String name,
            Date time,
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
     * @param callbacks callbacks to be run syncron.
     * @return timer handle
     */
    TimerHandle createScheduleJob(String name, long time, boolean stopOnError, RUNNABLE... callbacks);

    /**
     * createScheduleJob
     * 
     * @param name job identifier
     * @param scheduleExpression timer expression
     * @param stopOnError whether to stop on any error.
     * @param callbacks callbacks to be run syncron.
     * @return timer handle
     */
    TimerHandle createScheduleJob(String name,
            ScheduleExpression scheduleExpression,
            boolean stopOnError,
            RUNNABLE... callbacks);

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
     * @param timerHandle (optional) timer to stop.
     */
    void disposeScheduleJob(TimerHandle timerHandle);

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
     * getJob
     * 
     * @param name identifier
     * @return job instance
     */
    Job<RUNNABLE> getJob(String name);

    /**
     * getCurrentJobs
     * 
     * @return all jobs given to the timer service
     */
    Collection<Job<RUNNABLE>> getCurrentJobs();

    /**
     * getJobHistory
     * 
     * @return all finished jobs
     */
    Collection<JobHistoryEntry> getJobHistory();
}
