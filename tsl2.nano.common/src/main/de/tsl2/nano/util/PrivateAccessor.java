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


/**
 * Should only be used by framework developers</p>
 * Some classes are not intended to be extended. Defining fields and methods as private, a normal extension isn't
 * possible. This class provides access to that private fields through reflection. To enhance performance, members and
 * methods are cached for that instance. To disable the member cache, call {@link #setMemberCache(Map)}.
 * 
 * @author ts
 * @version $Revision$
 */
public class PrivateAccessor<T> extends UnboundAccessor<T> {

    public PrivateAccessor(T instance) {
        super(instance);
    }

    @Override
    protected Field getField(String name) throws Exception {
        return instance.getClass().getDeclaredField(name);
    }

    @Override
    protected Method getMethod(String name, Class[] par) throws Exception {
        return instance.getClass().getDeclaredMethod(name, par);
    }
}
