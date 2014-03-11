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

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.StringUtil;

/**
 * If you have an interface, providing only getters, but for full bean access, you need setter methods, you can provide
 * the basic interface and a specific interface with the setter methods. these setter methods don't exist in your given
 * basic interface implementation, but the members exist. So, this proxy will access the members directly on calling the
 * specific interface setter methods.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class SetterExtenderPoxy implements InvocationHandler, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -1689089153253721181L;

    PrivateAccessor<?> accessor;

//    private static final Log LOG = LogFactory.getLog(SetterExtenderPoxy.class);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected SetterExtenderPoxy(Object delegator) {
        accessor = new PrivateAccessor(delegator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass().isAssignableFrom(BeanClass.getDefiningClass(accessor.instance().getClass()))) {
            return method.invoke(accessor.instance(), args);
        } else if (method.getDeclaringClass().isAssignableFrom(proxy.getClass()) && args.length == 1){
            accessor.set(StringUtil.substring(method.getName(), "set", null), args[0]);
            return null;
        } else {
            throw new UnsupportedOperationException("This proxy is not able to handle call: " + method.toGenericString());
        }
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
    public static final <T> T setterExtender(Class<T> proxyInterface, Object delegator) {
        return ((T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class[] { proxyInterface },
            new SetterExtenderPoxy(delegator)));
    }
    
    public static final Object instanceOf(Proxy proxy) {
        if (proxy == null)
            return null;
        return ((SetterExtenderPoxy)proxy.getInvocationHandler(proxy)).accessor.instance();
    }
}
