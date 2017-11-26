/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 13.05.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.tools.ant.taskdefs.Classloader;

import de.tsl2.nano.core.util.StringUtil;

/**
 * is able to find all types of java elements like classes, annotations, methods, fields - per reflection.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ClassFinder {
    private Set<Class<?>> classes;

    public ClassFinder() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * constructor
     */
    public ClassFinder(ClassLoader classLoader) {
        classes = new HashSet<>();
        addClasses(ClassLoader.getSystemClassLoader(), classes);
        while ((classLoader = addClasses(classLoader, classes)) != null);
        System.out.println("ClassFinder created for " + classes.size() + " classes");
    }

    /**
     * addClasses
     * @param classLoader
     * @return parent ClassLoader
     */
    private static ClassLoader addClasses(ClassLoader classLoader, Set<Class<?>> classes) {
        classes.addAll((Collection<? extends Class<?>>) new PrivateAccessor(classLoader).member("classes", Vector.class));
        return (ClassLoader) new PrivateAccessor(classLoader).call("getParent", Classloader.class);
    }

    public <T> Collection<Class<T>> findClass(Class<T> base) {
        return (Collection<Class<T>>) fuzzyFind(null, base, -1, null).values();
    }

    public Class findClass(String filter) {
        Map<Double, Class> result = fuzzyFind(filter);
        return result.size() > 0 && result.containsKey(1d) ? result.get(1d) : null;
    }

    public <M extends Map<Double, Class>> M fuzzyFind(String filter) {
        return fuzzyFind(filter, Class.class, -1, null);
    }

    /**
     * finds all classes/methods/fields of the given classloader fuzzy matching the given filter, having the given
     * modifiers (or modifiers is -1) and having the given annotation (or annotation is null)
     * 
     * @param filter fuzzy filter
     * @param resultType (optional) restricts to search for classes/extensions , methods or fields. If it is
     *            {@link Method}, only method matches will be returned. if it is an interface, all matching
     *            implementations will be returned.
     * @param modifier (optional, -1: all) see {@link Modifier}.
     * @param annotation (optional) class/method/field annotation as constraint.
     * @return all found java elements sorted by matching quote down. best quote is 1.
     */
    public <T, M extends Map<Double, T>> M fuzzyFind(String filter,
            Class<T> resultType,
            int modifier,
            Class<? extends Annotation> annotation) {
        Map<Double, T> result = new TreeMap<Double, T>() {
            @Override
            public T put(Double key, T value) {
                while (containsKey(key))
                    key += 0000000001;
                return super.put(key, value);
            }
        };
        Class cls;
        double match;
        boolean addMethods = resultType == null || Method.class.isAssignableFrom(resultType);
        boolean addFields = resultType == null || Field.class.isAssignableFrom(resultType);
        boolean addClasses =
            resultType == null || Class.class.isAssignableFrom(resultType) || (!addMethods && !addFields);
        //clone the classes vector to avoid concurrent modification - when the classloader is working
        for (Iterator<Class<?>> it = /*((Vector<Class<?>>) */classes/*.clone())*/.iterator(); it.hasNext();) {
            cls = it.next();
            if (addClasses) {
                if ((modifier < 0 || cls.getModifiers() == modifier)
                    && (annotation == null || cls.getAnnotation(annotation) != null)) {
                    match = filter != null ? StringUtil.fuzzyMatch(cls.getName(), filter) : 1;
                    if (match > 0) {
                        if (resultType == null || Class.class.isAssignableFrom(resultType)
                            || resultType.isAssignableFrom(cls))
                            if (!cls.equals(resultType)) //don't return the base class itself
                                result.put(match, (T) cls);
                    }
                }
            }
            if (addMethods) {
                result.putAll((Map<Double, T>) fuzzyFindMethods(cls, filter, modifier, annotation));
            }
            if (addFields) {
                result.putAll((Map<Double, T>) fuzzyFindFields(cls, filter, modifier, annotation));
            }
        }
        return (M) result;
    }

    public Map<Double, Method> fuzzyFindMethods(Class cls,
            String filter,
            int modifier,
            Class<? extends Annotation> annotation) {
        HashMap<Double, Method> map = new HashMap<Double, Method>() {
            @Override
            public Method put(Double key, Method value) {
                while (containsKey(key))
                    key += 0000000001;
                return super.put(key, value);
            }
        };
        Method[] methods = Modifier.isPublic(modifier) ? cls.getMethods() : cls.getDeclaredMethods();
        double match;
        for (int i = 0; i < methods.length; i++) {
            if ((modifier < 0 || methods[i].getModifiers() == modifier)
                && (annotation == null || methods[i].getAnnotation(annotation) != null)) {
                match = StringUtil.fuzzyMatch(methods[i].toGenericString(), filter);
                if (match > 0)
                    map.put(match, methods[i]);
            }
        }
        return map;
    }

    public Map<Double, Field> fuzzyFindFields(Class cls,
            String filter,
            int modifier,
            Class<? extends Annotation> annotation) {
        HashMap<Double, Field> map = new HashMap<Double, Field>() {
            @Override
            public Field put(Double key, Field value) {
                while (containsKey(key))
                    key += 0000000001;
                return super.put(key, value);
            }
        };
        Field[] fields = Modifier.isPublic(modifier) ? cls.getFields() : cls.getDeclaredFields();
        double match;
        for (int i = 0; i < fields.length; i++) {
            if ((modifier < 0 || fields[i].getModifiers() == modifier)
                && (annotation == null || fields[i].getAnnotation(annotation) != null)) {
                match = StringUtil.fuzzyMatch(fields[i].toGenericString(), filter);
                if (match > 0)
                    map.put(match, fields[i]);
            }
        }
        return map;
    }

}
