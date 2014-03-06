/*
 * Copyright © 2002-2008 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.serviceaccess;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedAction;

import de.tsl2.nano.Messages;
import de.tsl2.nano.exception.ManagedException;
import de.tsl2.nano.util.StringUtil;

/**
 * simple service proxy using the real service as a delegate.
 * 
 * @author ts 15.12.2008
 * @version $Revision: 1.0 $
 */
public class ServiceProxy<T> extends DefaultService implements InvocationHandler {
    /** delegate (real object) */
    protected T delegate;
    /** sometimes (perhaps on schedulers) we don't want to log every call */
    boolean doLog = true;

    /**
     * Constructor
     * 
     * @param delegate see {@linkplain #delegate}
     */
    protected ServiceProxy(T delegate) {
        super();
        assert delegate != null : "the service delegate must not be null";
        this.delegate = delegate;
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
        if (delegate == null || !interfaze.isAssignableFrom(delegate.getClass())) {
            throw new ManagedException("the delegate instance must implement the service interface!\ninterface: " + interfaze
                + "\ndelegate: "
                + Messages.stripParameterBrackets(String.valueOf(delegate))
                + "\n\nmostly the reason are missing appserver-client-libraries to your client.");
        }
        return (T) Proxy.newProxyInstance(classLoader, new Class[] { interfaze }, new ServiceProxy(delegate));
    }

    /**
     * if the method is a bean attribute access method like a getter or a setter, it will be handled through the
     * {@link #beanProperties} map.
     * 
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
        return doAs(getSubject(), new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    log("==> calling service: " + method.toGenericString()
                        + ", args: "
                        + StringUtil.toString(args, 100));
                    final Object result = method.invoke(delegate, args);
                    log("<== service " + method.toGenericString()
                        + " returned with: "
                        + StringUtil.toString(result, 100));
                    return result;
                } catch (final Exception e) {
                    Throwable t = e;
                    if (e instanceof InvocationTargetException) {
                        t = ((InvocationTargetException) e).getTargetException();
                    }
                    if (t instanceof UndeclaredThrowableException) {
                        t = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
                    }
                    LOG.error("ServiceProxy has an invoking-problem on delegate:" + delegate
                        + "\n    TargetException: "
                        + t, t);
                    return ManagedException.forward(t);
                }
            }
        });
    }

    /**
     * maybe overridden to write to anything else
     * 
     * @param string log text
     */
    protected void log(String string) {
        LOG.info(string);
    }

    /**
     * @return Returns the doLog.
     */
    public boolean isDoLog() {
        return doLog;
    }

    /**
     * @param doLog The doLog to set.
     */
    public void setDoLog(boolean doLog) {
        this.doLog = doLog;
    }

    /**
     * toString
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ServiceProxy: " + delegate.toString();
    }

    /**
     * convenience to turn off logging for the given service. see {@link #doLog} and {@link #setDoLog(boolean)}
     * @param service
     */
    @SuppressWarnings("rawtypes")
    public static final void dontLog(Object service) {
        LOG.info("turning off logging for service " + service);
        ((ServiceProxy)Proxy.getInvocationHandler((Proxy)service)).setDoLog(false);
    }
}
