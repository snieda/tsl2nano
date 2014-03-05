/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 04.03.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.text.Format;
import java.util.Collection;

/**
 * 
 * @author Tom
 * @version $Revision$ 
 */
public interface IConstraint<T> {
    /** format-constraint for the attributes value */
    Format getFormat();

    /** value type */
    Class<T> getType();
    
    /** checks, if the given value is valid - should not throw an exception */
    IStatus checkStatus(String name, T value);

    /** checks, if the given value is valid - throws an exception */
    void check(String name, T value);

    /** default value for this attribute */
    T getDefault();

    
    /** returns the minimum value or null */
    Comparable<T> getMinimum();

    /** returns the maximum value or null */
    Comparable<T> getMaximum();

    /** returns the allowed values or null */
    Collection<T> getAllowedValues();

    /** maximum length - useful on strings */
    int getLength();

    /** scale - useful for numbers of type BigDecimal */
    int getScale();

    /** precision - useful for numbers of type BigDecimal */
    int getPrecision();

    /** should return true, if attribute-value may be null */
    boolean isNullable();

    /** define some basic attribute definitions */
    <C extends IConstraint<T>> C setBasicDef(int length, boolean nullable, Format format, T defaultValue);

    /** define number definitions - if the attribute is a number */
    <C extends IConstraint<T>> C  setNumberDef(int scale, int precision);

    /** defines a min/max range constraint. use {@link ValueCompare} to compare on changing values */
    <C extends IConstraint<T>> C  setRange(Comparable<T> min, Comparable<T> max);

    /**
     * defines all allowed values - if you call {@link #setRange(Comparable, Comparable)}, you shouldn't call this
     * method
     */
    <C extends IConstraint<T>> C  setRange(Collection<T> allowedValues);

    /** define constraining text format. use RegularExpressionFormat to define a regexp pattern */
    <C extends IConstraint<T>> C  setFormat(Format format);

    /** define type */
    <C extends IConstraint<T>> C  setType(Class<T> type);

    /** define maximum length */
    <C extends IConstraint<T>> C  setLength(int length);

    /** define scale */
    <C extends IConstraint<T>> C  setScale(int scale);

    /** define precision */
    <C extends IConstraint<T>> C  setPrecision(int precision);

    /** define nullable */
    <C extends IConstraint<T>> C  setNullable(boolean nullable);

    <C extends IConstraint<T>> C  setDefault(T defaultValue);
    
    /** usable for int definitions that are not bitfields */
    public static final int UNDEFINED = -1;

    /** usable for all int definitions like bitfiels type and style */
    public static final int UNSET = 0;
}
