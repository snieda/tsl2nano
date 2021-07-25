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

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.MessageDigest;
import java.text.Format;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.cls.UnboundAccessor;
import de.tsl2.nano.core.execution.IRunnable;
import de.tsl2.nano.core.log.LogFactory;

/**
 * utils for general purpose on simple objects.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Util {
    private static final Log LOG = LogFactory.getLog(Util.class);
    public static final String FRAMEWORK_PACKAGE = getBasePackage(Util.class);

    public static String getBasePackage(Class<?> cls) {
		String pck = cls.getPackage().getName();
        String[] p = pck.split("\\.");
        return StringUtil.concat(new char[] { '.' }, p[0], p[1], p[2]);
	}

    private static final byte[] salt16 = {
            (byte) 0x71, (byte) 0x37, (byte) 0x30, (byte) 0x23,
            (byte) 0x45, (byte) 0x52, (byte) 0x01, (byte) 0x15,
            (byte) 0x63, (byte) 0x82, (byte) 0x27, (byte) 0x72,
            (byte) 0x81, (byte) 0xd7, (byte) 0xde, (byte) 0x89
        };

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
     * isJavaType
     * @param cls class to evaluate
     * @return true, if class package name starts with 'java'
     */
    public static boolean isJavaType(Class<?> cls) {
        return cls.getPackage() != null && cls.getPackage().getName().startsWith("java");
    }

    public static boolean isSimpleType(Class<?> cls) {
    	return PrimitiveUtil.isPrimitiveOrWrapper(cls) || NumberUtil.isNumber(cls) || Date.class.isAssignableFrom(cls) || String.class.isAssignableFrom(cls);
    }

    public static boolean isDataType(Class<?> cls) {
    	return Serializable.class.isAssignableFrom(cls) && Cloneable.class.isAssignableFrom(cls) && Comparable.class.isAssignableFrom(cls);
    }
    
    /**
     * 
     * @param cls class to evaluate
     * @return true if this class can be constructed (it is public and not abstract)
     */
    public static boolean isInstanceable(Class<?> cls) {
        return !cls.isAnnotation() && !cls.isLocalClass() && !cls.isAnonymousClass() /*&& !cls.isArray()*/ && !cls.isInterface() && !isAbstract(cls) && Modifier.isPublic(cls.getModifiers());
    }

	public static boolean isAbstract(Class<?> cls) {
		return Modifier.isAbstract(cls.getModifiers()) && !cls.isArray();
	}
	/**
	 * @param cls any class to get the single base type for
	 * @return on arrays the array component type, on byte streams byte.class, on wrappers the primitive
	 */
	public static Class<?> getSingleBaseType(Class<?> cls) {
		if (PrimitiveUtil.isPrimitiveArray(cls))
			return PrimitiveUtil.getPrimitiveArrayComponentType(cls);
		else if (cls.isArray())
			return cls.getComponentType();
		else if (ByteUtil.isByteStream(cls))
			return byte.class;
		else 
			return PrimitiveUtil.getPrimitive(cls);
	}
	
    /**
     * isAllNull
     * 
     * @param objects objects to check
     * @return true, if all objects are null
     */
    public static boolean isAllNull(Object... objects) {
        checkMinParameterCount(objects, 1);
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
        checkMinParameterCount(objects, 1);
        for (Object o : objects) {
            if (o != null) {
                return false;
            }
        }
        return true;
    }

    protected static void checkMinParameterCount(Object[] objects, int i) {
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
        return isEmpty(obj, false) || obj == UnboundAccessor.NULL || (obj.getClass().isArray() && Array.getLength(obj) == 0)
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
     * if given value {@link #isEmpty(Object)}, null will be returned, the value itself otherwise
     * @param value to be checked
     * @return null or value
     */
    public static final <T> T nonEmpty(T value) {
        return isEmpty(value) ? null : value;
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
     * checks, whether entry is one of elements
     * 
     * @param entry to check
     * @param elements collection of available entries
     * @return true, if data is equal to one of c.
     */
    public static final <T> boolean in(T entry, T... elements) {
        checkMinParameterCount(elements, 1);
        return Arrays.asList(elements).contains(entry);
    }

    public static final <T> boolean in_(T entry, T... elements) {
        Arrays.sort(elements);
        return Arrays.binarySearch(elements, entry) > -1;
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
        return cryptoHash(data, "SHA-512");
    }

    /**
     * creates a hash for the given data. use {@link StringUtil#toHexString(byte[])} to convert the result to a more
     * readable string.
     * 
     * @param data data to hash
     * @param algorithm one of MD2, MD5, SHA, SHA-1, SHA-256, SHA-384, SHA-512
     * @return hashed data encoded with UTF-8
     */
    public static final byte[] cryptoHash(byte[] data, String algorithm) {
        try {
        	MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(salt16);
            return md.digest(data);
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
        checkMinParameterCount(objects, 1);
        return Arrays.hashCode(objects);
    }

    /**
     * checks, if all objects are equal
     * Usage in own equals() implementation:<code>
		if (!(o instanceof MyClass))
			return false;
		MyClass m = (MyClass) o;
		return Util.equals(this.member1, m.member1) && Util.equals(member2, m.member2) ...;
     * </code>
     * @param objects objects to check
     * @return true, if all objects are equal
     */
    public static boolean equals(Object... objects) {
        checkMinParameterCount(objects, 2);
        Object last = objects[0];
        for (Object o : objects) {
            if ((o != null && !o.equals(last)) || (last != null && ! last.equals(o))) {
                return false;
            }
            last = o;
        }
        return true;
    }

    /**
     * value of array at given index
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
    public static <T> T get(String name, T defaultValue) {
        return get(System.getProperties(), name, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Properties p, String name, T defaultValue) {
        Object result = p.get(name);
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

    /** does a fast toString() like the Object.toString() does. Usefull on logging. */
    public static final String toObjString(Object instance) {
    	return instance != null ? instance.getClass().getSimpleName() + "@" + instance.hashCode() : "null";
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

    public static String toString(String seprator, Object... values) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (isEmpty(values[i]))
                continue;
            buf.append((buf.length()>0 ? seprator : "") + values[i]);
        }
        return buf.toString();
    }
    
    public static String toJson(Object obj) {
    	return JSon.toJSon(obj);
    }

    public static ClassLoader getContextClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = Util.class.getClassLoader();
            if (cl == null)
                cl = ClassLoader.getSystemClassLoader();
            LOG.warn("context classloader of current thread " + Thread.currentThread() + " is null! using classloader " + cl);
        }
        return cl;
	}

    /**let the trY to the standard exception handling  */
    public static <T> T trY(SupplierEx<T> callback) {
        return trY(callback, true);
    }
    public static <T> T trY(SupplierEx<T> callback, boolean escalate) {
        return ManagedException.trY(callback, escalate);
    }
    public static <T> T trY(SupplierExVoid<T> callback) {
    	return trY(callback, true);
    }
    /**let the trY to the standard exception handling  */
    public static <T> T trY(SupplierExVoid<T> callback, boolean escalate) {
        return ManagedException.trY(callback, escalate);
    }
	public static final void assert_(boolean assertion, String message, Object... args) {
		if (!assertion && !Boolean.getBoolean("tsl2.nano.disable.assertion"))
			throw new IllegalArgumentException(String.format(message, args));
	}
	public static <T> Stream<T> stream(Collection<T> c, boolean parallel) {
		return parallel ? c.parallelStream() : c.stream();
	}
}
