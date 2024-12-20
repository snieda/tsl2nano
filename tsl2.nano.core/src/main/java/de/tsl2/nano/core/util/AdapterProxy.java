/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Mar 7, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core.util;

import static de.tsl2.nano.core.cls.PrimitiveUtil.getDefaultValue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.PrimitiveUtil;

/**
 * generic adapter to be usable as base class for interface implementations. get/put any values in a map.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class AdapterProxy implements InvocationHandler {
    Map<String, Object> values;
    boolean toStringRecursion;
    
    AdapterProxy(Map<String, Object> values) {
        this.values = values != null ? values : new HashMap<>();
    }

    public static final <T> T create(Class<T> interfaze) {
        return create(interfaze, null);
    }
    @SuppressWarnings("unchecked")
    public static final <T> T create(Class<T> interfaze, Map<String, Object> values) {
        return ((T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class[] { interfaze }, new AdapterProxy(values)));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!Util.isEmpty(args)) {
            Object v = values.get(method.getName());
            if (v != null && method.getReturnType().isAssignableFrom(v.getClass()))
                return v;
            else if (method.getName().equals("equals") && method.getReturnType().equals(boolean.class)
                    && method.getParameterCount() == 1 && args.length == 1)
                return args[0] == proxy;
            else if (!BeanAttribute.isGetterMethod(method) && !method.getDeclaringClass().equals(Object.class))
                values.put(BeanAttribute.getNameFromSetter(method), args.length == 1 ? args[0] : args);
        } else if (method.getName().equals("toString"))
        	return toProxyString(proxy);
        if (BeanAttribute.isGetterMethod(method)) {
            Object v = values.get(BeanAttribute.getName(method));
            if (v == null) {
                v = values.get(method.getName());
            }
            if ((v instanceof Object[]) && !Object[].class.isAssignableFrom(method.getReturnType()) )
                return findReturnValue(method, (Object[]) v, args);
            if (v != null && PrimitiveUtil.isAssignableFrom(method.getReturnType(), v.getClass()))
                return v;
        }
        return method.getDefaultValue() != null ? method.getDefaultValue() : method.getReturnType().isPrimitive() ? getDefaultValue(method.getReturnType()): null;
    }

	private String toProxyString(Object proxy) {
		//avoid stackoverflow on having member pointing to itself
		if (toStringRecursion)
			return toString();
		else {
			try {
				toStringRecursion = true;
                return "{\"" + proxy.getClass().getInterfaces()[0].getSimpleName() + "("
                        + proxy.getClass().getSimpleName() + ":" + getClass().getSimpleName() + ")\""
						+ ": " + MapUtil.toJSon(values) + "}";
			} finally {
				toStringRecursion = false;
			}
		}
	}
    
    private Object findReturnValue(Method method, Object[] values, Object[] args) {
        if (Util.isEmpty(args)) {
            for (Object o : values) {
                if (o != null && method.getReturnType().isAssignableFrom(o.getClass()))
                    return o;
            }
        } else {
            for (Object o : values) {
                for (Object a : args) { //we compare the object references!
                    if (o != null && o != a && method.getReturnType().isAssignableFrom(o.getClass()))
                        return o;
                }
            }
        }
        return null;
    }

    public Map<String, Object> values() {
        return values;
    }
    
    public static Map<String, Class> getValueTypes(Class interfaze) {
    	Method[] methods = CollectionUtil.concat(interfaze.getMethods(), interfaze.getDeclaredMethods());
    	Map<String,Class> map = new HashMap<>();
    	for (int i = 0; i < methods.length; i++) {
            if (!void.class.isAssignableFrom(MethodUtil.getExplicitType(methods[i])))
                map.put(methods[i].getName(), MethodUtil.getExplicitType(methods[i]));
		}
    	return map;
    }
}
