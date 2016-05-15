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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import de.tsl2.nano.core.util.StringUtil;

/**
 * is able to find all types of java elements like classes, annotations, methods, fields - per reflection.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class ClassFinder {
    private Vector<Class<?>> classes;

    /**
     * constructor
     */
    @SuppressWarnings("unchecked")
    public ClassFinder(ClassLoader classLoader) {
        classes = (Vector<Class<?>>) new PrivateAccessor(classLoader).member("classes", Vector.class);
    }

    /**
     * finds all classes/methods/fields of the given classloader fuzzy matching the given filter, having the given
     * modifiers (or modifiers is -1) and having the given annotation (or annotation is null)
     * 
     * @param filter fuzzy filter
     * @param resultType (optional) restricts to search for classes , methods or fields.
     * @param modifier (optional, -1: all) see {@link Modifier}.
     * @param annotation (optional) class/method/field annotation as constraint.
     * @return all found java elements.
     */
    @SuppressWarnings("unchecked")
    public <T, M extends Map<Double, T>> M fuzzyFind(String filter,
            Class<T> resultType,
            int modifier,
            Class<? extends Annotation> annotation) {
        Map<Double, T> result = new TreeMap<Double, T>();
        Class cls;
        double match;
        boolean addClasses = resultType == null || Class.class.isAssignableFrom(resultType);
        boolean addMethods = resultType == null || Method.class.isAssignableFrom(resultType);
        boolean addFields = resultType == null || Field.class.isAssignableFrom(resultType);
        //clone the classes vector to avoid concurrent modification - when the classloader is working
        for (Iterator<Class<?>> it = ((Vector<Class<?>>) classes.clone()).iterator(); it.hasNext();) {
            cls = it.next();
            if (addClasses) {
                if ((modifier < 0 || cls.getModifiers() == modifier)
                    && (annotation == null || cls.getAnnotation(annotation) != null)) {
                    match = StringUtil.fuzzyMatch(cls.getName(), filter);
                    if (match > 0)
                        result.put(match, (T) cls);
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

    private Map<Double, Method> fuzzyFindMethods(Class cls,
            String filter,
            int modifier,
            Class<? extends Annotation> annotation) {
        HashMap<Double, Method> map = new HashMap<Double, Method>();
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

    private Map<Double, Field> fuzzyFindFields(Class cls,
            String filter,
            int modifier,
            Class<? extends Annotation> annotation) {
        HashMap<Double, Field> map = new HashMap<Double, Field>();
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
