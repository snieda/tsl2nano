/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Sep 6, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.service.feature;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.Principal;

import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.serviceaccess.ServiceProxy;
import de.tsl2.nano.util.bean.BeanUtil;

/**
 * handles optional calls (features).<br>
 * feature proxy asks for a defined feature principal, before starting the call. if the desired feature principal is not
 * defined, the call will be ignored.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class FeatureProxy<T> extends ServiceProxy<T> {
    private final Class<T> interfaze;

    /**
     * constructor
     * 
     * @param delegate
     */
    protected FeatureProxy(Class<T> interfaze, T delegate) {
        super(delegate);
        this.interfaze = interfaze;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final String featureName = getFeatureName();
        if (!isEnabled() || delegate == null) {
            if (method.getReturnType().isPrimitive()) {
                return BeanUtil.getDefaultValue(method.getReturnType());
            } else {
                return null;
            }
        }

//        this check has to done on class loading and newInstance
//        if (delegate == null) {
//            throw new FormattedException("FEATURE IMPLEMENTATION '" + featureName
//                + "' IS NOT AVAILABLE! PLEASE CHECK YOUR INSTALLATION.");
//        }
        LOG.info("=====> STARTING FEATURE '" + featureName + "'");
        final Object result = super.invoke(proxy, method, args);
        LOG.info("=====> FEATURE " + featureName + "' RETURNED: " + result);
        return result;
    }

    /**
     * creates a new proxy instance with BeanProxy as invocationhandler.
     * 
     * @param interfaze interface to implement
     * @param attributes map of bean attributes for this bean implementation
     * @param classLoader class loader to load 'interfaze'.
     * @return implementation of the given interface.
     */
    public static <T> T createBeanImplementation(Class<T> interfaze, T delegate, ClassLoader classLoader) {
        assert delegate == null || interfaze.isAssignableFrom(delegate.getClass()) : "the delegate instance must implement the interfaze!";
        return (T) Proxy.newProxyInstance(classLoader, new Class[] { interfaze }, new FeatureProxy(interfaze, delegate));
    }

    /**
     * getFeatureName
     * 
     * @return simple interface name minus 'I'.
     */
    private String getFeatureName() {
        //simple interface name minus 'I'.
        return interfaze.getSimpleName().substring(1);
    }

    /**
     * isEnabled
     * 
     * @return true, if feature was enabled (see subject and principal definition).
     */
    public boolean isEnabled() {
        final String featureName = getFeatureName();
        final Principal principal = new Feature(featureName);
        if (!ServiceFactory.instance().hasPrincipal(principal)) {
            LOG.info("Feature not available: " + principal.getName());
            return false;
        }
        return true;
    }
}
