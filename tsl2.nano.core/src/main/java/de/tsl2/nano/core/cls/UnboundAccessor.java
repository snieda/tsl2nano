/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 13.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core.cls;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ObjectUtil;
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
    private T instance;

    /** caches the access to member objects, if {@link #useMemberCache} is true */
    Map<String, Object> memberCache;
    /** caches the access to method objects. */
    Map<String, Method> methodCache;
    /** enables {@link #memberCache} cache. */
    boolean useMemberCache;
    /** if true, no local/anonymous/enhancing classes will be used, but the known public class instead */
	private boolean useDefiningClass;
    public static Object NULL = new Object() {
        public String toString() {
            return "<empty>";
        };
    };

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

    /** see {@link #useDefiningClass} */
    public <A extends UnboundAccessor<T>> A setUseDefiningClass(boolean useDefiningClass) {
		this.useDefiningClass = useDefiningClass;
		return (A) this;
	}
    
    /**
     * this method returns the instance itself. extending classes may override this to return an object that will be
     * more accessible than the instance itself (perhaps an instance of a super class).
     * 
     * @return instance itself
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
        return memberNames(new ArrayList<String>(), clazz(), havingAnnotations);
    }

	protected Class<T> clazz() {
		return useDefiningClass ? BeanClass.getDefiningClass(instance) : (Class<T>) instance.getClass();
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
            if (Util.isEmpty(havingAnnotations) || Util.contains(annotationTypes, havingAnnotations)) {
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
    public Map<String, Object> members(String... names) {
        if (memberCache == null) {
            useMemberCache = true;
            setMemberCache(new LinkedHashMap<String, Object>());
        }
        return members(clazz(), names);
    }

    /**
     * all member values (for debugging)
     * 
     * @param cls class to evaluate
     * @return all members of given class and all super-classes
     */
    protected Map<String, Object> members(Class<? extends Object> cls, String... names) {
        if (cls.getSuperclass() != null) {
            members(cls.getSuperclass(), names);
        }
        Field[] fields = cls.getDeclaredFields();
        List nameList = names.length > 0 ? Arrays.asList(names) : null;
        for (Field field : fields) {
        	if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
        		continue;
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
     * returns any member of all super classes of {@link #instance}. member may be a path, separated by '.'.
     * 
     * @param name field name or path
     * @param memberType field type (if {@link Object}.class, you have to cast the returning object by yourself)
     * @return fields value or null , if path is broken by a null value
     */
    public <M> M member(String name, Class<M> memberType) {
        if (useMemberCache) {
            Object cf = memberCache.get(name);
            if (cf != null) {
                return (M) (cf == NULL ? null : cf);
            }
        }
        Object v = member(instance(), path(name));
        if (useMemberCache) {
            memberCache.put(name, (v != null ? v : NULL));
        }
        return (M) v;
    }

    /**
     * member of given instance walking through the given path of member names.
     * 
     * @param instance root instance
     * @param path member path starting from instance
     * @return value or null, if not available through path
     */
    public Object member(Object instance, String... path) {
        try {
            String name = path[0];
            Field f = getField(instance.getClass(), name);
            f.setAccessible(true);
            Object v = f.get(instance);
            return path.length > 1 && v != null ? member(v, Arrays.copyOfRange(path, 1, path.length)) : v;
        } catch (Exception e) {
            ManagedException.forward(e, false);
            return null;
        }
    }

    /**
     * set a field/member value. given member name may be path separated by '.'
     * 
     * @param memberName member to change. may be a path separated by '.'.
     * @param newValue new member value
     */
    public void set(String memberName, Object newValue) {
        if (LOG.isTraceEnabled())
            LOG.trace("changing field " + clazz().getName() + "." + memberName + ": " + newValue);
        set(instance(), newValue, path(memberName));
        if (useMemberCache) {
            memberCache.put(memberName, newValue != null ? newValue : NULL);
        }
    }

    /**
     * sets a new value for given member path starting from given instance
     * 
     * @param instance root instance to walk through given path from
     * @param newValue new value to set on end of path
     * @param path member path, starting on given instance class
     */
    public void set(Object instance, Object newValue, String... path) {
        try {
            instance = path.length > 1 ? member(instance, Arrays.copyOfRange(path, 0, path.length - 1)) : instance;
            Field f = getField(instance.getClass(), path[path.length - 1]);
            f.setAccessible(true);
            //TODO: refactor to avoid access to BeanAttribute
            f.set(instance, ObjectUtil.wrap(newValue, f.getType()));
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * @return delegates to {@link #typeOf(Class, String...)} using {@link #instance()} and {@link #path(String)}
     */
    public Class typeOf(String name) {
        return typeOf(clazz(), path(name));
    }

    /**
     * evaluates the type of the value defined by path starting from class. the class holding the last value in the path
     * defines the type - which may often be an interface.
     * 
     * @param cls class to start the path from
     * @param path field/member path
     * @return type of value defined by class holding the last item of the path.
     */
    public Class typeOf(Class cls, String... path) {
        try {
            String name = path[0];
            Field f = getField(name);
            if (f != null && path.length > 1)
                return typeOf(f.getType(), Arrays.copyOfRange(path, 1, path.length));
            return f != null ? f.getType() : getMethod(name, new Class[0]).getReturnType();
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * resolves a path, separated by '.' to a string array
     * 
     * @param name name to be separated
     * @return separated path entries
     */
    public static final String[] path(String name) {
        return name.split("\\.");
    }

    public boolean hasMember(String path) {
        return forceMember(path) != null;
    }
    
    /**
     * calls {@link #member(Object, String...)} and catches Exceptions. if member is not available, null will be
     * returned instead of throwing an exception.
     * 
     * @param name field/member path
     * @return value, if field/member with given name exists, or null on any exception
     */
    public Object forceMember(String... path) {
        try {
            return member(instance(), path);
        } catch (Exception e) {
            return null;
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
        return getField(clazz(), name);
    }

    protected Field getField(Class type, String name) throws NoSuchFieldException {
        return type.getField(name);
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
    	Class[] pars = null;
    	if (methodCache.get(name) == null) {
    		if (!Util.isEmpty(args)) {
    			pars = new Class[args.length];
    			for (int i = 0; i < args.length; i++) {
    				if (args[i] == null)
    					throw new IllegalArgumentException("please register method " + name + " with its parameters or give arguments that are not null!");
					pars[i] = args[i].getClass();
				}
    		} else
    			pars = new Class[0];
    	}
        return call(name, returnType, pars, args);
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
            ManagedException.forward(e, false);
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
        return clazz().getMethod(name, par);
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

    public void forEachMember(BiConsumer<String, Object> actor) {
    	members().forEach( (n, v) -> actor.accept(n, v));
    }
    
    @Override
    public String toString() {
        return Util.toString(getClass(), instance);
    }
}
