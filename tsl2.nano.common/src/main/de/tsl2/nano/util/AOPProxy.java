/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Nov 6, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.execution.ICRunnable;

/**
 * simple proxy to enable aop-like enhancement.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class AOPProxy<T> implements InvocationHandler {
    Object delegate;
    ICRunnable<?> before, after;
    boolean proxyResult;

    /**
     * constructor
     * 
     * @param delegate original instance
     * @param before (optional) action to execute. will be run with arguments instance, method, and args - before executing the original method.
     * @param after (optional) action to execute. will be run with arguments instance, method, and args - after executing the original method.
     * @param proxyResult whether to wrap the result into a new proxy (recursive).
     */
    public AOPProxy(Object delegate, ICRunnable<?> before, ICRunnable<?> after, boolean proxyResult) {
        this.delegate = delegate;
        this.before = before;
        this.after = after;
        this.proxyResult = proxyResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (before != null)
            before.run(null, delegate, method, args);
        Object result = method.invoke(delegate, args);
        if (after != null)
            after.run(null, delegate, method, args);
        if (method.getReturnType().isInterface()) {
            result = createEnhancement(result, before, after, proxyResult);
        }
        return result;
    }

    /**
     * createEnhancement
     * @param <T>
     * @param instance delegate
     * @param before (optional) action to execute. will be run with arguments instance, method, and args - before executing the original method.
     * @param after (optional) action to execute. will be run with arguments instance, method, and args - after executing the original method.
     * @param proxyResult whether to wrap the result into a new proxy (recursive).
     * @return
     */
    public static final <T> T createEnhancement(T instance, ICRunnable<?> before, ICRunnable<?> after, boolean proxyResult) {
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), BeanClass.getBeanClass(instance.getClass())
            .getInterfaces(), new AOPProxy(instance, before, after, proxyResult));
    }
}
