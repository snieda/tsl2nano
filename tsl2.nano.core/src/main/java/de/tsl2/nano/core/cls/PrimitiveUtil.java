/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Apr 17, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core.cls;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.core.util.Util;

/**
 * handler for primitives and their immutable wrappers. all wrappers have a static 'TYPE' providing the primitive class
 * - and MIN_VAUE and MAX_VALUE.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PrimitiveUtil {
    private static final Log LOG = LogFactory.getLog(PrimitiveUtil.class);

    static final Class[] primitives;
    static final Class[] wrappers;
    static final SimpleClassComparator comparator;
    static {
        comparator = new SimpleClassComparator();
        /*
         * we use two arrays to search in both direction (not possible on a map).
         * sorting both will have the right order - the names have the same natural order.
         */
        primitives = new Class[] { short.class,
            int.class,
            long.class,
            float.class,
            double.class,
            char.class,
            byte.class,
            boolean.class,
            void.class };
        //to use Arrays.binarySearch()
        Arrays.sort(primitives, comparator);

        wrappers = new Class[] { Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            Character.class,
            Byte.class,
            Boolean.class,
            Void.class };
        //to use Arrays.binarySearch()
        Arrays.sort(wrappers, comparator);
    }

    public static <T> T create(Class<T> type, String value) {
        try {
            return (T) FormatUtil.getDefaultFormat(type, true).parseObject(value);
        } catch (ParseException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * isPrimitiveOrWrapper
     * 
     * @param type
     * @return true, if given type is primitive or wrapper.
     */
    public static boolean isPrimitiveOrWrapper(Class<?> type) {
        //binarySearch removed in cause of having Classes that are not Comparable!
        //CollectionUtil not usable - not in core package
        return type.isPrimitive() || (Comparable.class.isAssignableFrom(type) && Arrays.asList(wrappers).indexOf(type) != -1);
    }

    /**
     * getPrimitive
     * 
     * @param immutableWrapper
     * @return the primitive class for the given wrapper type or the given type itself, if not found as primitive.
     */
    public static <T> Class<T> getPrimitive(Class<T> immutableWrapper) {
        int i = Arrays.binarySearch(wrappers, immutableWrapper, comparator);
        return i >= 0 ? primitives[i] : immutableWrapper;
    }
    
    public static boolean isPrimitiveArray(Object obj) {
    	return obj != null && isPrimitiveArray(obj.getClass());
    }
    public static boolean isPrimitiveArray(Class primitiveArrayType) {
        return primitiveArrayType.isArray() && primitiveArrayType.getComponentType().isPrimitive();
    }
    
    public static Class getPrimitiveArrayComponentType(Class primitiveArrayType) {
    	assert isPrimitiveArray(primitiveArrayType);
    	return (Class) BeanClass.getStatic(getWrapper(primitiveArrayType.getComponentType()), "TYPE");
    }
    
	public static void fillArray(Object arrayInstance, Object value) {
		assert isPrimitiveArray(arrayInstance);
    	int length = Array.getLength(arrayInstance);
    	for (int i=0; i<length; i++) {
    		Array.set(arrayInstance, i, value);
    	}
	}
	
	public static Class getPrimitiveArrayClass(String str) {
		return BeanClass.load("[" + str.substring(0, 1).toUpperCase());
	}
	
    public static Object toArray(Stream stream, Class componentType, int length) {
        Object arr = Array.newInstance(componentType, length);
        AtomicInteger i = new AtomicInteger();
        stream.forEach(item -> Array.set(arr, i.getAndIncrement(), convert(item, componentType)));
        return arr;
    }

	public static String toArrayString(Object primitiveArray) {
		assert isPrimitiveArray(primitiveArray);
		if (primitiveArray instanceof boolean[])
			return Arrays.toString((boolean[])primitiveArray);
		else if (primitiveArray instanceof byte[])
			return Arrays.toString((byte[])primitiveArray);
		else if (primitiveArray instanceof char[])
			return Arrays.toString((char[])primitiveArray);
		else if (primitiveArray instanceof double[])
			return Arrays.toString((double[])primitiveArray);
		else if (primitiveArray instanceof float[])
			return Arrays.toString((float[])primitiveArray);
		else if (primitiveArray instanceof int[])
			return Arrays.toString((int[])primitiveArray);
		else if (primitiveArray instanceof long[])
			return Arrays.toString((long[])primitiveArray);
		else if (primitiveArray instanceof short[])
			return Arrays.toString((short[])primitiveArray);
		else
			throw new IllegalArgumentException();
	}

	/**
     * getWrapper
     * 
     * @param primitive primitive type
     * @return the wrapper class for the given primitive type or the given type itself, if not found as wrapper.
     */
    public static <T> Class<T> getWrapper(Class<T> primitive) {
//        assert primitive.isPrimitive() : "The given class " + primitive + " must be a primitive!";
        int i = Arrays.binarySearch(primitives, primitive, comparator);
        return i >= 0 ? wrappers[i] : primitive;
    }

    /**
     * creates a default instance of the given primitive
     * 
     * @param <T> primitive type
     * @param standardType primitive type
     * @return instanceof primitives immutable
     */
    public static <T> T getDefaultValue(Class<T> standardType) {
        
        if (standardType.equals(boolean.class) || standardType.equals(Boolean.class)) {
            return (T) Boolean.FALSE;
        } else if (standardType.equals(int.class) || standardType.equals(Integer.class)) {
            return (T) new Integer(0);
        } else if (standardType.equals(char.class) || standardType.equals(Character.class)) {
            return (T) new Character((char) 0);
        } else if (standardType.equals(short.class) || standardType.equals(Short.class)) {
            return (T) new Short((short) 0);
        } else if (standardType.equals(long.class) || standardType.equals(Long.class)) {
            return (T) new Long(0l);
        } else if (standardType.equals(float.class) || standardType.equals(Float.class)) {
            return (T) new Float(0f);
        } else if (standardType.equals(double.class) || standardType.equals(Double.class)) {
            return (T) new Double(0d);
        } else if (standardType.equals(byte.class) || standardType.equals(Byte.class)) {
            return (T) new Byte((byte) 0);
        } else if (standardType.equals(void.class) || standardType.equals(Void.class)) {
            return null;
        } else {
            throw ManagedException.implementationError("only primitives and their immutables are allowed!", standardType);
        }
    }

    /** delegates to {@link #isDefaultValue(Class, Object)} */
    public static <T> boolean isDefaultValue(T value) {
    	return value == null || !isPrimitiveOrWrapper(value.getClass())? false : isDefaultValue((Class<T>)value.getClass() , value);
    }
    
    /**
     * on primitives we have the problem, that they always have a value. sometimes we need the workaround to check, if
     * it is a 'real' value.
     * 
     * @param primitiveType
     * @param value to be checked, if it is the primitives default value (e.g. on int = 0)
     * @return true if given value is the primitives default value
     */
    public static <T> boolean isDefaultValue(Class<T> primitiveType, T value) {
        return value.equals(getDefaultValue(primitiveType));
    }

    /**
     * returns the minimum value of the given primitive
     * 
     * @param <T> primitive type
     * @param standardType primitive type
     * @return minimum value for given type
     */
    public static <T> T getMinimumValue(Class<T> standardType) {
        assert standardType.isPrimitive() : "standardType must be a primitive, but is:" + standardType;
        Class<T> t = getWrapper(standardType);
        return (T) (Boolean.class.isAssignableFrom(t) ? Boolean.FALSE : BeanClass.getBeanClass(t).createInstance(
            BeanClass.getStatic(t, "MIN_VALUE")));
    }

    /**
     * returns the minimum value of the given primitive
     * 
     * @param <T> primitive type
     * @param standardType primitive type
     * @return minimum value for given type
     */
    public static <T> T getMaximumValue(Class<T> standardType) {
        assert isPrimitiveOrWrapper(standardType) : "standardType must be a primitive, but is:" + standardType;
        Class<T> t = BigDecimal.class.isAssignableFrom(standardType) || BigInteger.class.isAssignableFrom(standardType) 
            ? (Class<T>) Long.class 
            : getWrapper(standardType);
        return (T) (Boolean.class.isAssignableFrom(t) ? Boolean.TRUE : BeanClass.getBeanClass(t).createInstance(
            BeanClass.getStatic(t, "MAX_VALUE")));
    }

    /**
     * extends the {@link Class#isAssignableFrom(Class)} to check for primitives and their immutables
     * 
     * @param cls1 to be assignable from cls2
     * @param cls2 cls2
     * @return true, if cls.{@link Class#isAssignableFrom(Class)} returns true or the cls1 is primitive of cls2 or vice
     *         versa.
     */
    public static boolean isAssignableFrom(Class<?> cls1, Class<?> cls2) {
        boolean isassignable = cls1.isAssignableFrom(cls2);
        if (!isassignable) {
            try {
                if (cls1.isPrimitive() && !cls2.isPrimitive()) {
                    isassignable = cls1.isAssignableFrom(getPrimitive(cls2));
                } else if (cls2.isPrimitive() && !cls1.isPrimitive()) {
                    isassignable = cls2.isAssignableFrom(getPrimitive(cls1));
                }
            } catch (Exception ex) {
                //do nothing - it was only a try...
                LOG.debug("class " + cls1 + " is not assignable from " + cls2);
            }
        }
        return isassignable;
    }

    /**
     * WARNING: JAVAs autoboxing converts the primitive into a wrapping instance again, if you assign the result to an
     * object!
     * <p/>
     * normally, java uses its auto-boxing to convert internally. but calling methods through reflection need exact
     * argument types!
     * 
     * @param o wrapped instance to be unwrapped to a primitive value
     * @return primitive value
     */
    public static short asPrimitive(Short o) {
        return o.shortValue();
    }

    public static int asPrimitive(Integer o) {
        return o.intValue();
    }

    public static long asPrimitive(Long o) {
        return o.longValue();
    }

    public static float asPrimitive(Float o) {
        return o.floatValue();
    }

    public static double asPrimitive(Double o) {
        return o.doubleValue();
    }

    public static double asPrimitive(BigDecimal o) {
        return o.doubleValue();
    }

    public static boolean asPrimitive(Boolean o) {
        return o.booleanValue();
    }

    public static char asPrimitive(Character o) {
        return o.charValue();
    }

    public static byte asPrimitive(Byte o) {
        return o.byteValue();
    }

    /**
     * conversion of primitives or wrappers. conversions/casts of primitives in typed/compiled code is no problem. on
     * dynamic conversions/casts, knowing the converting types on runtime there is a problem, because {@link Class#cast}
     * restricts casts like Integer-->Long - while its no problem on static code.
     * <p/>
     * this method does not use reflection to do the conversion.
     * 
     * @param value primitive/wrapper value to convert to another primitive/wrapper defined by conversionType
     * @param conversionType result type
     * @return converted value
     */
    public static <T> T convert(Object value, Class<T> conversionType) {
        if (value == null)
            return null;
        if (conversionType.isAssignableFrom(value.getClass()))
        	return (T) value;
        
        //first: convert the non-number values to numbers
        if (!(value instanceof Number)) {
	        if (isAssignableFrom(Boolean.class, value.getClass())
	        		|| "false".equals(value) || "true".equals(value))
	            value = Boolean.valueOf(value.toString()) ? 1 : 0;
	        else if (isAssignableFrom(Character.class, value.getClass()))
	            value = value.hashCode();
	        else if (isAssignableFrom(String.class, value.getClass()))
	        	if (isAssignableFrom(Character.class, conversionType))
	        		value = value.hashCode();
                else if (!Util.isEmpty(value, true)) {
                    Object p = FormatUtil.parse(Double.class, (String) value);//Double.valueOf((String) value);
                    if (p == null)
                        throw new IllegalArgumentException(
                                "value \"" + value + "\" cannot be converted to " + conversionType);
                    else
                        value = p;
                } else
	        		throw new IllegalArgumentException(value + " can't be converted to " + conversionType);
	        else
	        	throw new IllegalArgumentException(value + " can't be converted to " + conversionType);
        }
                    
        //now we have a number
        double d = ((Number)value).doubleValue();
        
        if (isAssignableFrom(Boolean.class, conversionType)) {
            return (T) (d != 0 ? Boolean.TRUE : Boolean.FALSE);
        } else if (isAssignableFrom(Integer.class, conversionType)) {
            Integer c = (int)d;
            return (T) c;
        } else if (isAssignableFrom(Long.class, conversionType)) {
            Long c = (long)d;
            return (T) c;
        } else if (isAssignableFrom(Float.class, conversionType)) {
            Float c = (float)d;
            return (T) c;
        } else if (isAssignableFrom(Double.class, conversionType)) {
            Double c = d;
            return (T) c;
        } else if (isAssignableFrom(Short.class, conversionType)) {
            Short c = (short)d;
            return (T) c;
        } else if (isAssignableFrom(Byte.class, conversionType)) {
            Byte c = (byte)d;
            return (T) c;
        } else if (isAssignableFrom(Character.class, conversionType)) {
            Character c = (char)d;
            return (T) c;
        }
        throw new IllegalArgumentException("conversionType is not primitive or wrapper");
    }

    /**
     * usable on loading classes where a classloader isn't able to do it.
     * 
     * @param name full class name. for primitives its only the name of the primitive e.g.: int.
     * @return primitive class or null
     */
    public static Class<?> getPrimitiveClass(String name) {
        for (int i = 0; i < primitives.length; i++) {
            if (primitives[i].getName().equals(name)) {
                return primitives[i];
            }
        }
        return null;
    }

	public static Object[] string2Wrapper(String[] args) {
		return Arrays.stream(args).map(a -> string2Wrapper(a)).toArray();
	}

	/**
	 * @param a
	 * @return converts a string to a boolean, double or integer - or returns the string itself
	 */
	public static Object string2Wrapper(String a) {
		a = a.trim();
		if (a.matches("true|false"))
			return Boolean.valueOf(a);
		else if (a.matches("[+-]?\\d+[.,]{1}\\d+"))
			return Double.valueOf(a);
		else if (a.matches("[+-]?\\d+"))
			return Integer.valueOf(a);
		return a;
	}

	public static int getDecMaxLen(Class<?> type) {
		Number max = (Number) getMaximumValue(type);
		return (int) Math.log10(max.doubleValue()) + 1;
	}

    public static boolean isBoolean(Object obj) {
        return obj instanceof Boolean || boolean.class.isAssignableFrom(obj.getClass());
    }
}

class SimpleClassComparator implements Comparator<Class> {
    @SuppressWarnings("rawtypes")
    @Override
    public int compare(Class o1, Class o2) {
        return o1.getSimpleName().compareTo(o2.getSimpleName());
    }

}