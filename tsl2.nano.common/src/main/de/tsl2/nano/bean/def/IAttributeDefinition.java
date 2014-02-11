/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Feb 15, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;
import java.text.Format;
import java.util.Collection;

import de.tsl2.nano.bean.IAttributeDef;

/**
 * full bean attribute definition - to validate attribute value.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IAttributeDefinition<T> extends IAttributeDef, Serializable {
    /** bean-class, holding the attribute */
    Class<?> getDeclaringClass();
    
    /** the attributes name */
    String getName();
    
    /** format-constraint for the attributes value */
    Format getFormat();
    
    /** evaluates the attribute value for the given bean instance. no generic type is given because the implementing 
     * base class BeanAttribute doesn't define that */
    T getValue(Object instance);
    
    /** sets a new value for the given instance */
    void setValue(Object instance, T value);
    
    /** the values class type */
    Class<T> getType();

    /** checks, if the given value is valid for this attribute - should not throw an exception */
    IStatus isValid(T value);

    /** default value for this attribute */
    T getDefault();

    /** description for the attribute */
    String getDescription();

    /** returns the minimum value or null */
    T getMininum();
    
    /** returns the maximum value or null */
    T getMaxinum();

    /** returns the allowed values or null */
    Collection<T> getAllowedValues();

    /**
     * @return whether the value type is a collection.
     */
    public boolean isMultiValue();
    
    /** maximum length - useful on strings */
    int getLength();

    /** scale - useful for numbers of type BigDecimal */
    int getScale();

    /** precision - useful for numbers of type BigDecimal */
    int getPrecision();

    /** should return true, if attribute-value may be null */
    boolean isNullable();

    /** should be true, if this attribute defines the id of the owning bean */
    boolean id();
    /** define some basic attribute definitions */
    IAttributeDefinition<T> setBasicDef(int length, boolean nullable, Format format, T defaultValue, String description);

    /** define number definitions - if the attribute is a number */
    IAttributeDefinition<T>  setNumberDef(int scale, int precision);

    /** defines a min/max range constraint. use {@link ValueCompare} to compare on changing values */
    IAttributeDefinition<T> setRange(Comparable<T> min, Comparable<T> max);

    /** defines all allowed values - if you call {@link #setRange(Comparable, Comparable)}, you shouldn't call this method */
    IAttributeDefinition<T> setRange(Collection<T> allowedValues);
    
    /** if parameter isId is true, the attribute will be handled as id-attribute for the owning bean */
    IAttributeDefinition<T> setId(boolean isId);

    /** if parameter isUnique is true, the attribute will be handled as unique-attribute for the owning bean */
    IAttributeDefinition<T> setUnique(boolean isUnique);

    /** define constraining text format. use RegularExpressionFormat to define a regexp pattern */
    IAttributeDefinition<T>  setFormat(Format format);

    /** define maximum length */
    IAttributeDefinition<T>  setLength(int length);

    /** define scale */
    IAttributeDefinition<T>  setScale(int scale);

    /** define precision */
    IAttributeDefinition<T>  setPrecision(int precision);

    /** define nullable */
    IAttributeDefinition<T>  setNullable(boolean nullable);

    /** returns the current attribute value status (ok, warn or error) */
    IStatus getStatus();
    
    /** returns additional/optional presentation informations */
    IPresentable getPresentation();
    
    /** returns additional/optional column presentation informations */
    IPresentableColumn getColumnDefinition();
    /** set definition for a column */
    void setColumnDefinition(int index, int sortIndex, boolean sortUpDirection, int width);

    /**
     * renames the attribute to be a relation from another bean. relation name parts are concatenated through '.'.
     */
    void setAsRelation(String string);

    /** attribute relation separator (like 'myattr1.relationattr.nextrelationattr' */
    public static final String REL_SEPARATOR = ".";
    
}
