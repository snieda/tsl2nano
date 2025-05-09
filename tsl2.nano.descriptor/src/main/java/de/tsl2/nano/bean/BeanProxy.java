/*
 * Copyright © 2002-2008 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.bean;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.BeanProperty;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.util.DelegationHandler;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.StringUtil;

/**
 * simple bean proxy mechanism to avoid implementations of bean-mocks. holds all bean properties in a map, returning
 * them by a call to the getter method, and setting them through a call of the setter.
 * <p>
 * 
 * if you define a delegate, the delegate will be preferred.
 * <p>
 * 
 * you are able to provide beanProperties on construction. this beanProperties may contain simple attribute values and
 * full method call results. there are two ways to store method call results:<br>
 * 1. store an {@link IAction} with the methods name as key<br>
 * 2. store an object with the methods name plus its arguments. use {@link #getMethodArgsId(Method, Object[])} to
 * evaluate the key for your method call result.
 * <p>
 * 
 * 'internal' bean properties can be yield or changed through the interface {@link BeanProperty}. Each {@link BeanProxy}
 * will implement this interface, too - to provide a simple enhancement of bean classes, without any implementation of
 * an extension. simply cast your proxy instance to {@link BeanProperty} to work on the property map.
 * <p>
 * for testing purposes, the proxy puts its last invocation arguments to the beanProperties. call
 * {@link #getLastInvokationArgs(Object, String)} or
 * <code>(({@link BeanProperty})myProxyInstance).get_({@link #PROPERTY_INVOKATION_INFO} + methodName)</code>
 * 
 * @author ts 15.12.2008
 * @version $Revision: 1.0 $
 */
