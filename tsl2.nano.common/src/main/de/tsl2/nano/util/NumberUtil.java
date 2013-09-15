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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import de.tsl2.nano.log.LogFactory;

import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.format.FormatUtil;
import de.tsl2.nano.util.bean.BeanClass;

/**
 * Some utils for numbers and comparables
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class NumberUtil {
    private static final Log LOG = LogFactory.getLog(NumberUtil.class);

    static DecimalFormat numberFormat;
    static {
        numberFormat = (DecimalFormat) NumberFormat.getNumberInstance();
        //to have the same type (BigDecimal) on all numbers.
        numberFormat.setParseBigDecimal(true);
        LOG.debug("NumberUtil using DecimalFormat with current state:" + numberFormat.toPattern());
    }

    /**
     * fill the numberPrefix with fillChar to have the length 'fixLength'.
     * 
     * @param numberPrefix origin number
     * @param fixLength desired length
     * @param fillChar desired filling numbers
     * @return fix-length number
     */
    public static final Long fixLengthNumber(Long numberPrefix, int fixLength, char fillChar) {
        final String numberStr = numberPrefix != null ? numberPrefix.toString() : "";
        final String fullNumberString = StringUtil.fixString(numberStr, fixLength, fillChar, true);
        return Long.valueOf(fullNumberString);
    }

    /**
     * provides the given numbers wrapped into an array of bigdecimals
     * 
     * @param numbers
     * @return array filled with given numbers - wrapped into {@link BigDecimal}s.
     */
    public static final BigDecimal[] getBigDecimals(Number... numbers) {
        BigDecimal[] result = new BigDecimal[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            result[i] = new BigDecimal(numbers[i].doubleValue());
        }
        return result;
    }

    /**
     * fast implementation to create a bigdecimal array with {@link BigDecimal#ZERO}.
     * 
     * @param count array length
     * @return filled array.
     */
    public static final BigDecimal[] getEmptyNumbers(int count) {
        BigDecimal[] result = new BigDecimal[count];
        for (int i = 0; i < count; i++) {
            result[i] = BigDecimal.ZERO;
        }
        return result;
    }

    /**
     * subtracts all subtractions from value
     * 
     * @param value base value
     * @param subtractions numbers to subtract from value
     * @return the rest
     */
    public static final BigDecimal subtract(BigDecimal value, BigDecimal... subtractions) {
        for (final BigDecimal s : subtractions) {
            value = value.subtract(s);
        }
        return value;
    }

    /**
     * builds a sum of all values
     * 
     * @param values numbers to add
     * @return the sum
     */
    public static final BigDecimal add(BigDecimal... values) {
        BigDecimal value = new BigDecimal(0);
        for (final BigDecimal s : values) {
            value = value.add(s);
        }
        return value;
    }

    /**
     * builds a sum of all values. the value-array may be of any {@link Number} implementation. the addition will be
     * done by constructing new BigDecimals on each value.
     * 
     * @param values numbers to add
     * @return the sum
     */
    public static final BigDecimal add(Number... values) {
        BigDecimal value = new BigDecimal(0);
        for (final Number s : values) {
            value = value.add(new BigDecimal(s.doubleValue()));
        }
        return value;
    }

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
     * checks a number to be an instance and to have a value different from zero.
     * 
     * @param number number to check
     * @return true, if number is null or = 0.
     */
    public static final boolean isEmpty(Number number) {
        return number == null || number.doubleValue() == 0;
    }

    /**
     * isPositive
     * 
     * @param number number
     * @return true, if number = 0
     */
    public static final boolean isZero(BigDecimal number) {
        return BigDecimal.ZERO.compareTo(number) == 0;
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
    public static final <T extends Object & Comparable<? super T>> boolean intersects(T from1, T to1, T from2, T to2) {
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
    public static final <T extends Object & Comparable<? super T>> boolean includes(T from1, T to1, T from2, T to2) {
        return from1.compareTo(from2) <= 0 && to1.compareTo(to2) >= 0;
    }

    /**
     * isPositive
     * 
     * @param number number
     * @return true, if number > 0
     */
    public static final boolean isPositive(BigDecimal number) {
        return BigDecimal.ZERO.compareTo(number) < 0;
    }

    /**
     * isNegative
     * 
     * @param number number
     * @return true, if number < 0
     */
    public static final boolean isNegative(BigDecimal number) {
        return BigDecimal.ZERO.compareTo(number) > 0;
    }

    /**
     * isNotNegative
     * 
     * @param number number
     * @return true, if number >= 0
     */
    public static final boolean isNotNegative(BigDecimal number) {
        return BigDecimal.ZERO.compareTo(number) <= 0;
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
     * converts a string to a {@link BigDecimal}. the current {@link Locale} will be used!
     * 
     * @param numberAsString number as string (with pattern for current locale)
     * @return instance of {@link BigDecimal} or null, if parsing failed
     */
    public static final BigDecimal getBigDecimal(String numberAsString) {
        try {
            return (BigDecimal) FormatUtil.getDefaultFormat(BigDecimal.class, true).parseObject(numberAsString);
        } catch (final ParseException e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    /**
     * delegates to {@link Integer#highestOneBit(int)}
     * 
     * @param decimal decimal number containing bits
     * @return highest bit in decimal number
     */
    public static final int highestOneBit(int decimal) {
        return Integer.highestOneBit(decimal);
    }

    /**
     * highestBitPosition
     * @param decimal number
     * @return number of trailing zeros or -1 if zero.
     */
    public static final int highestBitPosition(int decimal) {
        if (decimal == 0)
            return -1;
        return Integer.numberOfTrailingZeros(highestOneBit(decimal));
    }

    /**
     * bitToDecimal. to do the other direction (e.g. decimalToBit), call {@link #highestOneBit(int)}.
     * 
     * @param bit bit field to be converted into a decimal number
     * @return decimal representation of given bit
     */
    public static final int bitToDecimal(int bit) {
        return (int) 1 << bit;//Math.pow(2, bit);//Float.floatToIntBits(bit);
    }

    /**
     * toggles (removes or adds) the given bitsToFilter from/to the given bit-field
     * 
     * @param field bit field
     * @param bitsToFilter bits to remove or add
     * @return toggled bit field
     */
    public static final int toggleBits(int field, int...bitsToFilter) {
        for (int i = 0; i < bitsToFilter.length; i++) {
            field = hasBit(field, bitsToFilter[i]) ? field - bitsToFilter[i] : field | bitsToFilter[i];
        }
        return field;
    }
    
    /**
     * filters (removes) the given bitsToFilter from the given bit-field
     * 
     * @param field bit field
     * @param bitsToFilter bits to remove from bit field
     * @return filtered bit field
     */
    public static final int filterBits(int field, int... bitsToFilter) {
        for (int i = 0; i < bitsToFilter.length; i++) {
            field = hasBit(field, bitsToFilter[i]) ? field - bitsToFilter[i] : field;
        }
        return field;
    }

    /**
     * removes all bits between lowest bit and highest bit from given bit field.
     * 
     * @param field bit field
     * @param low lowest bit to filter (eliminate). please provide only the bit position: e.g. 10 instead of 1 << 10.
     * @param high highest bit to filter (eliminate). please provide only the bit position: e.g. 10 instead of 1 << 10.
     * @return filtered bit field
     */
    public static final int filterBitRange(int field, int low, int high) {
        for (int i = low; i <= high; i++) {
            int b = 1 << i;
            if (field < b)
                break;
            field = hasBit(field, b) ? field - b : field;
        }
        return field;
    }

    /**
     * filters (retaines) the given bitsToFilter from the given bit-field. bitsToFilter should be bit values - not bit
     * positions!
     * 
     * @param field bit field
     * @param bitsToFilter bit values (no bit positions) to remove from bit field
     * @return filtered bit field
     */
    public static final int retainBits(int field, int... bitsToFilter) {
        int f = 0;
        for (int i = 0; i < bitsToFilter.length; i++) {
            f += hasBit(field, bitsToFilter[i]) ? bitsToFilter[i] : 0;
        }
        return f;
    }

    /**
     * delegates to {@link #hasBit(int, int, boolean)} using oneOfThem=true.
     */
    public static final boolean hasBit(int bit, int... availableBits) {
        int mask = 0;
        for (int i = 0; i < availableBits.length; i++) {
            mask |= availableBits[i];
        }
        return hasBit(mask, bit, true);
    }

    /**
     * delegates to {@link #hasBit(int, int, boolean)} using oneOfThem=true.
     */
    public static final boolean hasBit(int mask, int bit) {
        return hasBit(mask, bit, true);
    }

    /**
     * evaluates whether the given number contains the bits of the given mask.
     * 
     * @param number number to evaluate
     * @param mask - an OR combination of bits
     * @param oneOfThem if true, only one of the given combination must be contained in the event to return true
     * @return true, if number contains the given bits (mask).
     */
    public static final boolean hasBit(int mask, int number, boolean oneOfThem) {
        return (number & mask) >= (oneOfThem ? 1/*=any*/: number);
    }

    /**
     * delegates to {@link #getNumberAndStringComparator(Format, boolean)} with stringOnly= false.
     */
    public static Comparator getNumberAndStringComparator(final Format df) {
        return getNumberAndStringComparator(df, false);
    }

    /**
     * generic comparator: first numbers then text
     * 
     * @param df formatter to get a string representation of an object to compare
     * @param stringOnly true defines, all objects to compare are of type string (for performance aspects!)
     * @return comparator using the string representation to compare
     */
    @SuppressWarnings("unchecked")
    public static Comparator getNumberAndStringComparator(final Format df, final boolean stringOnly) {
        return new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                String s1;
                String s2;
                //perhaps, use the natural order
                if (!stringOnly) {
                    if (!(o1 instanceof String) && o1 instanceof Comparable
                        && o2 != null
                        && o1.getClass().isAssignableFrom(o2.getClass())) {
                        return ((Comparable) o1).compareTo(o2);
                    } else {
                        s1 = df.format(o1);
                        s2 = df.format(o2);
                    }
                } else {
                    s1 = (String) o1;
                    s2 = (String) o2;
                }
                //show, if we can compare numbers
                Number n1 = extractNumber(s1);
                Number n2 = extractNumber(s2);
                if (n1 != null && n2 != null && n1.getClass().isAssignableFrom(n2.getClass())) {
                    return ((Comparable) n1).compareTo(n2);
                } else { //ok, we compare strings
                    return s1.compareToIgnoreCase(s2);
                }
            }
        };

    }

    /**
     * checks, if string is (complete) parseable as number and returns the number or null
     * 
     * @param s1 string perhaps containing a number
     * @return number, if string is parseable as number or null
     */
    public static Number extractNumber(String s1) {
        try {
            //to check, if the whole string s1 was parsed (the parser may parse only the first part of s1), we need the parse position
            ParsePosition pos = new ParsePosition(0);
            Number number = numberFormat.parse(s1, pos);
            if (pos.getIndex() < s1.length())
                return null;
            return number;
        } catch (final Exception ex) {
            return null;
        }
    }

    /**
     * delegates to {@link DateUtil#currentTimeSeconds()}. This call is only unique every second!
     * 
     * @return local unique int number
     */
    public static int getLocalUniqueInt() {
        return DateUtil.currentTimeSeconds();
    }

    /**
     * isNumber
     * 
     * @param instanceOrClass
     * @return true, if given class is a number (immutable or primitive)
     */
    public static boolean isNumber(Class<?> type) {
        return Number.class.isAssignableFrom(type) || short.class.isAssignableFrom(type)//short may be a problem to format!
            || int.class.isAssignableFrom(type)
            || long.class.isAssignableFrom(type)
            || float.class.isAssignableFrom(type)
            || double.class.isAssignableFrom(type);
    }

    public static boolean isInteger(Class<?> type) {
        return BeanClass.isAssignableFrom(Integer.class, type) || BeanClass.isAssignableFrom(Long.class, type)
            || BeanClass.isAssignableFrom(Short.class, type) || BeanClass.isAssignableFrom(BigInteger.class, type);
    }

    public static boolean isFloating(Class<?> type) {
        return BeanClass.isAssignableFrom(Float.class, type) || BeanClass.isAssignableFrom(Double.class, type)
            || BeanClass.isAssignableFrom(BigDecimal.class, type);
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
}
