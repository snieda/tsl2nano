/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Jun 12, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.service.schedule;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

import de.tsl2.nano.action.IStatus;;

/**
 * Entry for a called job
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class JobHistoryEntry implements Serializable, Comparable {
    /** serialVersionUID */
    private static final long serialVersionUID = -7722123062397813057L;
    
    String name;
    long begin;
    long end;
    IStatus status;

    
    /**
     * constructor
     */
    public JobHistoryEntry() {
        super();
    }
    
    /**
     * constructor
     * @param job to extract the history informations from
     */
    public JobHistoryEntry(Job job) {
        this(job.getName(), job.getLastStart(), job.getLastStop(), job.getLastResult());
    }
    /**
     * constructor
     * 
     * @param name
     * @param begin
     * @param ende
     * @param status
     */
    public JobHistoryEntry(String name, long begin, long ende, IStatus status) {
        super();
        this.name = name;
        this.begin = begin;
        this.end = ende;
        this.status = status;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Returns the begin.
     */
    public long getBegin() {
        return begin;
    }

    /**
     * @return Returns the ende.
     */
    public long getEnd() {
        return end;
    }

    /**
     * @return Returns the begin.
     */
    public String getBeginAsString() {
        return DateFormat.getDateTimeInstance().format(new Date(begin));
    }

    /**
     * @return Returns the ende.
     */
    public String getEndAsString() {
        return DateFormat.getDateTimeInstance().format(new Date(end));
    }

    /**
     * @return Returns the status.
     */
    public IStatus getStatus() {
        return status;
    }
    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @param begin The begin to set.
     */
    public void setBegin(long begin) {
        this.begin = begin;
    }
    /**
     * @param end The end to set.
     */
    public void setEnd(long end) {
        this.end = end;
    }
    /**
     * @param status The status to set.
     */
    public void setStatus(IStatus status) {
        this.status = status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (begin ^ (begin >>> 32));
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        JobHistoryEntry other = (JobHistoryEntry) obj;
        if (begin != other.begin) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Object o) {
        JobHistoryEntry entry = (JobHistoryEntry) o;
        return new Long(begin).compareTo(entry.begin);
    }
}
