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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * Should only be used by framework developers</p> To be independent of other libraries you may use this class - using
 * reflection to get fields and methods . To enhance performance, members and methods are cached for that instance. To
 * disable the member cache, call {@link #setMemberCache(Map)}.
 * 
 * <pre>
 * Simple Example:<br/>
 * new UnboundAccessor<Permission>(this).member("myfieldname", String.class);
 * <p/>
 * Complex Example:<br/>
 * ...
 * pa new UnboundAccessor<Permission>(this);
 * pa.registerMethod("mymethodname", new Class[]{String.class}, false);
 * ...
 * pa.call("mymethodname", "myparameter1");
 * </pre>
 * 
 * @author ts
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class UnboundAccessor<T> {
    private static final Log LOG = LogFactory.getLog(UnboundAccessor.class);
    /** instance to access (not-)inherited members and methods (privates too) */
    T instance;

    /** caches the access to member objects, if {@link #useMemberCache} is true */
    Map<String, Object> memberCache;
    /** caches the access to method objects. */
    Map<String, Method> methodCache;
    /** enables {@link #memberCache} cache. */
    boolean useMemberCache;
    static Object NULL = new Object() { public String toString() {return "<empty>";};};

    /**
     * {@link #useMemberCache} will be false.
     * 
     * @param instance to be accessible through reflection
     */
    public UnboundAccessor(T instance) {
        this(instance, false);
    }

    /**
     * constructor
     * 
     * @param instance to be accessible through reflection
     * @param useMemberCache activate the {@link #memberCache}. see {@link #useMemberCache}. It is possible to activate
     *            the {@link #memberCache} later through {@link #setMemberCache(Map)}.
     */
    public UnboundAccessor(T instance, boolean useMemberCache) {
        super();
        this.instance = instance;
        this.useMemberCache = useMemberCache;
        this.methodCache = new Hashtable<String, Method>();
        if (useMemberCache) {
            this.memberCache = new Hashtable<String, Object>();
        }
    }

    /**
     * instance
     * 
     * @return the unwrapped instance
     */
    public T instance() {
        return instance;
    }

    /**
     * delegates to {@link #memberNames(List, Class, Annotation...)}.
     * 
     * @param havingAnnotations
     * @return all members of accessing class - inclusive super-classes, having at least one the given annotation-types
     */
    public <A extends Annotation> List<String> memberNames(Class<A>... havingAnnotations) {
        return memberNames(new ArrayList<String>(), accessibleInstance().getClass(), havingAnnotations);
    }

    protected <A extends Annotation> List<String> memberNames(List<String> memberNames,
            Class<? extends Object> cls,
            Class<A>... havingAnnotations) {
        if (cls.getSuperclass() != null) {
            memberNames(memberNames, cls.getSuperclass(), havingAnnotations);
        }
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            Annotation[] annotations = field.getAnnotations();
            Class[] annotationTypes = new Class[annotations.length];
            for (int i = 0; i < annotations.length; i++) {
                annotationTypes[i] = annotations[i].annotationType();
            }
            if (Util.contains(annotationTypes, havingAnnotations)) {
                memberNames.add(field.getName());
            }
        }
        return memberNames;
    }

    /**
     * all member values (for debugging). delegates to {@link #members(Class)}.
     * 
     * @return all members of all instance class and all super-classes
     */
    public Map<String, Object> members(String...names) {
        if (memberCache == null) {
            useMemberCache = true;
            setMemberCache(new LinkedHashMap<String, Object>());
        }
        return members(this.accessibleInstance().getClass(), names);
    }

    /**
     * all member values (for debugging)
     * 
     * @param cls class to evaluate
     * @return all members of given class and all super-classes
     */
    protected Map<String, Object> members(Class<? extends Object> cls, String...names) {
        if (cls.getSuperclass() != null) {
            members(cls.getSuperclass(), names);
        }
        Field[] fields = cls.getDeclaredFields();
        List nameList = names.length > 0 ? Arrays.asList(names) : null;
        for (Field field : fields) {
            if (nameList == null || nameList.contains(field.getName()))
                member(field.getName(), field.getType());
        }
        return memberCache;
    }

    /**
     * delegates to {@link #member(String, Class)} using type Object.
     */
    public Object member(String name) {
        return member(name, Object.class);
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
            if (cf != null) {
                return (M) (cf == NULL ? null : cf);
            }
        }
        try {
            Field f = getField(name);
            f.setAccessible(true);
            if (useMemberCache) {
                M value = (M) f.get(accessibleInstance());
                memberCache.put(name, (value != null ? value : NULL));
                return value;
            } else {
                return (M) f.get(accessibleInstance());
            }
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * set a field/member value
     * 
     * @param memberName member to change
     * @param newValue new member value
     */
    public void set(String memberName, Object newValue) {
        try {
            if (LOG.isTraceEnabled())
                LOG.trace("changing field " + instance.getClass().getName() + "." + memberName + ": " + newValue);
            Field f = getField(memberName);
            f.setAccessible(true);
            //TODO: refactor to avoid access to BeanAttribute
            f.set(instance, BeanAttribute.wrap(newValue, f.getType()));
            if (useMemberCache) {
                memberCache.put(memberName, newValue != null ? newValue : NULL);
            }
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * typeOf
     * 
     * @param name
     * @return
     */
    public Class typeOf(String name) {
        try {
            Field f = getField(name);
            return f != null ? f.getType() : getMethod(name, new Class[0]).getReturnType();
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * this method returns the instance itself. extending classes may override this to return an object that will be
     * more accessible than the instance itself (perhaps an instance of a super class).
     * 
     * @param instance2
     * @return instance2 itself
     */
    protected Object accessibleInstance() {
        return instance;
    }

    /**
     * hasMember
     * @param name field/member name
     * @return true, if field/member with given name exists
     */
    public boolean hasMember(String name) {
        try {
            return getField(name) != null;
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * evaluates the desired field. see {@link Class#getField(String)}
     * 
     * @param name field name
     * @return field
     * @throws Exception if field not accessible
     */
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
            if (m == null) {
                m = registerMethod(name, par, true);
            }
            return (M) m.invoke(instance, args);
        } catch (Exception e) {
            ManagedException.forward(e);
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
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * evaluates the desired method. see {@link Class#getMethod(String, Class...)}
     * 
     * @param name method name
     * @param method parameters
     * @return method
     * @throws Exception if method not accessible
     */
    protected Method getMethod(String name, Class[] par) throws Exception {
        return instance.getClass().getMethod(name, par);
    }

    private String getMethodID(String name, Class[] par) {
        return name + StringUtil.toString(par);
    }

    /**
     * if you have already used members you can cache them here. {@link #useMemberCache} will be true to activate the
     * {@link #memberCache}.
     * 
     * @param memberCache The memberCache to set.
     */
    public void setMemberCache(Map<String, Object> memberCache) {
        this.memberCache = memberCache;
    }

    @Override
    public String toString() {
        return Util.toString(getClass(), instance);
    }
}
