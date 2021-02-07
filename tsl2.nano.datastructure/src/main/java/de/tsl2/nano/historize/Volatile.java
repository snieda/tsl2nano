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

import java.util.function.Supplier;

import de.tsl2.nano.core.util.Util;

/**
 * Same as an Expiring Reference. solution for getter methods doing a long time calculation to evaluate their value - but called more than once in a
 * short time. mostly, the value wont change between two user actions, so this class provides a mechanism to hold the
 * evaluated value for a very short time.
 * <p/>
 * use 1:
 * 
 * <pre>
 * if (myVolatileValue.expired()) {
 *  ...evaluate the value...
 *  myVolatileValue.set(myNewValue);
 * }
 * return myVolatileValue.get();
 * </pre>
 * 
 * use 2 (use a callback as supplier, may be an performance problem on high frequency calls):
 * 
 * <pre>
 * get( () -> evalNewValueIfExpired() );
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
    /** if true, {@link #expired()} and {@link #get()} will reset the value on expiring */
	private boolean hard;

    public Volatile(long period) {
        this(period, null);
    }
    
    public Volatile(long period, T value) {
    	this(period, value, false);
    }
    /**
     * constructor
     * 
     * @param period of milliseconds to hold the value. this should be a value smaller than 1sec (< 1000)
     * @param value (optional) initial value.
     */
    public Volatile(long period, T value, boolean hard) {
        super();
        this.period = period;
        this.hard = hard;
        if (value != null) {
            set(value);
        }
    }

    public void setHardResetOnExpiring(boolean hard) {
		this.hard = hard;
	}
    
    /**
     * expired
     * @return true, if value is to old. if #hard was set, the value will be reseted
     */
    public boolean expired() {
        boolean expired = System.currentTimeMillis() > start + period;
        if (expired && hard) {
            value = null;
        }
        return expired;
    }

    public T get() {
        return expired() && hard ? null : value;
    }

    public T get(Supplier<T> newValueOnExpired) {
    	return expired() ? set(newValueOnExpired.get()) : value;
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
