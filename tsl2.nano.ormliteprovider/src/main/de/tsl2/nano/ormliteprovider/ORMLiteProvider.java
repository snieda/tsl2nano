/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 08.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.ormliteprovider;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

/**
 * 
 * @author ts
 * @version $Revision$
 */
public class ORMLiteProvider implements EntityManagerFactory {
    Collection<EntityManager> ems;
    Map<String, Object> props;
    /**
     * constructor
     */
    public ORMLiteProvider() {
        ems = new LinkedList<EntityManager>();
        props = new LinkedHashMap<String, Object>();
    }

    @Override
    public void close() {
        for (EntityManager em : ems) {
            em.close();
        }
        ems = null;
    }

    @Override
    public EntityManager createEntityManager() {
        return createEntityManager(getProperties());
    }

    @Override
    public EntityManager createEntityManager(Map arg0) {
        de.tsl2.nano.ormliteprovider.EntityManager em = new de.tsl2.nano.ormliteprovider.EntityManager(arg0);
        ems.add(em);
        return em;
    }

    @Override
    public Cache getCache() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Metamodel getMetamodel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> getProperties() {
        return props;
    }

    @Override
    public boolean isOpen() {
        return ems != null;
    }

}
