/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 28.01.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.historize;

import de.tsl2.nano.core.util.Util;

/**
 * Same as an Expiring Reference. solution for getter methods doing a long time calculation to evaluate their value - but called more than once in a
 * short time. mostly, the value wont change between two user actions, so this class provides a mechanism to hold the
 * evaluated value for a very short time.
 * <p/>
 * use:
 * 
 * <pre>
 * if (myVolatileValue.expired()) {
 *  ...evaluate the value...
 *  myVolatileValue.set(myNewValue);
 * }
 * return myVolatileValue.get();
 * </pre>
 * 
 * @author Tom
 * @version $Revision$
 */
public class Volatile<T> {
    /** start of time period to hold the new value */
    long start;
    /** life cycle of given value (timout) */
    long period;
    /** value to hold for a short time */
    T value;

    public Volatile(long period) {
        this(period, null);
    }
    
    /**
     * constructor
     * 
     * @param period of milliseconds to hold the value. this should be a value smaller than 1sec (< 1000)
     * @param value (optional) initial value.
     */
    public Volatile(long period, T value) {
        super();
        this.period = period;
        if (value != null) {
            set(value);
        }
    }

    /**
     * expired
     * @return true, if value is to old.
     */
    public boolean expired() {
        boolean expired = System.currentTimeMillis() > start + period;
        if (expired) {
            value = null;
        }
        return expired;
    }

    public T get() {
        return expired() ? null : value;
    }

    public T invalidate() {
        T last = get();
       value = null;
       start = 0;
       return last;
    }
    public T set(T value) {
        this.value = value;
        activate();
        return value;
    }

    /**
     * activate
     */
    protected void activate() {
        start = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return Util.toString(getClass(), "timeout", period);
    }
}
