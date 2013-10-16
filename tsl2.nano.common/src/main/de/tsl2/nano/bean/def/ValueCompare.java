/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Mar 9, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

/**
 * usable to compare bean values - perhaps on validation in {@link IAttributeDefinition}
 * @author Thomas Schneider
 * @version $Revision$ 
 */
public class ValueCompare<T extends Comparable<T>> implements Comparable<T> {
    BeanValue<T> value;

    /**
     * constructor
     * @param value
     */
    public ValueCompare(BeanValue<T> value) {
        super();
        this.value = value;
    }

    @Override
    public int compareTo(T o) {
        if (o == null)
            return 1;
        T v = value.getValue();
        return v != null ? v.compareTo(o) : -1;
    }
}
