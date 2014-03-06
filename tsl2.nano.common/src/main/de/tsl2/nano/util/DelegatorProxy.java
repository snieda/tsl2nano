/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Mar 7, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;

import de.tsl2.nano.log.LogFactory;

/**
 * generic delegator to be usable as base class for simple delegation implementations. the delegator doesn't have to
 * implement the given interface, but has to provide the desired implementation with a method thats name and arguments
 * have to match the interface method.
 * <p/>
 * if you give more than one delegator, they will be asked in the given order to have the desired method - to invoke it.
 * <p/>
 * if no delegator provides the called method, the standard proxy implementation will be used.
 * <p/>
 * the delegator is also usable as instance extender on runtime.
 * 
 * <pre>
 * example:
 *     public void setBeanFinder(final IBeanFinder<T, Object> beanFinder) {
 *         Object internalBeanFinder = new Object() {
 *             Collection<T> getData(T from, Object to) {
 *                 collection = (COLLECTIONTYPE) beanFinder.getData(from, to);
 *                 return collection;
 *             };
 *         };
 *         this.beanFinder = DelegatorProxy.delegator(IBeanFinder.class, internalBeanFinder, beanFinder);
 *     }
 * 
 * </pre>
 * 
 * To work directly on interface implementations, use {@link MultipleInheritanceProxy}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class DelegatorProxy implements InvocationHandler, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -1689089153253721181L;

    Object[] delegators;

    private static final Log LOG = LogFactory.getLog(DelegatorProxy.class);

    protected DelegatorProxy(Object... delegators) {
        this.delegators = delegators;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        for (int i = 0; i < delegators.length; i++) {
            Method delegatorMethod = getMethod(method, delegators[i]);
            if (delegatorMethod != null) {
                LOG.debug("delegating call of '" + method + "' to '" + delegatorMethod + "'");
                return delegatorMethod.invoke(delegators[i], args);
            }
        }
        //no delegator found for this method ? use the default return value.
        LOG.debug("no delegator found for '" + method + "'");
        return null;
    }

    /**
     * getMethod
     * 
     * @param method
     * @param delegator
     * @return
     */
    private Method getMethod(Method method, Object delegator) {
        Method delegatorMethod = null;
        try {
            delegatorMethod = delegator.getClass().getMethod(method.getName(), method.getParameterTypes());
            delegatorMethod.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            //no problem, try the declared methods...
            try {
                delegatorMethod = delegator.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
                delegatorMethod.setAccessible(true);
            } catch (NoSuchMethodException ex1) {
                //no problem, try the next delegator...
            }
        }
        return delegatorMethod;
    }

    /**
     * delegator
     * 
     * @param <T>
     * @param proxyInterface
     * @param delegators
     * @return
     */
    @SuppressWarnings("unchecked")
    public static final <T> T delegator(Class<T> proxyInterface, Object... delegators) {
        return ((T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class[] { proxyInterface },
            new DelegatorProxy(delegators)));
    }
    /**
     * delegator
     * 
     * @param <T>
     * @param proxyInterfaces
     * @param delegators
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static final Object delegator(Class[] proxyInterfaces, Object... delegators) {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            proxyInterfaces,
            new DelegatorProxy(delegators));
    }
}
