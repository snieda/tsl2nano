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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;

import de.tsl2.nano.Environment;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.service.util.AbstractStatelessServiceBean;
import de.tsl2.nano.service.util.GenericServiceBean;
import de.tsl2.nano.service.util.IGenericBaseService;

/**
 * NOT FINISHED YET!
 * <p/>
 * Overrides the base service methods of {@link GenericServiceBean} to do actions on a list of jpa EntitiyManagers. The
 * first entity manager is the root/main to do the service on. all other entity managers will be used for replications.
 * 
 * <pre>
 * Technical Mechanism:
 *  - on getting data by queries, the root/main entity manager will be used to evaluate the data. the others will persist this new data.
 *  - on storing new or changed data, all entity managers will do the same on their connection.
 *  - works only if auto-commit = true
 * </pre>
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class GenericReplicatingServiceBean extends GenericServiceBean {

    protected List<IGenericBaseService> replicationServices;

    private static final Log LOG = LogFactory.getLog(GenericReplicatingServiceBean.class);
    
    /**
     * constructor
     */
    public GenericReplicatingServiceBean(EntityManager entityManager, boolean createReplication) {
        this(entityManager, createReplication ? createStandardReplication() : new LinkedList<IGenericBaseService>());
        
    }

    /**
     * constructor
     * 
     * @param enitityManager
     * @param replications
     */
    public GenericReplicatingServiceBean(EntityManager entityManager, List<IGenericBaseService> replicationServices) {
        super();
        this.entityManager = entityManager;
        this.replicationServices = replicationServices;
    }

    /**
     * createStandardReplication
     * 
     * @return list filled with service on persistence-unit 'replication'
     */
    protected static List<IGenericBaseService> createStandardReplication() {
        return Arrays.asList((IGenericBaseService) new ReplicationServiceBean());
    }

    /**
     * setting default entitymanager.
     * 
     * @param index index of replication (TODO: how to evaluate a name of an entity manager?)
     */
    public synchronized void switchToConnection(int index) {
        entityManager = ((AbstractStatelessServiceBean) replicationServices.get(index)).connection();
    }

    @Override
    public EntityManager connection() {
        return entityManager;
    }

    protected void doForReplication(Runnable replicationJob) {
        LOG.debug("doing replication...");
        Thread rep = Executors.defaultThreadFactory().newThread(replicationJob);
        rep.setName("replication-service-job");
        rep.start();
    }

    @Override
    public Collection<?> findByQuery(String queryString,
            boolean nativeQuery,
            int startIndex,
            int maxResult,
            Object[] args,
            Map<String, ?> hints,
            Class... lazyRelations) {
        // TODO Auto-generated method stub
        final Collection<?> result = super.findByQuery(queryString, nativeQuery, startIndex, maxResult, args, hints, lazyRelations);

        //IMPROVE: how to encapsulate this loop?
        for (final IGenericBaseService repService : replicationServices) {
            doForReplication(new Runnable() {
                @Override
                public void run() {
                    repService.persistCollection(result);
                }
            });
        }
        return result;
    }

    @Override
    public <T> T persistNoTransaction(final T bean,
            final boolean refreshBean,
            final boolean flush,
            final Class... lazyRelations) {
        T savedBean = super.persistNoTransaction(bean, refreshBean, flush, lazyRelations);

        //IMPROVE: how to encapsulate this loop?
        for (final IGenericBaseService repService : replicationServices) {
            doForReplication(new Runnable() {
                @Override
                public void run() {
                    repService.persistNoTransaction(bean, refreshBean, flush, lazyRelations);
                }
            });
        }
        return savedBean;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(final Object bean) {
        super.remove(bean);

        //IMPROVE: how to encapsulate this loop?
        for (final IGenericBaseService repService : replicationServices) {
            doForReplication(new Runnable() {
                @Override
                public void run() {
                    repService.remove(bean);
                }
            });
        }
    }
}
