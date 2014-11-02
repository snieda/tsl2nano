/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 05.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.format.FormatUtil;

/**
 * utils for general purpose on simple objects.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Util {

    protected Util() {
    }

    /**
     * isAllNull
     * 
     * @param objects objects to check
     * @return true, if all objects are null
     */
    public static boolean isAllNull(Object... objects) {
        checkParameterCount(objects, 1);
        for (Object o : objects) {
            if (o != null)
                return false;
        }
        return true;
    }

    /**
     * hasNull
     * 
     * @param objects objects to check
     * @return true, if at least one object is null
     */
    public static boolean hasNull(Object... objects) {
        checkParameterCount(objects, 1);
        for (Object o : objects) {
            if (o != null)
                return false;
        }
        return true;
    }

    protected static void checkParameterCount(Object[] objects, int i) {
        if (objects == null || objects.length < i)
            throw new IllegalArgumentException("at least " + i + " parameter must be given!");
    }

    /**
     * isEmpty
     * 
     * @param obj object to analyze
     * @return true, if object is null or empty
     */
    @SuppressWarnings("rawtypes")
    public static final boolean isEmpty(Object obj) {
        return isEmpty(obj, false) || (obj.getClass().isArray() && Array.getLength(obj) == 0)
            || ((obj instanceof Collection) && ((Collection) obj).isEmpty())
            || ((obj instanceof Map) && ((Map) obj).isEmpty());
    }

    /**
     * isEmpty
     * 
     * @param object object to analyze
     * @return true, if object is null or empty
     */
    public static final boolean isEmpty(Object object, boolean trim) {
        return object == null || object.toString() == null
            || (trim ? object.toString().trim().isEmpty() : object.toString().isEmpty());
    }

    /**
     * delegates to {@link #isContainer(Class)}
     */
    public static final boolean isContainer(Object obj) {
        return obj != null ? isContainer(obj.getClass()) : false;
    }

    /**
     * isContainer
     * 
     * @param cls
     * @return true, if cls is a collection or map
     */
    public static final boolean isContainer(Class<?> cls) {
        return /*cls.isArray() || */Collection.class.isAssignableFrom(cls) || Map.class.isAssignableFrom(cls);
    }

    /**
     * if {@link #isContainer(Object)} returns true, obj is an array, collection or map. this method returns a
     * collection where obj is wrapped into.
     * 
     * @param obj
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Collection<?> getContainer(Object obj) {
        if (obj == null)
            return null;
        Class<?> cls = obj.getClass();
        if (cls.isArray())
            return asList(obj);
        else if (Collection.class.isAssignableFrom(cls))
            return (Collection<?>) obj;
        else if (Map.class.isAssignableFrom(cls))
            return MapUtil.asEntrySetExtender((Map) obj);
        else
            throw new ManagedException(obj + " is not a container!");
    }

    /**
     * checks, whether entry is one of elements
     * 
     * @param entry to check
     * @param elements collection of available entries
     * @return true, if data is equal to one of c.
     */
    public static final <T> boolean in(T entry, T... elements) {
        checkParameterCount(elements, 1);
        return Arrays.asList(elements).contains(entry);
    }

    /**
     * checks whether data contains one of items
     * 
     * @param data data to check
     * @param items available items
     * @return true, data contains one of items
     */
    public static final <T> boolean contains(T[] data, T... items) {
        for (int i = 0; i < data.length; i++) {
            if (in(data[i], items))
                return true;
        }
        return false;
    }

    /**
     * checks whether data contains one of items
     * 
     * @param data data to check
     * @param items available items
     * @return true, data contains one of items
     */
    public static final <T> boolean containsAll(T[] data, T... items) {
        return Arrays.asList(data).containsAll(Arrays.asList(items));
    }

    /**
     * asString
     * 
     * @param obj obj to call toString() if not null
     * @return obj.toString() or null
     */
    public static final String asString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    /**
     * getCryptoHash
     * 
     * @param data
     * @return
     */
    public static final byte[] cryptoHash(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA").digest(data);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * evaluates hashcode of all objects
     * 
     * @param objects objects to check
     * @return combined hashcode
     */
    public static int hashCode(Object... objects) {
        checkParameterCount(objects, 1);
        return Arrays.hashCode(objects);
    }

    /**
     * checks, if all objects are equal
     * 
     * @param objects objects to check
     * @return true, if all objects are equal
     */
    public static boolean equals(Object... objects) {
        checkParameterCount(objects, 2);
        Object last = objects[0];
        for (Object o : objects) {
            if (o != null && !o.equals(last))
                return false;
            last = o;
        }
        return true;
    }

    /**
     * value
     * 
     * @param objects object to check
     * @param defaultValue
     * @return object, or if null the defaultValue
     */
    @SuppressWarnings("unchecked")
    public static <T> T value(Object object, T defaultValue) {
        return (T) (object != null ? object : defaultValue);
    }

    /**
     * wrap an array into a collection. works on Object[] using Arrays.asList() and on primitive arrays with a simple
     * loop.
     * <p/>
     * Internal Information: As this method should be content of CollectionUtil, it is implemented here to be inside a
     * core class.
     * 
     * @param array object that is an array
     * @return filled collection
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Collection asList(Object array) {
        assert array.getClass().isArray() : "array parameter must be an array!";
        if (array instanceof Object[]) {
            //the Arrays.asList() returns a fixed size list!
            return Arrays.asList((Object[]) array);
        }

        /*
         * on primitives, do it yourself
         * Arrays.asList() needs a special array cast
         */
        final int length = Array.getLength(array);
        final Collection c = new ArrayList(length);
        for (int i = 0; i < length; i++) {
            c.add(Array.get(array, i));
        }
        return c;
    }

    /**
     * converts the given value to the given type. this is done by parsing the toString() representation --> slow
     * performance!
     * 
     * @param type type of new value
     * @param value value to convert
     * @return to type converted value
     */
    public static Object convert(Class type, Object value) {
        if (value == null)
            return type.isPrimitive() ? PrimitiveUtil.getDefaultValue(type) : null;
        try {
            //TODO: howto bypass javas autoboxing on primitives?
//            if (type.isPrimitive())
//                return PrimitiveUtil.asPrimitive((Double)value);
//            else
                return FormatUtil.getDefaultFormat(type, true).parseObject(value.toString());
        } catch (ParseException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * converts an array of values to the given type. this is done by parsing the toString() representation of the items
     * --> slow performance.
     * 
     * @param type
     * @param items
     * @return
     */
    public static Object[] convertAll(Class type, Object... items) {
        for (int i = 0; i < items.length; i++) {
            items[i] = convert(type, items[i]);
        }
        return items;
    }

    /**
     * standard toString implementation
     * 
     * @param cls root class
     * @param members class members
     * @return tostring representation
     */
    public static String toString(Class<?> cls, Object... members) {
        StringBuilder buf = new StringBuilder(cls.getSimpleName() + "(" + members[0]);
        for (int i = 1; i < members.length; i++) {
            buf.append(", " + String.valueOf(members[i]));
        }
        return buf.append(")").toString();
    }
}
