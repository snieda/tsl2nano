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

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.format.FormatUtil;

/**
 * handler for primitives and their immutable wrappers
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
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
        return type.isPrimitive() || Arrays.binarySearch(wrappers, type) >= 0;
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
        assert standardType.isPrimitive() : "standardType must be a primitive, but is:" + standardType;

        if (standardType.equals(boolean.class)) {
            return (T) Boolean.FALSE;
        } else if (standardType.equals(int.class)) {
            return (T) new Integer(0);
        } else if (standardType.equals(char.class)) {
            return (T) new Character((char) 0);
        } else if (standardType.equals(short.class)) {
            return (T) new Short((short) 0);
        } else if (standardType.equals(long.class)) {
            return (T) new Long(0);
        } else if (standardType.equals(float.class)) {
            return (T) new Float(0);
        } else if (standardType.equals(double.class)) {
            return (T) new Double(0);
        } else if (standardType.equals(byte.class)) {
            return (T) new Byte((byte) 0);
        } else if (standardType.equals(void.class)) {
            return null;
        } else {
            throw ManagedException.implementationError("only primitives are allowed!", standardType);
        }
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
        assert standardType.isPrimitive() : "standardType must be a primitive, but is:" + standardType;
        Class<T> t = getWrapper(standardType);
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
                if (cls1.isPrimitive() && !cls2.isPrimitive())
                    isassignable = cls1.isAssignableFrom(getPrimitive(cls2));
                else if (cls2.isPrimitive() && !cls1.isPrimitive())
                    isassignable = cls2.isAssignableFrom(getPrimitive(cls1));
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
        return ((Character) o).charValue();
    }

    public static byte asPrimitive(Byte o) {
        return o.byteValue();
    }
}

class SimpleClassComparator implements Comparator<Class> {
    @Override
    public int compare(Class o1, Class o2) {
        return o1.getSimpleName().compareTo(o2.getSimpleName());
    }

}