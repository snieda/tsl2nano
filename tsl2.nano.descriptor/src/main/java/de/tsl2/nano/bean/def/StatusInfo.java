/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 28.11.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.sql.Timestamp;

/**
 * Extends Status to provide some informations like start/end time and to have bean access methods 
 * @author Tom
 * @version $Revision$ 
 */
public class StatusInfo extends SStatus {
    /** serialVersionUID */
    private static final long serialVersionUID = 5448882712330708812L;

    Timestamp startedAt;
    Timestamp endedAt;
    
    public String getMessage() {
        return message();
    }
    
    public Timestamp getStartedAt() {
        return startedAt;
    }
    
    public Timestamp getEndedAt() {
        return endedAt;
    }
    
    public long start() {
        startedAt = new Timestamp(System.currentTimeMillis());
        return startedAt.getTime();
    }
    
    public long stop() {
        endedAt = new Timestamp(System.currentTimeMillis());
        return endedAt.getTime();
    }
    
    public boolean running() {
        return startedAt != null && endedAt == null;
    }
    
    public boolean finished() {
        return endedAt != null;
    }
    
    @Override
    public String toString() {
        return super.toString() + "{startedAt: " + startedAt + ", endedAt: " + endedAt + "}";
    }
}
