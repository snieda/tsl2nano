/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Mar 19, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.execution;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.util.bean.BeanClass;

/**
 * UNDER CONSTRUCTION
 * <p/>
 * The Compatibility Layer tries to give alternatives on running code inside unknown systems. Means, if libraries are
 * not available but used, alternatives can be configured and used. Avoids unusable libraries in cause of
 * NoClassDefFoundError or NoSuchMethodError.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class CompatibilityLayer {
    private static final Log LOG = LogFactory.getLog(CompatibilityLayer.class);

    private Map<Runnable, Runnable> runnerCache;

    /** to be run instead of original action */
    private static final Runnable EMPTY_RUNNER = new Runnable() {

        @Override
        public void run() {
            LOG.debug("compatibility problem - jumping to next call");
        }
    };

    public static final boolean IS_JDK = System.getProperty("java.vendor").matches(".*(Sun|Oracle).*");
    public static final boolean MIN_JDK14 = IS_JDK && System.getProperty("java.version").matches("1[.][4-9].*");
    public static final boolean MIN_JDK15 = IS_JDK && System.getProperty("java.version").matches("1[.][5-9].*");
    public static final boolean MIN_JDK16 = IS_JDK && System.getProperty("java.version").matches("1[.][6-9].*");

    /**
     * constructor
     */
    public CompatibilityLayer() {
        super();
        runnerCache = new Hashtable<Runnable, Runnable>();
    }

    /**
     * runs the given action - if any problem (like a NoClassDef or NoSuchMethod) occurred, the error will be logged and
     * false will be returned. for performance and security aspects, you should call this method only to establish
     * system compatibilities.
     * <p/>
     * TODO: use action name, ask for environment vars, byte-code-enhancing
     * 
     * @param action action to be started optionally
     * @return true, if action run without error.
     */
    public boolean runOptional(Runnable action) {
        Runnable defaultAction = runnerCache.get(action);
        action = defaultAction != null ? defaultAction : action;
        try {
            action.run();
            return true;
        } catch (Throwable e) {
            LOG.warn("couldn't run given optional action: " + e);
            storeCompatibilityAction(action, EMPTY_RUNNER);
            return false;
        }
    }

    /**
     * runs the given action - if any problem (like a NoClassDef or NoSuchMethod) occurred, the error will be logged and
     * false will be returned. for performance and security aspects, you should call this method only to establish
     * system compatibilities.
     * <p/>
     * TODO: use action name, ask for environment vars, byte-code-enhancing
     * 
     * @param action action to be started optionally
     * @return true, if action run without error.
     */
    public <C extends Serializable> C runOptional(IRunnable<C> action, Object... args) {
        try {
            return action.run(null, args);
        } catch (Throwable e) {
            LOG.warn("couldn't run given optional action: " + e);
            return null;
        }
    }

    public Object runOptional(String className, String methodName, Class[] par, Object... args) {
        try {
            return BeanClass.createBeanClass(className).callMethod(null, methodName, par, args);
        } catch (Throwable e) {
            LOG.warn("couldn't run given optional action: " + e);
            return null;
        }
    }

    /**
     * storeCompatibilityAction
     * 
     * @param action
     * @param defaultActionOnProblem
     */
    public void storeCompatibilityAction(Runnable action, Runnable defaultActionOnProblem) {
        LOG.info("storing compatibility problem - using default action instead in future!");
        runnerCache.put(action, defaultActionOnProblem);
    }

    /**
     * checks, whether the given class can be found on current threads classloader. TODO: map with lib-names +
     * main-classes - filled by properties
     * 
     * @param className class to load through threads classloader
     * @return true, if class could be load
     */
    public boolean isAvailable(String className) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
