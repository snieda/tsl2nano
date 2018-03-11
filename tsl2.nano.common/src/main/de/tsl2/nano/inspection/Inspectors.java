/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 09.03.2018
 * 
 * Copyright: (c) Thomas Schneider 2018, all rights reserved
 */
package de.tsl2.nano.inspection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Is able to find all implementations of an interface and delegates each call to all loaded implementations to do an
 * inspections or decoration.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Inspectors {
    private static Inspectors self = null;
    Map<Class<? extends Inspector>, Object> implementations = new HashMap<>();

    public static <T extends Inspector> T process(Class<T> inspectorInterface) {
        return process(inspectorInterface, DecorationProxy.class);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Inspector> T process(Class<T> inspectorInterface, Class<? extends InvocationHandler> processingType) {
        if (self == null)
            self = new Inspectors();
        Object dec = self.implementations.get(inspectorInterface);
        if (dec == null) {
            dec = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[] { inspectorInterface }, BeanClass.createInstance(processingType, inspectorInterface));
            self.implementations.put(inspectorInterface, dec);
        }
        return (T) dec;
    }

    /**
     * instances of provided implementations for the given interface
     */
    public static <T> List<T> getImplementations(Class<T> interfaze) {
        Collection<Class<T>> implClasses = ClassFinder.self().findClass(interfaze);
        ArrayList<T> handler = new ArrayList<>(implClasses.size());
        log("implementations for " + interfaze + "\n" + StringUtil.toFormattedString(implClasses, -1));
        for (Class<T> implClass : implClasses) {
            handler.add(BeanClass.createInstance(implClass));
        }
        return handler;
    }
    
    static final void log(Object txt) {
        System.out.println(txt);
    }

}
