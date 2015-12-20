/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 21.11.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;

/**
 * base handler using a delegate to enhance. optional attribute properties (key=attributeName, value=attributeValue)
 * will overrule the delegate.
 * 
 * @author Tom
 * @version $Revision$
 */
public class DelegationHandler<T> implements IDelegationHandler<T>, Serializable, Cloneable {
    /** serialVersionUID */
    private static final long serialVersionUID = -6883144560678494837L;
    private static final Log LOG = LogFactory.getLog(DelegationHandler.class);
    
    /** values for this proxy to use - overruling the {@link #delegate} */
    @ElementMap(attribute = true, inline = true, keyType = String.class, required = false)
    protected Map<String, Object> properties;
    /** optional delegate (real object) */
    @Element(required = false)
    protected T delegate;

    /**
     * constructor
     */
    protected DelegationHandler() {
        super();
    }

    /**
     * constructor
     * 
     * @param interfaze type to create an empty delegate through a proxy
     * @param properties attribute properties. String/Object pairs.
     */
    @SuppressWarnings("unchecked")
    public DelegationHandler(T delegate, Object...properties) {
        this(delegate, properties.length > 0 ? MapUtil.asMap(properties) : null);
    }

    /**
     * convenience constructor wrapping given properties into a property map and delegating to
     * {@link #DelegationHandler(T, Map)}
     * 
     * @param interfaze type to create an empty delegate through a proxy
     * @param properties attribute properties. String/Object pairs.
     */
    @SuppressWarnings("unchecked")
    public DelegationHandler(Class<T> interfaze, Object... properties) {
        this(interfaze, MapUtil.asMap(properties));
    }

    /**
     * constructor
     * 
     * @param interfaze type to create an empty delegate through a proxy
     * @param properties attribute properties
     */
    @SuppressWarnings("unchecked")
    public DelegationHandler(Class<T> interfaze, Map<String, Object> properties) {
        this((T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] { interfaze },
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    return null;
                }
            }), properties);
    }

    /**
     * constructor
     * 
     * @param delegate
     * @param properties
     */
    public DelegationHandler(T delegate, Map<String, Object> properties) {
        super();
        if (delegate == null)
            throw new IllegalArgumentException("delegate must not be null!");
        this.delegate = delegate;
        this.properties = properties != null ? properties : getProperties();
    }

    @Override
    public T getDelegate() {
        return delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object p;
        if ((p = getProperties().get(getMethodArgsId(method, args))) != null)
            return p;
        else if ((p = properties.get(method.getName())) != null)
            return p;
        else if (canDelegate(method, args))
            return invokeDelegate(method, args);
        else
            return null;
    }

    protected boolean canDelegate(Method method, Object[] args) {
        return !Proxy.isProxyClass(delegate.getClass());
    }

    protected Object invokeDelegate(Method method, Object[] args) throws IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException {
        return method.invoke(getDelegate(), args);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class[] getInterfaces() {
        return getDelegate().getClass().getInterfaces();
    }

    protected Map<String, Object> getProperties() {
        if (properties == null)
            properties = new HashMap<String, Object>();
        return properties;
    }
    /**
     * creates an id for the given method and its calling arguments
     * 
     * @param method method
     * @param args calling arguments
     * @return new key to be stored in a map
     */
    public static String getMethodArgsId(Method method, Object[] args) {
        return method.getDeclaringClass() + "."
            + method.getName()
            + "("
            + StringUtil.toString(args, Integer.MAX_VALUE)
            + ")";
    }

    /**
     * setProperty
     * 
     * @param key key
     * @param value value
     */
    public void setProperty(String key, Object value) {
        getProperties().put(key, value);
    }

    @Commit
    protected void initDeserialization() {

    }

    @Override
    public Object clone() {
        try {
            LOG.debug("cloning handler " + this);
            return super.clone();
        } catch (CloneNotSupportedException e) {
            ManagedException.forward(e);
            return null;
        }
    }
    @Override
    public String toString() {
        return Util.toString(getClass(), "delegate: " + getDelegate(), "interfaces: " + getInterfaces(), "properties: " + properties);
    }
    
    /**
     * createProxy
     * 
     * @param invocationHandler
     * @return
     */
    @SuppressWarnings("unchecked")
    public static final <T> T createProxy(DelegationHandler<T> invocationHandler) {
        LOG.debug("creating proxy for handler: " + invocationHandler);
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            BeanClass.getBeanClass(invocationHandler.getDelegate().getClass())
                .getInterfaces(), invocationHandler);
    }
}
