/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 30.10.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.persistence.replication;

import java.util.LinkedList;
import java.util.List;

/**
 * The replication object holds the entity instance to persist (only if istransient = false). a have havy weight means,
 * a lot of objects depend on this object.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class ReplicationObject {
    Object instance;
    long weight;
    boolean istransient;
    List<ReplicationObject> depends = new LinkedList<ReplicationObject>();
    /**
     * constructor
     * @param instance
     * @param weight
     * @param istransient
     * @param depends
     */
    protected ReplicationObject(Object instance, long weight, boolean istransient, List<ReplicationObject> depends) {
        super();
        this.instance = instance;
        this.weight = weight;
        this.istransient = istransient;
        this.depends = depends;
    }
}
