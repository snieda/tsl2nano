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
import java.text.Format;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.execution.IRunnable;

/**
 * utils for general purpose on simple objects.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Util {
    public static final String FRAMEWORK_PACKAGE;
    static {
        String pck = Util.class.getPackage().getName();
        String[] p = pck.split("\\.");
        FRAMEWORK_PACKAGE = StringUtil.concat(new char[] { '.' }, p[0], p[1], p[2]);
    }

    protected Util() {
    }

    /**
     * isFrameworkClass
     * 
     * @param cls
     * @return true, if given class is part of this framework
     */
    public static boolean isFrameworkClass(Class<?> cls) {
        //compare the first three package names
        //some classes like Object[].class have no package path!
        return cls.getPackage() != null && cls.getPackage().getName().startsWith(FRAMEWORK_PACKAGE);
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
            if (o != null) {
                return false;
            }
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
            if (o != null) {
                return false;
            }
        }
        return true;
    }

    protected static void checkParameterCount(Object[] objects, int i) {
        if (objects == null || objects.length < i) {
            throw new IllegalArgumentException("at least " + i + " parameter must be given!");
        }
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
        if (obj == null) {
            return null;
        }
        Class<?> cls = obj.getClass();
        if (cls.isArray()) {
            return asList(obj);
        } else if (Collection.class.isAssignableFrom(cls)) {
            return (Collection<?>) obj;
        } else if (Map.class.isAssignableFrom(cls)) {
            return CollectionUtil.asEntrySetExtender((Map) obj);
        } else {
            throw new ManagedException(obj + " is not a container!");
        }
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
            if (in(data[i], items)) {
                return true;
            }
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
     * delegates to {@link #cryptoHash(byte[], String)} using SHA and length of 32.
     */
    public static final byte[] cryptoHash(byte[] data) {
        return cryptoHash(data, "SHA", 32);
    }

    /**
     * creates a hash for the given data. use {@link StringUtil#toHexString(byte[])} to convert the result to a more
     * readable string.
     * 
     * @param data data to hash
     * @param algorithm one of MD2, MD5, SHA, SHA-1, SHA-256, SHA-384, SHA-512
     * @return hashed data encoded with UTF-8
     */
    public static final byte[] cryptoHash(byte[] data, String algorithm, int length) {
        try {
            return MessageDigest.getInstance(algorithm).digest(data);
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
            if (o != null && !o.equals(last)) {
                return false;
            }
            last = o;
        }
        return true;
    }

    /**
     * value of array at given
     * 
     * @param arr array
     * @param index array index for value
     * @param defaultValue default value, if array does not contain a value at the given index.
     * @return value of array at given index, or if null or not existing, the defaultValue
     */
    public static <T> T value(T[] arr, int index, T defaultValue) {
        return arr.length > index && arr[index] != null ? arr[index] : defaultValue;
    }

    /**
     * if given value is not null, it will be used/returned. if it is null, the defaultValue will be used/returned
     * 
     * @param objects object to check
     * @param defaultValue
     * @return object, or if null the defaultValue
     */
    public static <T> T value(T object, T defaultValue) {
        return object != null ? object : defaultValue;
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
        if (value == null) {
            return type.isPrimitive() ? PrimitiveUtil.getDefaultValue(type) : null;
        }
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
     * does a conditional for-loop for you. loops over the given iterable sources, executes the given action if the
     * source elements string representation matches the given regex expression.
     * <p/>
     * NOTE: it's possible to use recursion, if your iterable items contain iterables itself.
     * 
     * @param items items to be looped over
     * @param regEx regular expression to be checked against all iterable items.
     * @param action action to be done on a matching source item.
     * @return all results of executed actions that matched the regular expression.
     */
    public static <S, T> Iterable<T> forEach(Iterable<S> items, final String regEx, final IRunnable<T, S> action) {
        Collection<T> result = new ArrayList<T>(); //no size through iterable available!
        return forEach(items, regEx, result, action);
    }

    private static <S, T> Iterable<T> forEach(Iterable<S> items,
            final String regEx,
            Collection<T> result,
            final IRunnable<T, S> action) {
        for (S i : items) {
            //recursive
            if (i instanceof Iterable) {
                forEach((Iterable<S>) i, regEx, result, action);
            }
            //do the job
            if (String.valueOf(i).matches(regEx)) {
                result.add(action.run(i));
            }
        }
        return result;
    }

    /**
     * convenience to get a system property. if defined, it will be converted to the type of defaultValue. if not
     * defined, the default value will be returned.
     * 
     * @param name system property name
     * @param defaultValue system property default
     * @return value or default value
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String name, T defaultValue) {
        Object result = System.getProperties().get(name);
        if (result != null) {
            if (defaultValue != null && !defaultValue.getClass().isAssignableFrom(result.getClass())) {
                Format df = FormatUtil.getDefaultFormat(defaultValue, true);
                try {
                    return (T) df.parseObject((String) result);
                } catch (ParseException e) {
                    ManagedException.forward(e);
                }
            } else {
                return (T) result;
            }
        }
        return defaultValue;
    }

    /**
     * workaround for java compiler (mostly not for the jit-compiler) to uncheck generic types to be castable to another
     * generic. It does the same like casting to Object.
     * <p/>
     * Example: Set<MyClass> set = (Set<MyClass) (Object) new HashSet<MyExtendedClass>();
     * 
     * @param checkedGenericObject
     * @param uncheckedCast the cast to be done on the given object
     * @return un-typed object
     */
    public static final Object untyped(Object checkedGenericObject/*, Class<T> uncheckedCast*/) {
        return checkedGenericObject;
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
