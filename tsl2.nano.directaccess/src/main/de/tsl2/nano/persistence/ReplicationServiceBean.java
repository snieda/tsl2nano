/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 16.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.persistence;

import javax.persistence.EntityManager;

/**
 * Service for persistence-unit 'replication'
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class ReplicationServiceBean extends GenericLocalServiceBean {

    /**
     * constructor
     */
    public ReplicationServiceBean() {
        this(createEntityManager("replication"));
    }

    /**
     * constructor
     * 
     * @param entityManager
     */
    public ReplicationServiceBean(EntityManager entityManager) {
        super(entityManager, false);
    }
}
