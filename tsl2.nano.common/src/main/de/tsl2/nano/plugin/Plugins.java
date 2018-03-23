/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 09.03.2018
 * 
 * Copyright: (c) Thomas Schneider 2018, all rights reserved
 */
package de.tsl2.nano.plugin;

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
public class Plugins {
    private static Plugins self = null;
    Map<Class<? extends Plugin>, Object> implementations = new HashMap<>();

    public static <T extends Plugin> T process(Class<T> pluginInterface) {
        return process(pluginInterface, DecorationProxy.class);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Plugin> T process(Class<T> pluginInterface, Class<? extends InvocationHandler> processingType) {
        if (self == null)
            self = new Plugins();
        Object dec = self.implementations.get(pluginInterface);
        if (dec == null) {
            dec = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[] { pluginInterface }, BeanClass.createInstance(processingType, pluginInterface));
            self.implementations.put(pluginInterface, dec);
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
