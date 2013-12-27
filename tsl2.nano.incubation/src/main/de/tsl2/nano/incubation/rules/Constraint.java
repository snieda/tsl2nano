/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 01.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.incubation.rules;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * Checks constraints of a given value
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Constraint<T extends Comparable<T>> {
    @Attribute
    Class<T> type;
    @Element
    T min;
    @Element
    T max;

    /**
     * constructor
     */
    public Constraint() {
    }
    
    public Constraint(Class<T> type) {
        this(type, null, null);
    }

    /**
     * constructor
     * 
     * @param type
     * @param min
     * @param max
     */
    public Constraint(Class<T> type, T min, T max) {
        super();
        this.type = type;
        this.min = min;
        this.max = max;
    }

    /**
     * check given value to be of right type and in range
     * 
     * @param value to be checked
     */
    public void check(T value) {
        if (value != null && !type.isAssignableFrom(value.getClass())) {
            throw new ClassCastException("value " + value + " must be of type " + type);
        } else if ((min != null && min.compareTo(value) > 0) || (max != null && max.compareTo(value) < 0)) {
            throw new IllegalArgumentException("value " + value + " must be in range: " + min + " to " + max);
        }
    }
}
