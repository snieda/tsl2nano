/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 14.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;

/**
 * Should only be used by framework developers</p> Some classes are not intended to be extended. Defining fields and
 * methods as private, a normal extension isn't possible. This class provides access to that private fields through
 * reflection. To enhance performance, members and methods are cached for that instance. To disable the member cache,
 * call {@link #setMemberCache(Map)}.
 * <p/>
 * Not all private fields and private methods are accessible. The current {@link SecurityManager} may throw a
 * SecurityException. See {@link Field#setAccessible(boolean)} and {@link Method#setAccessible(boolean)}.
 * 
 * <pre>
 * Simple Example:<br/>
 * new PrivateAccessor<Permission>(this).member("myfieldname", String.class);
 * <p/>
 * Complex Example:<br/>
 * ...
 * pa new PrivateAccessor<Permission>(this);
 * pa.registerMethod("mymethodname", new Class[]{String.class}, false);
 * ...
 * pa.call("mymethodname", "myparameter1");
 * </pre>
 * 
 * Provides powerful but dangerous method and member filters. As last workaround for a framework developer the method
 * {@link #eval(String, Class, Class...)} tries to get a desired property from any object.
 * <p/>
 * For further informations, see {@link UnboundAccessor}.
 * 
 * @author ts
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class PrivateAccessor<T> extends UnboundAccessor<T> {

    /**
     * see {@link UnboundAccessor#UnboundAccessor(Object)}
     */
    public PrivateAccessor(T instance) {
        super(instance);
    }

    /**
     * see {@link UnboundAccessor#UnboundAccessor(Object, boolean)}
     */
    public PrivateAccessor(T instance, boolean useMemberCache) {
        super(instance, useMemberCache);
    }

    @Override
    protected Field getField(String name) throws Exception {
        return getField(instance.getClass(), name);
    }

    protected Field getField(Class<?> cls, String name) {
        try {
            return cls.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (cls.getSuperclass() != null) {
                return getField(cls.getSuperclass(), name);
            }
            ManagedException.forward(e);
            return null;
        }
    }

    @Override
    protected Method getMethod(String name, Class[] par) throws Exception {
        return getMethod(instance.getClass(), name, par);
    }

    protected Method getMethod(Class<?> cls, String name, Class[] par) {
        try {
            return cls.getDeclaredMethod(name, par);
        } catch (NoSuchMethodException e) {
            if (cls.getSuperclass() != null) {
                return getMethod(cls.getSuperclass(), name, par);
            }
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * tries to find the right method. calls all found methods to get the desired property. if no one was found, it
     * tries that again on the class fields.
     * 
     * @param nameExpression (optional) regular expression to match a method name
     * @param returnType (optional) desired return type
     * @param pars first or complete arguments that types have to match the methods parameter types.
     * @return desired property instance or null, if no method or field provides this property.
     */
    public <T> T eval(String nameExpression, Class<T> returnType, Object... pars) {
        Class[] args = getArgTypes(pars);
        Set<Method> methods = findMethod(nameExpression, returnType, args);
        for (Method m : methods) {
            try {
                m.setAccessible(true);
                return (T) m.invoke(instance, args);
            } catch (Exception e) {
                //Ok, try the next one...
            }
        }

        Set<Field> members = findMembers(nameExpression, returnType);
        for (Field f : members) {
            try {
                f.setAccessible(true);
                return (T) f.get(instance);
            } catch (Exception e) {
                //Ok, try the next one...
            }
        }
        return null;
    }

    private Class[] getArgTypes(Object[] pars) {
        Class[] args = new Class[pars.length];
        for (int i = 0; i < pars.length; i++) {
            args[i] = pars[i] != null ? pars[i].getClass() : Object.class;
        }
        return args;
    }

    /**
     * generic method finder. give a regular expression of methods name, its return type and some arg types to get a
     * filtered method list.
     * 
     * @param nameExpression (optional) regular expression to match a method name
     * @param returnType (optional) desired return type
     * @param args first or complete arguments that types have to match the methods parameter types.
     * @return set of found methods
     */
    protected Set<Method> findMethod(String nameExpression, Class returnType, Class... args) {
        Method[] methods = instance.getClass().getMethods();
        methods = CollectionUtil.concat(methods, instance.getClass().getDeclaredMethods());
        Set<Method> result = new LinkedHashSet<Method>();
        for (int i = 0; i < methods.length; i++) {
            if ((nameExpression == null || methods[i].getName().matches(nameExpression))
                && (returnType == null || BeanClass.isAssignableFrom(methods[i].getReturnType(), returnType))) {
                Class<?>[] parTypes = methods[i].getParameterTypes();
                boolean typesOk = true;
                for (int j = 0; j < args.length; j++) {
                    if (!(BeanClass.isAssignableFrom(parTypes[j], args[j]))) {
                        typesOk = false;
                        break;
                    }
                }
                if (typesOk)
                    result.add(methods[i]);
            }
        }
        return result;
    }

    /**
     * generic member finder. give a regular expression of methods name and its return type.
     * 
     * @param nameExpression (optional) regular expression to match a member name
     * @param returnType (optional) desired return type
     * @return set of found members
     */
    protected Set<Field> findMembers(String nameExpression, Class returnType) {
        Field[] fields = instance.getClass().getFields();
        fields = CollectionUtil.concat(fields, instance.getClass().getDeclaredFields());
        Set<Field> result = new LinkedHashSet<Field>();
        for (int i = 0; i < fields.length; i++) {
            if ((nameExpression == null || fields[i].getName().matches(nameExpression))
                && (returnType == null || BeanClass.isAssignableFrom(fields[i].getType(), returnType))) {
                result.add(fields[i]);
            }
        }
        return result;
    }

}
