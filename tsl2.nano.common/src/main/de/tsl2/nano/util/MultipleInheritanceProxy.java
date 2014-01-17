/*
 * Copyright © 2002-2008 Thomas Schneider
 * Schwanthaler Strasse 69, 80336 München. Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import de.tsl2.nano.log.LogFactory;

/**
 * simple proxy mechanism to enhance implemenations through additional interfaces and their delegates. the order of the
 * delegates will define the priority of the delegates. each delegate will be asked, if it implement the
 * interface-method.
 * <p/>
 * 
 * To work on delegator not directly implementing this interfaces, use {@link DelegatorProxy}.
 * 
 * @author ts 26.02.2010
 * @version $Revision: 1.0 $
 */
public class MultipleInheritanceProxy implements InvocationHandler {
    /** implementation instances. the order will define the priority! */
    Collection<Object> delegates = null;

    private static final Log LOG = LogFactory.getLog(MultipleInheritanceProxy.class);

    /**
     * Constructor
     */
    protected MultipleInheritanceProxy() {
        this(new LinkedList());
    }

    /**
     * Constructor
     * 
     * @param delegates see {@linkplain #delegates}
     */
    protected MultipleInheritanceProxy(Collection<Object> delegates) {
        super();
        this.delegates = delegates;
    }

    /**
     * creates a new proxy instance with BeanProxy as invocationhandler.
     * 
     * @param interfaze interface to implement
     * @param attributes map of bean attributes for this bean implementation
     * @return implementation of the given interface.
     */
//    public static Object createBeanImplementation(Class<?> interfaze, Map<String, Object> attributes) {
//    	return createBeanImplementation(interfaze, attributes, BeanProxy.class.getClassLoader());
//    }

    /**
     * creates a new proxy instance with BeanProxy as invocationhandler.
     * 
     * @param interfazes interfaces to implement
     * @param delegates implementation instances. the order is important!
     * @param classloader classloader to use the interfaces and instances
     * @return implementation of the given interfaces.
     */
    public static Object createMultipleInheritance(Class[] interfazes, Collection delegates, ClassLoader classLoader) {
        return Proxy.newProxyInstance(classLoader, interfazes, new MultipleInheritanceProxy(delegates));
    }

    /**
     * combines two instances into one.
     * 
     * @param instanceToEnhance object to enhance with functionality of extendedImplementation
     * @param extendedImplementation object to be used as enhancer.
     * @param cast interface to be returned
     * @return combination of both implementations.
     */
    public static final <T> T createEnhancedPresenter(Object instanceToEnhance,
            Object extendedImplementation,
            Class<T> cast,
            ClassLoader classLoader) {
        final HashSet<Class> interfazes = new HashSet<Class>(getAllInterfaces(instanceToEnhance.getClass()));
        interfazes.addAll(getAllInterfaces(extendedImplementation.getClass()));
        return (T) MultipleInheritanceProxy.createMultipleInheritance(interfazes.toArray(new Class[0]),
            Arrays.asList(instanceToEnhance, extendedImplementation),
            classLoader);
    }

    /**
     * getAllInterfaces
     * 
     * @param cls clazz to evaluate
     * @return all implementing interfaces (of all subclasses)
     */
    protected static Collection<Class> getAllInterfaces(Class<?> cls) {
        final HashSet<Class> interfazes = new HashSet<Class>();
        while (cls != null) {
            interfazes.addAll(Arrays.asList((Class[]) cls.getInterfaces()));
            cls = cls.getSuperclass();
        }
        return interfazes;
    }

    /**
     * the first implementing delegate collection member will be used to invoke the given method.
     * 
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        boolean implementationFound = false;
        for (final Object d : delegates) {
            if (method.getDeclaringClass().isAssignableFrom(d.getClass())) {
                result = method.invoke(d, args);
                implementationFound = true;
                break;
            }
        }
        if (!implementationFound) {
            LOG.warn("no implemenation found for " + method);
        }
        return result;
    }

    /**
     * toString
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "MultipleInheritanceProxy: " + delegates;
    }

}
