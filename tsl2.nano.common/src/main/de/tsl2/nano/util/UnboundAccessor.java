/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 13.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import de.tsl2.nano.exception.ForwardedException;

/**
 * Should only be used by framework developers</p> To be independent of other libraries you may use this class - using
 * reflection to get fields and methods . To enhance performance, members and methods are cached for that instance. To
 * disable the member cache, call {@link #setMemberCache(Map)}.
 * 
 * @author ts
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class UnboundAccessor<T> {
    /** instance to access (not-)inherited members and methods (privates too) */
    T instance;

    Map<String, Object> memberCache;
    Map<String, Method> methodCache;
    boolean useMemberCache;
    static Object NULL = new Object();

    /**
     * constructor
     * 
     * @param instance to be full accessible
     */
    public UnboundAccessor(T instance) {
        super();
        this.instance = instance;
    }

    /**
     * returns any member of all super classes of {@link #instance}
     * 
     * @param name field name
     * @param memberType field type (if {@link Object}.class, you have to cast the returning object by yourself)
     * @return fields value
     */
    public <M> M member(String name, Class<M> memberType) {
        if (useMemberCache) {
            Object cf = memberCache.get(name);
            if (cf != null)
                return (M) (cf == NULL ? null : cf);
        }
        try {
            Field f = getField(name);
            f.setAccessible(true);
            if (useMemberCache) {
                M value = (M) f.get(this);
                memberCache.put(name, value != null ? value : NULL);
                return value;
            } else {
                return (M) f.get(this);
            }
        } catch (Exception e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    protected Field getField(String name) throws Exception {
        return instance.getClass().getField(name);
    }

    /**
     * calls a registered method (register a method through {@link #registerMethod(String, Class...)}.
     * 
     * @param name method name
     * @param returnType methods return type (if {@link Object}.class, you have to cast the returning object by
     *            yourself)
     * @param args method call arguments
     * @return call result
     */
    public <M> M call(String name, Class<M> returnType, Object... args) {
        return call(name, returnType, null, args);
    }

    /**
     * call any method of all super classes of {@link #instance}
     * 
     * @param name method name
     * @param returnType methods return type (if {@link Object}.class, you have to cast the returning object by
     *            yourself)
     * @param par methods parameters
     * @param args calling arguments
     * @return methods result
     */
    public <M> M call(String name, Class<M> returnType, Class[] par, Object... args) {
        String methodID = par != null ? getMethodID(name, par) : name;
        Method m = methodCache.get(methodID);
        try {
            if (m == null)
                m = registerMethod(name, par, true);
            return (M) m.invoke(this, args);
        } catch (Exception e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    /**
     * register a method to be callable later without parameter definition
     * 
     * @param name method
     * @param par methods parameters
     * @return registered method
     */
    public void registerMethod(String name, Class... par) {
        registerMethod(name, par, false);
    }

    Method registerMethod(String name, Class[] par, boolean useParInMethodID) {
        Method m;
        try {
            m = getMethod(name, par);
            m.setAccessible(true);
            methodCache.put(useParInMethodID ? getMethodID(name, par) : name, m);
            return m;
        } catch (Exception e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    protected Method getMethod(String name, Class[] par) throws Exception {
        return instance.getClass().getMethod(name, par);
    }

    private String getMethodID(String name, Class[] par) {
        return name + StringUtil.toString(par);
    }

    /**
     * @param memberCache The memberCache to set.
     */
    public void setMemberCache(Map<String, Object> memberCache) {
        this.memberCache = memberCache;
    }
}
