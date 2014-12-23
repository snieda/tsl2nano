/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Mar 19, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core.execution;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;

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
@SuppressWarnings("rawtypes")
public class CompatibilityLayer {
    private static final Log LOG = LogFactory.getLog(CompatibilityLayer.class);

    private Map<Runnable, Runnable> runnerCache;
    private Map<String, Method> methodCache;

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
        methodCache = new Hashtable<String, Method>();
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
    public <C extends Serializable> C runOptional(ICRunnable<C> action, Object... args) {
        try {
            return action.run(null, args);
        } catch (Throwable e) {
            LOG.warn("couldn't run given optional action: " + e);
            Message.send(e.toString());
            return null;
        }
    }

    public Object run(String className, String methodName, Class[] par, Object... args) {
        try {
            return BeanClass.createBeanClass(className).callMethod(null, methodName, par, args);
        } catch (Throwable e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public Object runOptional(String className, String methodName, Class[] par, Object... args) {
        try {
            return BeanClass.createBeanClass(className).callMethod(null, methodName, par, args);
        } catch (Throwable e) {
            LOG.warn("couldn't run given optional action: " + e);
            Message.send(e.toString());
            return null;
        }
    }

    public Object runOptional(Object instance, String methodName, Class[] par, Object... args) {
        try {
            return BeanClass.call(instance, methodName, par, args);
        } catch (Throwable e) {
            Message.send(e.toString());
            LOG.warn("couldn't run given optional action: " + e);
            return null;
        }
    }

    /**
     * enables predefinition of reflection calls.
     * @param id unique name for method call (see {@link #runRegistered(String, Object...)})
     * @param className full classpath
     * @param methodName method name
     * @param force if true, availability of given method wont be checked
     * @param par methods parameter classes
     */
    public void registerMethod(String id, String className, String methodName, boolean force, Class... par) {
        Method method = new Method(className, methodName, par);
        if (force || isAvailable(className)) {
            methodCache.put(id, method);
            LOG.info("registering method '" + id + "': " + method);
        } else {
            LOG.warn("didn't register method '" + id + "': " + method);
        }
    }

    /**
     * let's call a predefined method with {@link #registerMethod(String, String, String, Class[], boolean)}.
     * @param id method id (defined in {@link #registerMethod(String, String, String, Class[], boolean)})
     * @param args call arguments (must match par in {@link #registerMethod(String, String, String, Class[], boolean)})
     * @return call result
     */
    public Object runRegistered(String id, Object...args) {
        Method m = methodCache.get(id);
        if (m == null) {
            throw new IllegalArgumentException("method " + id + " is not registered in compatibilitylayer!");
        }
        return runOptional(m.className, m.method, m.par, args);
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
     * pre loads the given classes. usable on calling {@link #runOptional(String, String, Class[], Object...)}
     * 
     * @param classNames classes to load
     * @return loaded classes - throws {@link RuntimeException} if failed.
     */
    public Class[] load(String... classNames) {
        Class[] classes = new Class[classNames.length];
        try {
            for (int i = 0; i < classNames.length; i++) {
                classes[i] = Thread.currentThread().getContextClassLoader().loadClass(classNames[i]);
            }
        } catch (ClassNotFoundException e) {
            ManagedException.forward(e);
        }
        return classes;
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

@SuppressWarnings("rawtypes")
class Method {
    String className;
    String method;
    Class[] par;

    /**
     * constructor
     * 
     * @param className
     * @param method
     * @param par
     */
    public Method(String className, String method, Class... par) {
        super();
        this.className = className;
        this.method = method;
        this.par = par;
    }
    @Override
    public String toString() {
        return className + " => " + method + "(" + StringUtil.toString(par) + ")";
    }
}
