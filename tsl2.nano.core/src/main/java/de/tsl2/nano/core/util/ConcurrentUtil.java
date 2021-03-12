/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 06.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;

/**
 * standard usage to create a daemon threads. defines parallel working jobs
 * through {@link #createParallelWorker(String, int, Class, Class)}.
 * 
 * @author Tom
 * @version $Revision$
 */
public class ConcurrentUtil {
    private static final Log LOG = LogFactory.getLog(ConcurrentUtil.class);

    @SuppressWarnings("rawtypes")
    private static final Map<Class, ThreadLocal<?>> threadLocals = new Hashtable<Class, ThreadLocal<?>>();

    private static final Map<Object, SuppliedWait> waiters = new Hashtable<>();

    /**
     * getCaller
     * 
     * @return calling method name
     */
    public static String getCaller() {
        // without calling security manager like Thread.getStacktrace() do.
        StackTraceElement[] st = new Exception().getStackTrace();
        return st.length > 2 ? st[2].toString() : "<unknown>";
    }

    public static Thread startDaemon(Runnable runnable) {
        return startDaemon(runnable.toString(), runnable, true, null);
    }

    public static Thread startDaemon(String name, Runnable runtime) {
        return startDaemon(name, runtime, true, null);
    }

    /**
     * starts the given runtime as daemon thread
     * 
     * @param name
     * @param runtime
     * @param lowPriority
     */
    public static Thread startDaemon(String name, Runnable runtime, boolean lowPriority,
            UncaughtExceptionHandler handler) {
        LOG.info("starting thread " + name);
        Thread thread = Executors.defaultThreadFactory().newThread(runtime);
        thread.setName(name);
        if (handler != null) {
            thread.setUncaughtExceptionHandler(handler);
        }
        if (lowPriority) {
            thread.setPriority(Thread.MIN_PRIORITY);
        }
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * provides thread local values, stored before through
     * {@link #setCurrent(Object)}. For further informations, see
     * {@link ThreadLocal} and {@link ThreadLocal#get()}
     * 
     * @param threadLocalType value type
     * @return value, if stored for the current thread or null
     */
    @SuppressWarnings("unchecked")
    public static <T> T getCurrent(Class<T> threadLocalType) {
        ThreadLocal<?> tl = threadLocals.get(threadLocalType);
        return (T) (tl != null ? tl.get() : null);
    }

    /**
     * collects all values, given by {@link #getCurrent(Class)} and puts them into a
     * map
     * 
     * @param threadLocalTypes defines the values to be loaded
     * @return map holding types and values of current thread
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Map<Class, Object> getCurrentAsMap(Class... threadLocalTypes) {
        HashMap<Class, Object> map = new HashMap<Class, Object>(threadLocalTypes.length);
        for (int i = 0; i < threadLocalTypes.length; i++) {
            map.put(threadLocalTypes[i], getCurrent(threadLocalTypes[i]));
        }
        return map;
    }

    /**
     * sets a new value. for further informations, {@link #getCurrent(Class)},
     * {@link ThreadLocal} and {@link ThreadLocal#set(Object)}.
     * 
     * @param values value to store as threadlocal inside the current thread.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void setCurrent(Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null)
                continue; // nothing to do
            ThreadLocal tl = (ThreadLocal) threadLocals.get(values[i].getClass());
            if (tl == null) {
                tl = new ThreadLocal();
                threadLocals.put(values[i].getClass(), tl);
            }
            tl.set(values[i]);
        }
    }

    /**
     * removes all values on the current thread, stored through
     * {@link #setCurrent(Object...)}.
     * 
     * @param types defines the values to be removed from the current thread
     */
    @SuppressWarnings("rawtypes")
    public static void removeCurrent(Class... types) {
        ThreadLocal tl;
        for (int i = 0; i < types.length; i++) {
            tl = threadLocals.get(types[i]);
            if (tl != null)
                tl.remove();
        }
    }

    /**
     * removes all values on all threads, stored through
     * {@link #setCurrent(Object...)}.
     * 
     * @param types defines the values to be removed from all threads
     */
    public static void removeAllCurrent(Class... types) {
        for (int i = 0; i < types.length; i++) {
            threadLocals.remove(types[i]);
        }
    }

    public static final void sleep(long milliseconds) {
        sleep(milliseconds, true);
    }

    /**
     * sleep convenience
     * 
     * @param milliseconds
     */
    public static final void sleep(long milliseconds, boolean doSysOutLog) {
        try {
            if (doSysOutLog)
                System.out.print(
                        "\n" + Thread.currentThread().getName() + " sleeping for " + milliseconds + " milliseconds...");
            Thread.sleep(milliseconds);
            if (doSysOutLog)
                System.out.print("...awake\n");
        } catch (InterruptedException e) {
            ManagedException.forward(e);
        }
    }

    public static final <T> T waitOn(Object waitObject, long timeout, Consumer<T> doOnResponse) {
        SuppliedWait<T> wait = new SuppliedWait<T>();
        try {
            if (waitObject != null)
                waiters.put(waitObject, wait);
            return wait.waitOn(waitObject, timeout, doOnResponse);
        } finally {
            if (waitObject != null)
                waiters.remove(waitObject);
        }
    }
    public static final void notifyWith(Object waitingObject, Object response) {
        SuppliedWait w = waiters.get(waitingObject);
        if (w != null)
            w.setResponseAndNotify(response);
    }

    /** TODO not sure, if that szenario would work - we are only on the own thread */
    public static final <T> T waitFor(Class<T> responseType) {
        return waitFor(Util.get("tsl2.nano.concurrent.pullwaittime", 1000), responseType);
    }

    public static final <T> T waitFor(long pullWaitTime, Class<T> responseType) {
        waitFor(pullWaitTime, () -> getCurrent(responseType) != null);
        return getCurrent(responseType);
    }

    public static final void waitFor(Supplier<Boolean> callback) {
        waitFor(Util.get("tsl2.nano.concurrent.pullwaittime", 1000), callback);
    }

    public static final void waitFor(long pullWaitTime, Supplier<Boolean> callback) {
        // try {
        //     Thread.currentThread().wait();
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }
        createReadWriteLock().read(() -> callback.get());
    	// while (!callback.get())
    	// 	sleep(pullWaitTime);
    }
    
    /**
     * calls {@link Thread#interrupt()} on a thread with the given name in the current threadgroup.
     * 
     * @param threadName thread to interrupt.
     * @return true, if thread was found and interrupted.
     */
    public static final boolean stopOrInterrupt(String threadName) {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        Thread allThreads[] = new Thread[tg.activeCount()];
        tg.enumerate(allThreads);
        for (int i = 0; i < allThreads.length; i++) {
            if (allThreads[i].getName().equals(threadName)) {
                LOG.debug("interrupting thread " + threadName);
                allThreads[i].interrupt();
                return true;
            }
        }
        LOG.error("couldn't find thread " + threadName);
        return false;
    }

    public static SuppliedLock createReadWriteLock() {
        return new SuppliedLock();
    }
    public static void runWorker(Runnable...runnables) {
    	createParallelWorker(runnables[0].toString()).run(runnables);
    }
    
    public static Worker<Object, Object> createParallelWorker(String name) {
        return createParallelWorker(name, 0, Object.class, Object.class);
    }

    /**
     * see {@link Worker}
     * 
     * @param name thread group name
     * @param priority priority of threads
     * @param input jobs input type
     * @param output jobs output type
     * @return worker instance. see {@link Worker}
     */
    public static <INPUT, OUTPUT> Worker<INPUT, OUTPUT> createParallelWorker(String name,
            int priority,
            Class<INPUT> input,
            Class<OUTPUT> output) {
        return new Worker<INPUT, OUTPUT>(name, 500, 200, priority);
    }

//    public static <T> T runOnThreadWithResult(String threadName, Runnable runnable, T[] resultHolder) {
//        //find the thread
//        Thread t;
//        ThreadGroup group = Thread.currentThread().getThreadGroup();
//        Thread[] allThreads = new Thread[Thread.activeCount()];
//        group.enumerate(allThreads);
//        for (int i = 0; i < allThreads.length; i++) {
//            if (allThreads[i].getName().equals(threadName)) {
//                t = allThreads[i];
//                break;
//            }
//        }
//        
//        t.set
//    }
    public static void reset() {
        threadLocals.clear();
        waiters.clear();
    }
    public static void doForCurrentThreadGroup(Consumer<Thread> consumer) {
		ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
		Thread[] threads = new Thread[threadGroup.activeCount()];
		threadGroup.enumerate(threads, true);
		LOG.info("doing " + consumer +  " on current thread group:\n" + StringUtil.toFormattedString(threads, -1));
		for (int i = 0; i < threads.length; i++) {
			consumer.accept(threads[i]);
		}
    }
}

/**
 * collects and starts (see {@link Worker#run(Runnable...)}) parallel working jobs. with
 * {@link Worker#waitForJobs(long)} you can wait for all jobs to be done.
 * 
 * @param <INPUT> job input
 * @param <OUTPUT> job output
 * @author Tom
 * @version $Revision$
 */
class Worker<INPUT, OUTPUT> {
    private static final Log LOG = LogFactory.getLog(Worker.class);
    String name;
    int count;
    Map<INPUT, OUTPUT> result = new ConcurrentHashMap<INPUT, OUTPUT>();
    int maxthreads;
    int idle;
    private int priority;

    /**
     * constructor
     * 
     * @param name
     * @param priority
     */
    public Worker(String name, int maxthreads, int idle, int priority) {
        super();
        this.name = name;
        this.maxthreads = maxthreads;
        this.idle = idle;
        this.priority = priority;
    }

    /**
     * run this given jobs in extra threads
     * 
     * @param jobs
     */
    public void run(Runnable... jobs) {
        if (jobs.length == 0) {
            throw new IllegalArgumentException("at least one job has to be given!");
        }
        for (int i = 0; i < jobs.length; i++) {
            /*
             * if there are to many ips and ports we have to wait for some threads to avoid outofmemory.
             */
            if (count - result.size() > maxthreads) {
                ConcurrentUtil.sleep(idle);
            }
            ConcurrentUtil.startDaemon(name + "-" + ++count, jobs[i], priority == Thread.MIN_PRIORITY, null);
        }
    }

    /**
     * waits until all given jobs are done.
     * 
     * @param timeout max time to wait
     * @return result of worker jobs
     */
    public Map<INPUT, OUTPUT> waitForJobs(long timeout) {
        LOG.info("========== " + name + " waiting for " + count + " jobs ================\n");
        long start = System.currentTimeMillis();
        while ((result.size() < count) && (start + timeout < System.currentTimeMillis())) {
            ConcurrentUtil.sleep(idle);
        }
        LOG.info("========== " + name + " has finished all jobs ================\n");
        return getResult();
    }

    public Map<INPUT, OUTPUT> getResult() {
        return result;
    }
}