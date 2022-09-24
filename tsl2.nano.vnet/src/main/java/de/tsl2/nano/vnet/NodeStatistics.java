/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Dec 20, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.vnet;

/**
 * to hold some statistics of a net-node.
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$ 
 */
public class NodeStatistics {
    /** count of direct notifications */
    long notifications = 0;
    /** count of notifications done by connections to this node */
    long notifiedThroughConnection = 0;
    /** total working time of this node */
    long workingTime = 0;
    
    /**
     * addWorkingTime
     * @param workingTime
     */
    public void addWorkingTime(long workingTime) {
        addWorkingTime(workingTime, true);
    }
    
    /**
     * addWorkingTime
     * @param workingTime
     * @param direct
     */
    public void addWorkingTime(long workingTime, boolean direct) {
        if (direct) {
            notifications++;
        } else {
            notifiedThroughConnection++;
        }
        this.workingTime = workingTime;
    }
    
    public void reset() {
        notifications = notifiedThroughConnection = workingTime = 0;
    }
    
    @Override
    public String toString() {
        return "notifications: " + notifications + ", connection-notifications: " + notifiedThroughConnection + ", " + "total working time: " + workingTime;
    }
}
