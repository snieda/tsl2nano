/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 23, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.collection;

/**
 * The {@link ReferenceMap} of apache commons is overwritten to add temporarily hard references. If a new object was
 * added (to a weak map), but a reference will be created some milliseconds later, the value may be garbage collected
 * already. This map holds a collection - the {@link TimedReferences} to don't lose the object to early.
 * <p/>
 * To set the duration of the hard references, call {@link #setTimeToBeOld(long)}, giving the milliseconds to hold an
 * object at least.
 * <p/>
 * Only the {@link #put(Object, Object)} method was overridden, {@link #putAll(java.util.Map)} should call this
 * {@link #put(Object, Object)} method. Removing values will not change the {@link TimedReferences}!
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class TimedReferenceMap<V> extends ReferenceMap<Object, V> {
    /** serialVersionUID */
    private static final long serialVersionUID = -2946119815999384729L;

    /** hard references to be hold for a minimum time */
    TimedReferences<V> timedReferences;

    /**
     * constructor
     */
    public TimedReferenceMap() {
        this(true);
    }
    
    /**
     * constructor
     * @param weak
     */
    public TimedReferenceMap(boolean weak) {
        super(weak);
        initTimedReferences();
    }

    /**
     * initTimedReferences
     */
    void initTimedReferences() {
        timedReferences = new TimedReferences<V>();
    }

    /**
     * setTimeToBeOld - time to be hold
     * 
     * @param timeToBeOld
     */
    public void setTimeToBeOld(long timeToBeOld) {
        timedReferences.setPeriodToBeOld(timeToBeOld);
    }

    /**
     * removeOldReferences
     */
    public void removeOldReferences() {
        timedReferences.removeOldTemporaries();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V put(Object key, V value) {
        timedReferences.add(value);
        return super.put(key, value);
    }
}
