/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 16.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.persistence.replication;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;

import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.persistence.GenericLocalServiceBean;

/**
 * Service for persistence-unit 'replication'
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class ReplicationServiceBean extends GenericLocalServiceBean {
    private static final Log LOG = LogFactory.getLog(GenericLocalServiceBean.class);
    
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

    /**
     * this extension tries to persist new data from source db to this replication db. the method
     * {@link #addReplicationEntities(de.tsl2.nano.service.util.IGenericBaseService, java.util.List, java.util.List)}
     * inserts all unpersisted relations - but in an undefined order. so we have to trie to persist the collection for
     * several times. if no element was persisted in one loop, the trial and error job stops.
     */
    @Override
    public <T> Collection<T> persistCollection(Collection<T> beans, Class... lazyRelations) {
//        connection().getTransaction().begin();
        final Collection<T> newBeans = new LinkedHashSet<T>(beans.size());
        int count = beans.size() + 1;
        while (beans.size() > 0) {
            count = beans.size();
            for (Iterator<T> it = beans.iterator(); it.hasNext();) {
                T bean = it.next();
                try {
                    newBeans.add(persist/*NoTransaction*/(bean));//, true, true));
                    it.remove();
                } catch (Exception ex) {
                    LOG.error(ex.toString());
                    //some relations weren't saved yet - we do that hoping to find it later in the list
                }
            }
            if (count == beans.size())
                throw new RuntimeException("replication couldn't be done on " + beans);
        }
//        connection().getTransaction().commit();
        return newBeans;
    }

    @Override
    public <T> T persistNoTransaction(T bean, boolean refreshBean, boolean flush, Class... lazyRelations) {
        return super.persistNoTransaction(/*BeanUtil.copy(*/bean/*)*/, refreshBean, flush, lazyRelations);
    }
}
