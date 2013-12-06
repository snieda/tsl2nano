/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Apr 13, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;

import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.log.LogFactory;

/**
 * Some utils for comparables
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class CUtil extends Util {

    protected static final Log LOG = LogFactory.getLog(NumberUtil.class);

    /**
     * evaluates the minimum of all values
     * 
     * @param values to compare
     * @return the minimum
     */
    public static final <T extends Object & Comparable<? super T>> T min(T... values) {
        return Collections.min(Arrays.asList(values));
    }

    /**
     * evaluates the minimum of all values
     * 
     * @param values to compare
     * @return the minimum
     */
    public static final <T extends Object & Comparable<? super T>> T max(T... values) {
        return Collections.max(Arrays.asList(values));
    }

    /**
     * evaluates the min and max and compares them with {@link Comparable#compareTo(Object)}. Be careful: The standard
     * implementations of compareTo() will return only 1, 0 or -1 - not the delta value!
     * 
     * @param <T> comparable type
     * @param values comparables
     * @return the result of max.compareTo(min)
     */
    public static final <T extends Object & Comparable<? super T>> int getDeltaCompare(T... values) {
        List<T> v = Arrays.asList(values);
        return Collections.max(v).compareTo(Collections.min(v));
    }

    public static final <T extends Number & Comparable<? super T>> float getDelta(T... values) {
        List<T> v = Arrays.asList(values);
        return Collections.max(v).floatValue() - Collections.min(v).floatValue();
    }

    /**
     * intersects
     * 
     * @param <T> type of comparable values
     * @param from1 first start
     * @param to1 first end
     * @param from2 second start
     * @param to2 second end
     * @return true, if first and second span intersect
     */
    public static final <T extends Object & Comparable<? super T>> boolean intersects(T from1,
            T to1,
            T from2,
            T to2) {
                return from1.compareTo(to2) <= 0 && from2.compareTo(to1) <= 0;
            }

    /**
     * includes
     * 
     * @param <T> type of comparable values
     * @param from1 first start
     * @param to1 first end
     * @param from2 second start
     * @param to2 second end
     * @return true, if first span includes second span
     */
    public static final <T extends Object & Comparable<? super T>> boolean includes(T from1,
            T to1,
            T from2,
            T to2) {
                return from1.compareTo(from2) <= 0 && to1.compareTo(to2) >= 0;
            }

    /**
     * isLower
     * 
     * @param op1 operand one
     * @param op2 operand two
     * @return true, if op1 < op2
     */
    public static final <T extends Comparable<T>> boolean isLower(T op1, T op2) {
        return op1.compareTo(op2) < 0;
    }

    /**
     * isGreater
     * 
     * @param op1 operand one
     * @param op2 operand two
     * @return true, if op1 > op2
     */
    public static final <T extends Comparable<T>> boolean isGreater(T op1, T op2) {
        return op1.compareTo(op2) > 0;
    }

    /**
     * min
     * 
     * @param op1 operand one
     * @param op2 operand two
     * @return op1, if op1 < op2
     */
    public static final <T extends Comparable<T>> T min(T op1, T op2) {
        return isLower(op1, op2) ? (T) op1 : op2;
    }

    /**
     * max
     * 
     * @param op1 operand one
     * @param op2 operand two
     * @return op1, if op1 > op2
     */
    public static final <T extends Comparable<T>> T max(T op1, T op2) {
        return isGreater(op1, op2) ? (T) op1 : op2;
    }

    /**
     * checkGreater
     * 
     * @param <T> value type
     * @param name value (field) name
     * @param value value to check to be greater as minimum min
     * @param min minimum value
     */
    public static final <T extends Comparable<T>> void checkGreater(String name, T value, T min) {
        if (!isGreater(value, min))
            throw new FormattedException("tsl2nano.valuesizefailure", new Object[] { name, min });
    }

    public CUtil() {
        super();
    }

}