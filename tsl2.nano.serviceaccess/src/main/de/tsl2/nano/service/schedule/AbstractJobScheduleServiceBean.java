/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider, www.idv-ag.de
 * created on: Jan 11, 2012
 * 
 * Copyright: (c) Thomas Schneider, www.idv-ag.de 2012, all rights reserved
 */
package de.tsl2.nano.service.schedule;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.NoMoreTimeoutsException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.collection.ListSet;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.util.FileUtil;
import de.tsl2.nano.util.StringUtil;

/**
 * see {@link IJobScheduleLocalService}.
 * 
 * @author Thomas Schneider, Thomas Schneider, www.idv-ag.de
 * @version $Revision$
 */
@Singleton
public abstract class AbstractJobScheduleServiceBean<RUNNABLE> implements
        IJobScheduleLocalService<RUNNABLE>,
        IJobScheduleService<RUNNABLE> {
    private static final Log LOG = LogFactory.getLog(AbstractJobScheduleServiceBean.class);

    private Collection<Job<RUNNABLE>> jobs = new ArrayList<Job<RUNNABLE>>();
    //TODO: serialize job-history
    private Collection<JobHistoryEntry> jobHistory = new ListSet<JobHistoryEntry>();
    private static final String FILE_HISTORY = "jobhistory.ser";

    private final boolean persistent = true;

    @Resource
    protected TimerService timerService;

    @PostConstruct
    protected void initializePersistedJobs() {
        Collection<Timer> timers = timerService.getTimers();
        for (Timer timer : timers) {
            //load persisted jobs after server crash or shutdown
            if (timer.getInfo() instanceof Job) {
                Job<RUNNABLE> job = (Job<RUNNABLE>) timer.getInfo();
                job.setTimerHandle(timer);
                jobs.add(job);
            }
        }
        //should be done through file-connector
        if (new File(FILE_HISTORY).exists()) {
            jobHistory = (Collection<JobHistoryEntry>) FileUtil.load(FILE_HISTORY);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimerHandle createScheduleJob(String name,
            Date time,
            Serializable context,
            Serializable user,
            boolean stopOnError,
            boolean stopOnConcurrent,
            boolean expire,
            Collection<RUNNABLE> callbacks) {
        Job job = initializeJob(name, callbacks, context, user, stopOnError, stopOnConcurrent);
        Timer timer;
        if (expire) {
            timer = timerService.createSingleActionTimer(time, new TimerConfig(job, persistent));
        } else {
            timer = timerService.createTimer(time, job);
        }
        return job.setTimerHandle(timer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimerHandle createScheduleJob(String name, long time, boolean stopOnError, RUNNABLE... callbacks) {
        Job job = initializeJob(name, Arrays.asList(callbacks), null, null, true, true);
        final Timer timer = timerService.createTimer(time, job);
        return job.setTimerHandle(timer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimerHandle createScheduleJob(String name,
            ScheduleExpression scheduleExpression,
            boolean stopOnError,
            RUNNABLE... callbacks) {
        return createScheduleJob(name, scheduleExpression, null, null, stopOnError, true, callbacks);
    }

    /**
     * {@inheritDoc}
     */
    public TimerHandle createScheduleJob(String name,
            ScheduleExpression scheduleExpression,
            Serializable context,
            Serializable user,
            boolean stopOnError,
            boolean stopOnConcurrent,
            RUNNABLE[] callbacks) {
        Job job = initializeJob(name, Arrays.asList(callbacks), context, user, stopOnError, stopOnConcurrent);
        final Timer timer = timerService.createCalendarTimer(scheduleExpression, new TimerConfig(job, persistent));
        return job.setTimerHandle(timer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createJob(String name,
            Date time,
            Serializable context,
            Serializable user,
            boolean stopOnError,
            boolean stopOnConcurrent,
            boolean expire,
            Collection<RUNNABLE> callbacks) {
        createScheduleJob(name, time, context, user, stopOnError, stopOnConcurrent, expire, callbacks);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createJob(String name, long time, boolean stopOnError, RUNNABLE... callbacks) {
        createScheduleJob(name, time, stopOnError, callbacks);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createJob(String name,
            ScheduleExpression scheduleExpression,
            boolean stopOnError,
            RUNNABLE... callbacks) {
        createScheduleJob(name, scheduleExpression, stopOnError, callbacks);
    }

    /**
     * initializeTimer
     * 
     * @param callbacks
     * @param stopOnError
     * @param stopOnConcurrent
     * @param timer
     * @return timer handle
     * @throws IllegalStateException
     * @throws NoSuchObjectLocalException
     * @throws EJBException
     * @throws NoMoreTimeoutsException
     */
    protected Job initializeJob(String name,
            Collection<RUNNABLE> callbacks,
            Serializable context,
            Serializable user,
            boolean stopOnError,
            boolean stopOnConcurrent) throws IllegalStateException,
            NoSuchObjectLocalException,
            EJBException,
            NoMoreTimeoutsException {
        if (callbacks == null || callbacks.size() == 0) {
            throw new FormattedException("list of callbacks is null or empty - cannot start the runJobs service");
        }
        //timer.getHandle() is only callable on persisted timers!
        Job<RUNNABLE> job = new Job<RUNNABLE>(name, null, callbacks, context, user, stopOnError, stopOnConcurrent);
        jobs.add(job);
        return job;
    }

    /**
     * getJob
     * 
     * @param the the jobs name or unique name (see {@link Job#getName()} and {@link Job#getUniqueName()}. if you give
     *            only the short name of {@link Job#getName()} the first found item will be returned.
     * @return job or null
     */
    public Job<RUNNABLE> getJob(String name) {
        for (Job<RUNNABLE> job : jobs) {
            if (job.getUniqueName().equals(name) || job.getName().equals(name))
                return job;
        }
        return null;
    }

    /**
     * @deprecated: use {@link #getJob(String)} instead getJob
     * @param th the jobs timerhandle
     * @return job or null
     */
    public Job<RUNNABLE> getJob(TimerHandle th) {
        for (Job<RUNNABLE> job : jobs) {
            if (job.getTimerHandle().equals(th))
                return job;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disposeScheduleJob(TimerHandle timerHandle) {
        Job<RUNNABLE> job = getJob(timerHandle);
        if (job != null) {
            LOG.info("disposing job " + job);
            timerHandle.getTimer().cancel();
            jobs.remove(job);
        } else {
            LOG.warn("timerHandle " + timerHandle + " not available!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disposeScheduleJob(String name) {
        Job<RUNNABLE> job = getJob(name);
        if (job != null) {
            LOG.info("disposing job " + job);
            job.getTimerHandle().getTimer().cancel();
            jobs.remove(job);
        } else {
            LOG.warn("job with name " + name + " not available!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Job<RUNNABLE>> getCurrentJobs() {
        checkExpiredJobs();
        return jobs;
    }

    /**
     * removes expired jobs
     */
    private void checkExpiredJobs() {
        Collection<Job> expired = new LinkedList<Job>();
        for (Job job : jobs) {
            if (job.isExpired()) {
                expired.add(job);
            }
        }
        if (expired.size() > 0) {
            LOG.info("removing expired jobs: " + StringUtil.toString(expired, 200));
            jobs.removeAll(expired);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getCurrentJobNames() {
        //because the timerHandle inside a job is not transferable, only the names will be evaluated.
        Collection<String> jobNames = new ArrayList<String>(jobs.size());
        for (Job job : jobs) {
            jobNames.add(job.getName());
        }
        return jobNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disposeAllScheduledJobs() {
//        final Collection<Timer> timers = timerService.getTimers();
        for (final Job<RUNNABLE> job : jobs) {
            job.getTimerHandle().getTimer().cancel();
        }
        jobs.clear();
    }

    /**
     * will be started, if the timer, created by {@link #createScheduleJob(ScheduleExpression, Collection, boolean)} has
     * a timeout. the callback-methods will be run synchron, but in an own thread, to avoid service-blocking
     * 
     * @param timer
     */
    @Timeout
    protected void runJob(final Timer timer) {
        final Job<RUNNABLE> job = getJob(timer.getHandle());
        if (job.isStopOnConcurrent()) {
            Job runningJob = getRunningJob();
            if (runningJob != null) {
                LOG.error("job-start canceled. no concurrent running jobs are allowed.\nPlease use argument 'stopOnConcurrent=false' if you want to allow concurrent running jobs!");
                RuntimeException ex = new FormattedException("swartifex.concurrentfailure", new Object[] { job,
                    runningJob });
                job.setLastException(ex);
                throw ex;
            }
        }
        /*
         * don't block the service.
         */
        final AbstractJobScheduleServiceBean _this = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                job.setAsStarted();
                for (final RUNNABLE runnable : job.getCallbacks()) {
                    try {
                        _this.run(runnable, job.getContext());
                    } catch (final Exception ex) {
                        if (job.isStopOnError()) {
                            RuntimeException fwdEx = ForwardedException.toRuntimeEx(ex, true);
                            stopRun(timer, job, fwdEx);
                            throw fwdEx;
                        } else {
                            job.setLastException(ex);
                            LOG.error("continuning after error:" + ex.toString(), ex);
                        }
                    }
                }
                stopRun(timer, job, null);
            }
        }).start();
    }

    /**
     * will be called, to set new properties on the stopped job. this has to be done asynchron to let the timerservice
     * refresh the timer instance after leaving the timeout method {@link #runJob(Timer)}.
     * 
     * @param timer timer of stopped job
     * @param job job to refresh informations on
     * @param ex (optional) error instance
     */
    protected void stopRun(final Timer timer, final Job job, final Exception ex) {
        Runnable stopJob = new Runnable() {
            @Override
            public void run() {
                //be sure, the timeout method 'runJob' was ended
                try {
                    Thread.sleep(ex != null ? 500 : 100);
                } catch (InterruptedException e) {
                    ForwardedException.forward(e);
                }
                job.setAsStopped(ex);
                addToHistory(job);
                //if job is expired, removed it from list
                if (timer.getNextTimeout() == null)
                    jobs.remove(job);
                LOG.info("next run will be: " + timer.getNextTimeout());
            }
        };
        new Thread(stopJob, job.toString()).start();
    }

    protected void addToHistory(Job job) {
        jobHistory.add(new JobHistoryEntry(job));
        //backup to file-system (should be done through file-connector)
        FileUtil.save(FILE_HISTORY, (Serializable) jobHistory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<JobHistoryEntry> getJobHistory() {
        return jobHistory;
    }

    /**
     * hasRunningJob
     * 
     * @return true, if another job is running.
     */
    protected Job getRunningJob() {
        for (Job<RUNNABLE> job : jobs) {
            if (job.isRunning())
                return job;
        }
        return null;
    }

    /**
     * runCallback
     * 
     * @param runnable to start
     */
    protected abstract void run(RUNNABLE runnable, Serializable context);
}