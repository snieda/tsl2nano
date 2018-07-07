/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Aug 18, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.service.util;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * class to be used as base for all ejb entities.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@MappedSuperclass
public abstract class AbstractBaseEntity<ID> implements IPersistable<ID> {
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue
    private ID id;

    /**
     * constructor
     */
    public AbstractBaseEntity() {
//        this.id = UUID.randomUUID().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : super.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AbstractBaseEntity)) {
            return false;
        }
        final AbstractBaseEntity other = (AbstractBaseEntity) obj;
        return getId() != null ? getId().equals(other.getId()) : false;
    }

    /**
     * getId
     * 
     * @return id
     */
    public ID getId() {
        return id;
    }
    
    public void setId(ID id) {
        this.id = id;
    }
}
