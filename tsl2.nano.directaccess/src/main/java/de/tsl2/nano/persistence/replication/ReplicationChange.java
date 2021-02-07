/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 19.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.persistence.replication;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Replication information for a data change to be persisted in the replication database - for later replication into
 * the origin database.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@Entity
public class ReplicationChange {
    /**
     * change time
     */
    @Temporal(TemporalType.TIMESTAMP)
    Date time;

    /**
     * database table name of change
     */
    @Column
    String table;

    /** table row id of changed data */
    @Column @Id
    Object id;

    protected ReplicationChange() {
	}
    
    /**
     * constructor
     * 
     * @param table
     * @param id
     */
    public ReplicationChange(String table, Object id) {
        super();
        this.time = new Date();
        this.table = table;
        this.id = id;
    }

    /**
     * @return Returns the time.
     */
    public Date getTime() {
        return time;
    }

    /**
     * @param time The time to set.
     */
    public void setTime(Date time) {
        this.time = time;
    }

    /**
     * @return Returns the table.
     */
    public String getTable() {
        return table;
    }

    /**
     * @param table The table to set.
     */
    public void setTable(String table) {
        this.table = table;
    }

    /**
     * @return Returns the id.
     */
    public Object getId() {
        return id;
    }

    /**
     * @param id The id to set.
     */
    public void setId(Object id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Replication(" + time + ": " + table + "." + id + ")";
    }
}
