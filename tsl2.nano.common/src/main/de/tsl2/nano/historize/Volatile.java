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

/**
 * solution for getter methods doing a long time calculation to evaluate their value - but called more than once in a
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
    /** life cycle of given value */
    long period;
    /** value to hold for a short time */
    T value;

    /**
     * constructor
     * 
     * @param period of milliseconds to hold the value. this should be a value smaller than 1sec (< 1000)
     * @param value (optional) initial value.
     */
    public Volatile(long period, T value) {
        super();
        this.period = period;
        if (value != null)
            set(value);
    }

    public boolean expired() {
        boolean expired = System.currentTimeMillis() > start + period;
        if (expired)
            value = null;
        return expired;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
        start = System.currentTimeMillis();
    }
}
