/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 18, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.collection;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.log.LogFactory;

/**
 * This map holds all new values for a short time after creation - to avoid being deleted to fast in
 * weak-reference maps (see ReferenceMap(SOFT, WEAK)). Works on values that are not referenced in your application.
 * <p/>
 * For easiness we remove old entries only on adding new ones.
 * 
 * @author ts
 * @version $Revision$
 */
public class TimedReferences<T> {
    Log LOG = LogFactory.getLog(TimedReferences.class);
    
    /** definition of time to be hold (temporarily) */
    static final long TIMEOUT = 1000 * 60;
    long periodToBeOld = TIMEOUT;
    
    /** storing all values with a timestamp - to be removed after {@link #TIMEOUT} */
    Map<Long, T> tempHardRefs = new LinkedHashMap<Long, T>();
    long lastRemove = 0;

    /**
     * stores the given value with a timestamp to a linked-map and removes old entries (see {@link #isLongPeriod(long)}.
     * 
     * @param value new created value
     */
    public void add(T value) {
        //do that only after periods
        if (isLongPeriod(lastRemove))
            removeOldTemporaries();

        tempHardRefs.put(System.currentTimeMillis(), value);
    }   

    /**
     * removes values with an old timestamp
     */
    public void removeOldTemporaries() {
        lastRemove = System.currentTimeMillis();
        int removedElements = 0;
        int elements = tempHardRefs.size();
        //pack the keyset into a new instance to avoid concurred modification problems
        Set<Long> storageTimes = new HashSet<Long>(tempHardRefs.keySet());
        for (Long storageTime : storageTimes) {
            if (isLongPeriod(storageTime)) {
                tempHardRefs.remove(storageTime);
                removedElements++;
            } else {
                // if we found a short period time, the following times will be short, too.
                break;
            }
        }
        LOG.debug("removed " + removedElements + " of " + elements + " temporary elements");
    }

    /**
     * clear
     */
    public void clear() {
        tempHardRefs.clear();
    }
    
    /**
     * isLongPeriod
     * 
     * @param time time to be evaluated
     * @return true, if time is older than {@link #TIMEOUT}
     */
    private boolean isLongPeriod(long time) {
        return System.currentTimeMillis() - time > periodToBeOld;
    }

    /**
     * @return Returns the periodToBeOld.
     */
    public long getTimeout() {
        return periodToBeOld;
    }

    /**
     * @param periodToBeOld The periodToBeOld to set.
     */
    public void setTimeout(long periodToBeOld) {
        this.periodToBeOld = periodToBeOld;
    }
    
}