/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Apr 13, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrimitiveUtil;

/**
 * Some utils for numbers and comparables
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class NumberUtil extends BitUtil {
    static DecimalFormat numberFormat() {
    	DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getNumberInstance();
        //to have the same type (BigDecimal) on all numbers.
        numberFormat.setParseBigDecimal(true);
        LOG.debug("NumberUtil using DecimalFormat with current state:" + numberFormat.toPattern());
        return numberFormat;
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
        BigDecimal value = BigDecimal.ZERO;
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
        BigDecimal value = BigDecimal.ZERO;
        for (final Number s : values) {
            value = value.add(new BigDecimal(s.doubleValue()));
        }
        return value;
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
     * checks all given numbers to be zero. if at least one value is not zero, it returns false.
     * 
     * @param numbers numbers to check
     * @return true, if all numbers = 0
     */
    public static final boolean isAllZero(BigDecimal... numbers) {
        if (numbers.length == 0) {
            throw new IllegalArgumentException("At least one number has to be given!");
        }
        for (int i = 0; i < numbers.length; i++) {
            if (BigDecimal.ZERO.compareTo(numbers[i]) != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * hasEqualSigns
     * 
     * @param numbers
     * @return
     */
    public static final boolean hasEqualSigns(BigDecimal... numbers) {
        checkMinParameterCount(numbers, 2);
        int sign = 0, s;
        for (int i = 0; i < numbers.length; i++) {
            s = numbers[i].signum();
            if (sign == 0 && s != 0) {
                sign = s;
            }
            if (s != 0 && sign != s) {
                return false;
            }
        }
        return true;
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
     * converts a string to a {@link BigDecimal}. the current {@link Locale} will be used!
     * 
     * @param numberAsString number as string (with pattern for current locale)
     * @return instance of {@link BigDecimal} or null, if parsing failed
     */
    public static final BigDecimal getBigDecimal(String numberAsString) {
        try {
            return (BigDecimal) FormatUtil.getDefaultFormat(BigDecimal.class, true).parseObject(numberAsString);
        } catch (final ParseException e) {
//            ManagedException.forward(e);
            return null;
        }
    }
    
    /**
     * delegates to {@link #getNumberAndStringComparator(Format, boolean)} with stringOnly= false.
     */
    public static Comparator getNumberAndStringComparator(final Format df) {
        return getNumberAndStringComparator(df, false);
    }

    /**
     * generic comparator: first comparable(value) then comparabe(format(value))
     * 
     * @param df formatter to get a string representation of an object to compare
     * @param stringOnly true defines, all objects to compare are of type string (for performance aspects!)
     * @return comparator using interface comparable or, if not present, the string representation to compare
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
            Number number = numberFormat().parse(s1, pos);
            if (pos.getIndex() < s1.length()) {
                return null;
            }
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
     * checks the given string to be a 'ganze' number
     * 
     * @param value
     * @return if value is parseable to a number
     */
    public static boolean isNumber(Object value) {
        try {
            if (value == null)
                return false;
            return new BigDecimal(value.toString()) != null;
        } catch (NumberFormatException ex) {
            //ok, not a number
            return false;
        }
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
                || double.class.isAssignableFrom(type)
                || byte.class.isAssignableFrom(type);
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
     * getDefaultInstance
     * 
     * @param numberType
     * @return default instance (value=0) of the given number type
     */
    public static <T extends Number> T getDefaultInstance(Class<T> numberType) {
        //to respect all number types we do the step through the string converter
        return numberType.isPrimitive() ? PrimitiveUtil.getDefaultValue(numberType) : BeanClass.createInstance(
            numberType, String.valueOf(0));
    }

    /**
     * convenience to check a number for NaN.
     * @param number to be checked
     * @return true, if number is {@link Double} and {@link Double#isNaN()} is true.
     */
    public static boolean isNaN(Object number) {
        return number != null && number instanceof Double && ((Double)number).isNaN();
    }
    
    /**
     * tries to convert any object to an identifying long number (bijective)
     * @param obj any object
     * @return number, representing this object
     */
//    @InverseFunction(methodName = "fromNumber", parameters = {long.class, Class.class}, compareParameterIndex = 0)
    public static long toNumber(Object obj) {
        if (obj instanceof Date)
            return ((Date)obj).getTime();
        else if (obj instanceof Number)
            return ((Number)obj).longValue();
        else if (obj instanceof Enum)
        	return ((Enum)obj).ordinal();
        else if (ObjectUtil.isStandardType(obj))
            return new BigInteger(1, obj.toString().getBytes()).longValue();
        else
        	return obj.hashCode();
    }

    public static <T> T fromNumber(long identifier, Class<T> type) {
        if (String.class.isAssignableFrom(type))
            return (T) StringUtil.fromDecString(String.valueOf(identifier));
        else if (Enum.class.isAssignableFrom(type))
        	return type.getEnumConstants()[(int) identifier];
        else
            return BeanClass.createInstance(type, identifier);
    }

    public static double random(double min, double max) {
    	return Math.random() * (max - min) + min;
    }
    public static boolean roundAbout(Number first, Number second) {
        Number dev = Double.valueOf(System.getProperty("tslnano.number.deviation.max", "0.00000000000000001"));
        return hasMaxDeviation(first, second, dev);
    }
    public static boolean hasMaxDeviation(Number first, Number second, Number deviation) {
        return second.doubleValue() - first.doubleValue() < deviation.doubleValue();
    }
	public static ArrayList<Integer> numbers(int end) {
		return numbers(0, end, 1);
	}
	public static ArrayList<Integer> numbers(int start, int end, int step) {
		ArrayList<Integer> dupList = new ArrayList<>((end - start) / step);
		for (int i = start; i < end; i+=step) dupList.add(i);
		return dupList;
	}
	public static double scaled(double n, int scale) {
		double p = Math.pow(10, scale);
		return (int)(n * p) / (double)scale;
	}
}
