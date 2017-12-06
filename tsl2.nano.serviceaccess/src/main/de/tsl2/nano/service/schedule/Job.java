/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: May 14, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.service.schedule;

import java.text.DateFormat;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import javax.ejb.Timer;
import javax.ejb.TimerHandle;

import org.apache.commons.logging.Log;

import de.tsl2.nano.action.IStatus;
import de.tsl2.nano.bean.def.SStatus;
import de.tsl2.nano.core.log.LogFactory;


/**
 * simple structure to hold job schedule informations. may be given as ejb timer-info.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Job<RUNNABLE> implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -2843721543076198343L;

    private static final Log LOG = LogFactory.getLog(Job.class);

    private String name;
    private boolean stopOnError = true;
    private boolean stopOnConcurrent = false;
    private Collection<RUNNABLE> callbacks;
    protected Serializable context;
    private TimerHandle timerHandle;
    private Object creator;
    private long createdAt;
    private long startedAt = 0;
    private long stoppedAt = 0;
    /** if timer expired, no access to the timer is available, so we stored that once */
    private Date nextStart;
    private IStatus lastResult = null;

    private transient String uniqueName;
//    private boolean persistent = true;

    /**
     * constructor
     * 
     * @param timerHandle handle belonging to the created ejb-timer
     * @param callbacks runnables to be executed on timer-timeout
     * @param creator creator of this job
     * @param stopOnError if true, the job will stop on any error
     * @param stopOnConcurrent if true, the job is not startable, if another job is running
     */
    public Job(String name,
            TimerHandle timerHandle,
            Collection<RUNNABLE> callbacks,
            Serializable context,
            Object creator,
            boolean stopOnError,
            boolean stopOnConcurrent) {
        super();
        this.name = name;
        this.timerHandle = timerHandle;
        if (timerHandle != null) {
            nextStart = timerHandle.getTimer().getNextTimeout();
        }
        this.callbacks = callbacks;
        this.context = context;
        this.stopOnError = stopOnError;
        this.stopOnConcurrent = stopOnConcurrent;
        this.creator = creator;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * @return Returns the stopOnError.
     */
    public boolean isStopOnError() {
        return stopOnError;
    }

    /**
     * @return Returns the stopOnConcurrent.
     */
    public boolean isStopOnConcurrent() {
        return stopOnConcurrent;
    }

    /**
     * runnables to be started sequentially on timer-timeout
     * @return Returns the callbacks.
     */
    public Collection<RUNNABLE> getCallbacks() {
        return callbacks;
    }

    /**
     * context to be given to the callbacks - if needed.
     * @return Returns the context.
     */
    public Serializable getContext() {
        return context;
    }

    /**
     * getCreator
     * @return optional creator information (if provided by constructor call).
     */
    public Object getCreator() {
        return creator;
    }
    
    /**
     * The {@link TimerHandle} is only available on application server!
     * 
     * @return Returns the timerHandle.
     */
    public TimerHandle getTimerHandle() {
        return timerHandle;
    }

    /**
     * getCreationTime
     * @return
     */
    public Long getCreationTime() {
        return createdAt;
    }
    
    /**
     * getLastStart
     * @return the last start , if available. if the ejb-container was stopped, this information is not available
     */
    public Long getLastStart() {
        return startedAt != 0 ? new Long(startedAt) : null;
    }

    /**
     * getLastStop
     * @return the last stop , if available. if the ejb-container was stopped, this information is not available
     */
    public Long getLastStop() {
        return stoppedAt != 0 ? new Long(stoppedAt) : null;
    }

    /**
     * getLastResult
     * 
     * @return the last result , if available. if the ejb-container was stopped, this information is not available
     */
    public IStatus getLastResult() {
        return lastResult;
    }

    /**
     * called by framework
     */
    public void setAsStarted() {
        LOG.info("running job " + toString() + " with " + callbacks.size() + " processes. last run was: " + new Date(startedAt));
        startedAt = System.currentTimeMillis();
    }

    /**
     * called by framework
     * 
     * @param ex (optional) exception if failed, othewise null
     */
    public void setAsStopped(Exception ex) {
        stoppedAt = System.currentTimeMillis();
        try {
            nextStart = timerHandle.getTimer().getNextTimeout();
        } catch (Exception ex1) {
            // --> timer expired
            nextStart = null;
        }
        LOG.info("stopping job " + toString() + " with " + callbacks.size()
            + " processes. started at: "
            + DateFormat.getDateInstance().format(new Date(startedAt))
            + " duration: "
            + DateFormat.getTimeInstance().format(new Date(stoppedAt - startedAt))
            + "\n   next start: "
            + (nextStart == null ? "expired!" : DateFormat.getDateInstance().format(nextStart))
            + (ex != null ? "\n   error: " + ex : ""));

            setLastException(ex);
    }

    /**
     * setLastException
     * 
     * @param ex
     */
    public void setLastException(Throwable ex) {
        if (ex != null) {
            lastResult = new SStatus(ex);
        } else {
            lastResult = new SStatus();
        }
    }

    /**
     * isRunning
     * 
     * @return true, if job is running
     */
    public boolean isRunning() {
        return stoppedAt - startedAt < 0;
    }

    /**
     * getNextRun
     * 
     * @return date of next run or null if expired
     */
    public Date getNextStart() {
        return nextStart;
    }

    /**
     * isExpired
     * 
     * @return true, if timer expired
     */
    public boolean isExpired() {
        return timerHandle != null && nextStart == null;
    }

    /**
     * getName
     * 
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * combination of name and creation time. see {@link #getUniqueName(String, long)}.
     * @return unique name
     */
    public String getUniqueName() {
        if (uniqueName == null) {
            uniqueName = getUniqueName(name, createdAt);
        }
        return uniqueName;
    }
    
    /**
     * combination of name and creation time
     * @param name simple name
     * @param creationTime creation time
     * @return name + creation time
     */
    public static String getUniqueName(String name, long creationTime) {
        return name + "-" + DateFormat.getInstance().format(new Date(creationTime));
    }
    
    /**
     * called by framework
     * 
     * @param timerHandle the timers handle
     */
    public TimerHandle setTimerHandle(Timer timer) {
        assert this.timerHandle == null : "timer should only be set once!";
        this.timerHandle = timer.getHandle();
        nextStart = timer.getNextTimeout();
        LOG.info("creating timer ==> nextstart: " + nextStart
            + ", handle: "
            + timer.getHandle()
            + ", unique-name: "
            + getUniqueName());
        return timerHandle;
    }

    @Override
    public String toString() {
//        if (timerHandle == null)
            return getUniqueName();
//        Timer timer = timerHandle.getTimer();
//        return name + "-" + (timer.getSchedule() != null ? timer.getSchedule() : timer.getNextTimeout());
    }
}