@SuppressWarnings("unchecked")
public class BeanProxy<T> extends DelegationHandler<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -3486884118905162667L;
    /** if true, the property hashmap will be prefered to the delegate */
    boolean preferProperties = false;
    private boolean returnEmptyCollections = false;
    
    static/*final*/Method METHOD_GET_PROPERTY;
    static/*final*/Method METHOD_SET_PROPERTY;
    static {
        try {
            METHOD_GET_PROPERTY = BeanProperty.class.getMethod("get_", new Class[] { String.class });
            METHOD_SET_PROPERTY = BeanProperty.class.getMethod("set_",
                new Class[] { String.class, Object.class });
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
    }

    /** to be used to get the last proxy invocation - for test purpose only */
    static final String PROPERTY_INVOKATION_INFO = "BeanProxyInvocationInfo.";

    /**
     * Constructor
     */
    protected BeanProxy() {
        this(new HashMap<String, Object>(), false, (T)null);
    }

    /**
     * Constructor
     * 
     * @param beanProperties bean properties to be used as attribute values.
     * @param delegate see {@linkplain #delegate}, may be null
     */
    protected BeanProxy(Map<String, Object> beanProperties, boolean preferProperties, T delegate) {
        super(delegate, beanProperties);
        this.preferProperties = preferProperties;
    }

    protected BeanProxy(Map<String, Object> beanProperties, boolean preferProperties, Class<T>... interfazes) {
        super((T)null, beanProperties);
        this.preferProperties = preferProperties;
        this.interfaces = interfazes;
    }

    public static <T> T createBeanImplementation(Class<T> interfaze) {
        return createBeanImplementation(interfaze, null);
    }
    /**
     * creates a new proxy instance with BeanProxy as invocationhandler.
     * 
     * @param interfaze interface to implement
     * @param attributes map of bean attributes for this bean implementation
     * @return implementation of the given interface.
     */
    public static <T> T createBeanImplementation(Class<T> interfaze,
            Map<String, Object> attributes) {
        return createBeanImplementation(interfaze, attributes, null, false, null);
    }

    public static <T> T createBeanImplementation(Class<T> interfaze,
            Map<String, Object> attributes,
            T delegate,
            ClassLoader classLoader) {
        return createBeanImplementation(interfaze, attributes, delegate, false, classLoader);
    }

    /**
     * creates a new proxy instance with BeanProxy as invocationhandler.
     * 
     * @param interfaze interface to implement
     * @param attributes map of bean attributes for this bean implementation
     * @return implementation of the given interface.
     */
    @SuppressWarnings({ "rawtypes" })
    public static <T> T createBeanImplementation(Class<T> interfaze,
            Map<String, Object> attributes,
            T delegate,
            boolean preferProperties,
            ClassLoader classLoader) {
        if (classLoader == null)
            classLoader = Thread.currentThread().getContextClassLoader();
        return (T) Proxy.newProxyInstance(classLoader,
            new Class[] { interfaze, BeanProperty.class },
            delegate != null ? new BeanProxy(attributes, preferProperties, delegate)
            				 : new BeanProxy<T>(attributes, preferProperties, interfaze));
    }

    public static void setReturnEmptyCollections(Object proxy, boolean returnEmptyCollections) {
        assert Proxy.isProxyClass(proxy.getClass());
        ((BeanProxy)((Proxy)proxy).getInvocationHandler(proxy)).setReturnEmptyCollections(returnEmptyCollections);
    }
    /**
     * @param returnEmptyCollections the returnEmptyCollections to set
     */
    public void setReturnEmptyCollections(boolean returnEmptyCollections) {
        this.returnEmptyCollections = returnEmptyCollections;
    }

    /**
     * if the method is a bean attribute access method like a getter or a setter, it will be handled through the
     * {@link #beanProperties} map.
     * 
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;

        if (method.getName() == "getClass") {
            return proxy.getClass().getInterfaces()[0];
        } else if (!preferProperties && delegate != null && method.getDeclaringClass().isAssignableFrom(delegate.getClass())) {
            result = invokeDelegate(method, args);
        } else if (method.equals(METHOD_GET_PROPERTY)) {
            result = properties.get(args[0]);
        } else if (method.getName().startsWith(BeanAttribute.PREFIX_READ_ACCESS) && (args == null || args.length == 0)) {
            final String attributeName = BeanAttribute.getName(method);
            result = properties.get(attributeName);
            //auto-boxing - storing the default value in properties
            if (result == null && method.getReturnType().isPrimitive()) {
                result = BeanUtil.getDefaultValue(method.getReturnType());
                properties.put(attributeName, result);
            }
        } else if (method.equals(METHOD_SET_PROPERTY)) {
            //this spec. enables all values to be changed!
            properties.put((String) args[0], args[1]);
        } else if (method.getName().startsWith(BeanAttribute.PREFIX_WRITE_ACCESS) && args.length == 1) {
            final String attributeName = BeanAttribute.getNameFromSetter(method);
            properties.put(attributeName, args[0]);
        } else {
            /*
             * the interface defines a method call, that is not a getter or a setter.
             * on construction, the properties may contain an action with
             * key = method-name. otherwise it is possible to store full method call
             * with arguments in the properties.
             */
            result = properties.get(getMethodArgsId(method, args));
            if (result == null)
                result = properties.get(getMethodArgsId(method, new Object[0]));
            if (result == null)
                result = properties.get(method.getName());
            if (result instanceof IAction) {
                result = ((IAction)result).activate();
            } else if (result instanceof BiFunction) {
                result = ((BiFunction)result).apply(method, args);
            }
            //auto-boxing - storing the default value in properties
            result = getDefaultPrimitive(method, args, result);
        }

        //not preferring properties but didn't find the property, try the delegate
        if (preferProperties && result == null && delegate != null && method.getDeclaringClass().isAssignableFrom(delegate.getClass()))
            result = invokeDelegate(method, args);
        
       if (result == null && Collection.class.isAssignableFrom(method.getReturnType()) && returnEmptyCollections) {
           result = new ListSet<>();
       }
        //for test purpose, we provide last invokation as property
        properties.put(PROPERTY_INVOKATION_INFO + method.getName(), getMethodArgsId(method, args));

        return result;
    }

    /**
     * getDefaultPrimitive
     * 
     * @param method method to return a result
     * @param args method args
     * @param result current result
     * @return if given result is null, returns a default immutable on primitive method return - otherwise the given
     *         result
     */
    public Object getDefaultPrimitive(Method method, Object[] args, Object result) {
        //auto-boxing - storing the default value in properties
        if (result == null && method.getReturnType().isPrimitive()) {
            result = BeanUtil.getDefaultValue(method.getReturnType());
            properties.put(getMethodArgsId(method, args), result);
        }
        return result;
    }

    /**
     * for test purpose only
     * 
     * @param proxyInstance proxy
     * @return last method args
     */
    public static String getLastInvokationArgs(Object proxyInstance, String methodName) {
        final String invInfo = (String) ((BeanProperty) proxyInstance).get_(PROPERTY_INVOKATION_INFO + methodName);
        return StringUtil.substring(invInfo, "(", ")", true);
    }

    /**
     * for testing with results on explizit method calls
     * 
     * Examples:
     *   BeanProxy.doReturnWhen(em, item, EntityManager.class.getMethod("createQuery", String.class));
     *   BeanProxy.doReturnWhen(ServiceFactory.getGenService(), item, "findByExample");
     */
    public static void doReturnWhen(Object proxyInstance, Object doReturn, Method whenMethod, Object...whenArgs) {
        doReturnWhen(proxyInstance, doReturn, getMethodArgsId(whenMethod, whenArgs));
    }
    public static void doReturnWhen(Object proxyInstance, BiFunction<Method, Object[], Object> doReturn, String methodNameAndArgs) {
        doReturnWhen(proxyInstance, (Object)doReturn, methodNameAndArgs);
    }
    public static void doReturnWhen(Object proxyInstance, Object doReturn, String methodNameAndArgs) {
        ManagedException.assertion(Proxy.isProxyClass(proxyInstance.getClass()), "no proxy: " + proxyInstance);
        ManagedException.assertion(BeanProxy.class.isAssignableFrom(Proxy.getInvocationHandler(proxyInstance).getClass()), 
            "no beanproxy: " + Proxy.getInvocationHandler(proxyInstance));
        
        ((BeanProperty)proxyInstance).set_(methodNameAndArgs, doReturn);
    }        
}