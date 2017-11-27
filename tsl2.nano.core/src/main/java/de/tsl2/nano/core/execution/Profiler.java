/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Mar 31, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.core.execution;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;

/**
 * simple performance and memory analyzer - to be usable by tests and debugging. use
 * {@link #stressTest(String, long, Runnable)} or {@link #compareTests(String, long, Runnable...)} to do enhanced
 * performance-testing.
 * 
 * Example 1:
 * 
 * <pre>
 * Profiler.si().starting(myAction, name);
 * for (long i = 0; i &lt; test_count; i++) {
 *     action.run();
 * }
 * return Profiler.si().ending(myAction, name);
 * </pre>
 * 
 * 
 * Example 2 (counting durations of all calls):
 * 
 * <pre>
 * Profiler.si().starting(myAction, name);
 * Profiler.si().ending(myAction, name, false);
 * ...
 * Profiler.si().summarize(myAction, name);
 * </pre>
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class Profiler {
    private static Profiler self = null;

    /** map used as hashset (like in original HashSet, but with find-access to the elements) */
    Map<Integer, ProfilerObject> profObjects = new HashMap<Integer, ProfilerObject>();

    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    private static final Log LOG = LogFactory.getLog(Profiler.class);

    /**
     * hide the default constructor
     */
    private Profiler() {
        super();
        cal.getTimeZone().setRawOffset(0);
    }

    /**
     * si
     * 
     * @return singelton instance
     */
    public static final Profiler si() {
        if (self == null) {
            self = new Profiler();
        }
        return self;
    }

    /**
     * do something to let the processor work on high frequency - to have equal preconditions for the first test and its
     * following tests.
     */
    protected long prepareProcessor() {
        LOG.info("preparing a hot working cpu...");
        try {
            Thread.sleep(2000);
            long j = workLoop(10000);
            return j;
        } catch (InterruptedException e) {
            ManagedException.forward(e);
            return -1;
        }
    }

    /**
     * workLoop
     * @return
     */
    public long workLoop(long steps) {
        int j=0;
        for (int i = 0; i < steps; i++) {
            j += i;
        }
        return j;
    }

    /**
     * starting
     * 
     * @param classOrInstance class - or instance to get the class from
     * @param name identifier on logging entries
     * @return start time
     */
    public long starting(Object classOrInstance, String name) {
        ProfilerObject po = ProfilerObject.fastConstruct();
        po.clazz = getClazz(classOrInstance);
        po.name = name;
        po.start = System.currentTimeMillis();
        po.startMem = getUsedMem();
        profObjects.put(po.hashCode(), po);

        cal.setTimeInMillis(po.start);
        log("\n=============================================================================================================================");
        log("==>> starting " + po.clazz.getSimpleName()
            + ": '"
            + name
            + "' at "
            + sdf.format(cal.getTime())
            + " used mem: "
            + po.startMem
            / 1000l
            + "kb");
        log("=============================================================================================================================");

        return po.start;
    }

    /**
     * used memory by jvm
     * 
     * @return used = (total - free)
     */
    public static final long getUsedMem() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * simple convenience
     * 
     * @param classOrInstance class - or instance to get the class from
     * @return class
     */
    private static Class getClazz(Object classOrInstance) {
        return (Class) (classOrInstance instanceof Class ? classOrInstance : classOrInstance.getClass());
    }

    /**
     * delegates to {@link #ending(Object, String, boolean)} with clean=true
     */
    public long ending(Object classOrInstance, String name) {
        return ending(classOrInstance, name, true);
    }

    /**
     * {@link #starting(Object, String)} must be called before - with identical name. logs some profiler informations
     * like duration and memory diff.
     * 
     * @param classOrInstance class - or instance to get the class from
     * @param name identifier to find the starting entry
     * @return duration time
     */
    public long ending(Object classOrInstance, String name, boolean clean) {
        ProfilerObject po = clean ? profObjects.remove(ProfilerObject.hash(getClazz(classOrInstance), name))
            : profObjects.get(ProfilerObject.hash(getClazz(classOrInstance), name));
        po.end = System.currentTimeMillis();
        po.endMem = getUsedMem();

        cal.setTimeInMillis(po.end - po.start);
        log("\n=============================================================================================================================");
        log("<<== ending   " + po.clazz.getSimpleName()
            + ": '"
            + name
            + "' duration: "
            + sdf.format(cal.getTime())
            + " diff mem: "
            + (po.endMem - po.startMem)
            / 1000l
            + "kb");
        log("=============================================================================================================================");

        return po.end - po.start;
    }

    private static final void log(String text) {
        System.out.println(text);
//        LOG.debug(text);
    }

    /**
     * tests and logs the given action - starting it 'test_count' times.
     * 
     * @param name test action name
     * @param test_count count to start every action
     * @param action test action
     * @return duration in millis
     */
    public long stressTest(final String name, final long test_count, final Runnable action) {
        prepareProcessor();
        Profiler.si().starting(action, name);
        for (long i = 0; i < test_count; i++) {
            action.run();
        }
        return Profiler.si().ending(action, name);
    }

    /**
     * compareTests NOT IMPLEMENTED YET
     * 
     * @param test_count count to start every action
     * @param actions different tests to do
     */
    public void compareTests(String description, final long test_count, final Runnable... actions) {
        log(description);
        List<Long> durations = new ArrayList<Long>(actions.length);
        for (int i = 0; i < actions.length; i++) {
            durations.add(stressTest("test " + i, test_count, actions[i]));
        }
        log("Summary of " + actions.length + " tests with " + test_count + " counts");
        for (int i = 0; i < actions.length; i++) {
            log("test " + i + ": " + sdf.format(new Date(durations.get(i))));
        }
    }

    /**
     * main test: junit test expects the standard constructor - not available.
     */
    public static void main(String args[]) {
        int TEST_COUNT = 100000000;

        /*
         * testing the profiler with ProfilerObjects as test objects.
         * comparing standard construction with fast construction.
         * 
         * we have to assign the new construct to a variable - to not be
         * optimized (and thrown away) by the compiler.
         * 
         * FAZIT: the fast-construction seems not to be faster!
         */
        Profiler.si().stressTest("standard-construction", TEST_COUNT, new Runnable() {
            ProfilerObject po = null;

            @Override
            public void run() {
                po = new ProfilerObject();
            }
        });
        Profiler.si().stressTest("fast-construction", TEST_COUNT, new Runnable() {
            ProfilerObject po = null;

            @Override
            public void run() {
                po = ProfilerObject.fastConstruct();
            }
        });
    }

    /**
     * summarize
     * 
     * @param testObject (optional) test object or class to find
     * @param name (optional) name to find
     * @return total amount of all found durations
     */
    public long summarize(Object testObject, String name) {
        long totalAmount = 0;
        for (ProfilerObject po : profObjects.values()) {
            if (testObject == null || getClazz(testObject).equals(po.clazz)) {
                if (name == null || name.equals(po.name)) {
                    totalAmount += po.end - po.start;
                }
            }
        }
        log("\n=============================================================================================================================");
        log("total durations of testObject=" + testObject
            + " and name="
            + name
            + " is:"
            + sdf.format(new Date(totalAmount)));
        log("=============================================================================================================================");
        return totalAmount;
    }

    /**
     * clears profiler cache
     */
    public void clear() {
        profObjects.clear();
    }
}

/**
 * structure holding profile data for an implementation block
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
class ProfilerObject implements Cloneable {
    Class clazz;
    String name;
    long start;
    long end;
    long startMem;
    long endMem;

    private static final ProfilerObject cloneSrc = new ProfilerObject();

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return hash(clazz, name);
    }

    /**
     * calc standard hash of class+name
     * 
     * @param clazz class
     * @param name identifier
     * @return standard hash
     */
    public static final int hash(Class clazz, String name) {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ProfilerObject other = (ProfilerObject) obj;
        if (clazz == null) {
            if (other.clazz != null) {
                return false;
            }
        } else if (!clazz.equals(other.clazz)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * constructs an instance not through constructor but through {@link #clone()}.
     * 
     * @return new {@link ProfilerObject} instance
     */
    public static final ProfilerObject fastConstruct() {
        try {
            return (ProfilerObject) cloneSrc.clone();
        } catch (CloneNotSupportedException e) {
            ManagedException.forward(e);
            return null;
        }
    }
}
