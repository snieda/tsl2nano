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
import java.util.Map;

import de.tsl2.nano.exception.ManagedException;

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
            if (cls.getSuperclass() != null)
                return getField(cls.getSuperclass(), name);
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
            if (cls.getSuperclass() != null)
                return getMethod(cls.getSuperclass(), name, par);
            ManagedException.forward(e);
            return null;
        }
    }
    
}
