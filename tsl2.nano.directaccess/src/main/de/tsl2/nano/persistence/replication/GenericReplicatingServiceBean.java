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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.metamodel.EntityType;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.classloader.ThreadUtil;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.service.util.AbstractStatelessServiceBean;
import de.tsl2.nano.service.util.GenericServiceBean;
import de.tsl2.nano.service.util.IGenericBaseService;
import de.tsl2.nano.serviceaccess.IAuthorization;

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
    protected IGenericBaseService replication;
    protected boolean connected = true;
    protected boolean collectReplications = false;

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
        ReplicationServiceBean rep;
        try {
            rep = new ReplicationServiceBean();
        } catch (Exception e) {
            //Ok, continue without replication - this shouldn't break the application!
            LOG.error("couldn't create standard replication", e);
            return new ArrayList<IGenericBaseService>();
        }
        return Arrays.asList((IGenericBaseService) rep);
    }

    protected IGenericBaseService getAvailableReplication() {
        if (replication == null) {
            for (Iterator<IGenericBaseService> it = replicationServices.iterator(); it.hasNext();) {
                IGenericBaseService r = it.next();
                if (checkConnection(r, false)) {
                    replication = r;
                    //TODO: do we really need this 'remove'? --> unsupportedoperation
//                    it.remove();
                    return replication;
                }
            }
            throw new IllegalStateException("No replication connection available!");
        }
        return replication;
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

    public boolean checkConnection(boolean throwException) {
        return checkConnection(this, throwException);
    }

    /**
     * checkConnection
     * 
     * @param service connection
     * @param throwException if true, the exception will be forwarded
     * @return true, if connection could be established
     */
    protected boolean checkConnection(IGenericBaseService service, boolean throwException) {
        try {
            //TODO: which statement to do...?
//            service.executeQuery("select now()", true, new Object[0]);
            /*
             * while there is no current transaction, the 'commit' should do nothing ;-)
             */
            service.executeQuery("commit"/* + "grant select on " + Environment.get(IAuthorization.class).getUser()*/,
                true, new Object[0]);
            return connected = true;
        } catch (Exception ex) {
//            //WORKAROUND for check
//            String msg = ex.toString();
//            boolean dbAnswer = msg != null && msg.contains("SQLException");
            if (/*!dbAnswer && */throwException)
                ManagedException.forward(ex);
            return connected = false;//!dbAnswer;
        }
    }

    protected void doForReplication(Runnable replicationJob) {
        ThreadUtil.startDaemon("replication-service-job", replicationJob, true,
            Environment.get(UncaughtExceptionHandler.class));
    }

    @Override
    public Collection<?> findByQuery(String queryString,
            boolean nativeQuery,
            int startIndex,
            int maxResult,
            Object[] args,
            Map<String, ?> hints,
            Class... lazyRelations) {
        final Collection<?> result;
        if (connected) {
            result = super.findByQuery(queryString, nativeQuery, startIndex, maxResult, args, hints, lazyRelations);

            //IMPROVE: how to encapsulate this loop?
            for (final IGenericBaseService repService : replicationServices) {
                doForReplication(new Runnable() {
                    @Override
                    public void run() {
                        LinkedList<Object> rep = new LinkedList<Object>(result);
                        addReplicationEntities(repService, rep, rep);
                        LOG.debug("trying to replicate " + rep.size() + " objects");
                        repService.persistCollection(rep);
                    }
                });
            }
        } else {
            //TODO: impl. mode collect
            result = getAvailableReplication().findByQuery(queryString,
                nativeQuery,
                startIndex,
                maxResult,
                args,
                hints,
                lazyRelations);
        }
        return result;
    }

    /**
     * checks recursive the given list of objects to be replicated to the given service.
     * 
     * @param repService service to check the new replication objects
     * @param reps objects to be checked if already persisted
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void addReplicationEntities(IGenericBaseService repService, List<Object> reps, List<Object> container) {
        Object loadedBean;
        BeanClass bc;
        ArrayList<Object> replications = new ArrayList<Object>(reps);
        List<Object> myAttrValues = new LinkedList<Object>();
        for (Object e : replications) {
            bc = BeanClass.getBeanClass(e.getClass());
            Collection<BeanAttribute> relationAttrs = bc.findAttributes(ManyToOne.class);
            relationAttrs.addAll(bc.findAttributes(OneToOne.class));
            myAttrValues.clear();
            for (BeanAttribute attr : relationAttrs) {
                Object attrValue = attr.getValue(e);
                if (attrValue != null && !container.contains(attrValue)) {
                    loadedBean = repService.refresh(attrValue);
                    if (loadedBean == null)
                        myAttrValues.add(attrValue);
                }
            }
            addReplicationEntities(repService, myAttrValues, container);
            container.addAll(0, myAttrValues);

            //do it for oneToMany again
//            myAttrValues.clear();
//            relationAttrs = bc.findAttributes(OneToMany.class);
//            for (BeanAttribute attr : relationAttrs) {
//                Collection<Object> oneToMany = (Collection<Object>) attr.getValue(e);
//                for (Object item : oneToMany) {
//                    if (item != null && !container.contains(item)) {
//                        loadedBean = repService.refresh(item);
//                        if (loadedBean == null)
//                            myAttrValues.add(item);
//                    }
//                }
//            }
//            addReplicationEntities(repService, myAttrValues, container);
//            container.addAll(0, myAttrValues);
        }
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
                    repService.persist(bean);
                    if (!connected) {
                        EntityType<? extends Object> entity = connection().getMetamodel().entity(bean.getClass());
                        String idName = entity.getId(bean.getClass()).getName();
                        Object id = BeanClass.getValue(bean, idName);
                        repService.persist(new ReplicationChange(entity.getName(), id));
                    }
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
