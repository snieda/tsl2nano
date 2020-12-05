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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.metamodel.EntityType;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ITransformer;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.StringUtil;
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
    protected IGenericBaseService replication;
    protected boolean connected = true;
    protected boolean collectReplications = false;
    //WORKAROUND
    static Tree<Object, BeanValue> tree = new Tree<Object, BeanValue>();

    private static final Log LOG = LogFactory.getLog(GenericReplicatingServiceBean.class);

    private static long threadcount = 0;

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
        	Message.send(new RuntimeException("couldn't create standard replication", e));
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
            service
                .executeQuery(
                    ENV.get("service.connection.check.sql", "commit")/* + "grant select on " + Environment.get(IAuthorization.class).getUser()*/,
                    true, new Object[0]);
            return connected = true;
        } catch (Exception ex) {
//            //WORKAROUND for check
//            String msg = ex.toString();
//            boolean dbAnswer = msg != null && msg.contains("SQLException");
            if (/*!dbAnswer && */throwException) {
                ManagedException.forward(ex);
            }
            return connected = false;//!dbAnswer;
        }
    }

    protected void doForReplication(Runnable replicationJob) {
        ConcurrentUtil.startDaemon(replicationJob.toString() + ":" + threadcount++, replicationJob, true,
            ENV.get(UncaughtExceptionHandler.class));
    }

    @Override
    public Collection<?> findByQuery(final String queryString,
            boolean nativeQuery,
            int startIndex,
            int maxResult,
            Object[] args,
            Map<String, ?> hints,
            Class... lazyRelations) {
        final Collection<?> result;
        if (connected) {
            result = super.findByQuery(queryString, nativeQuery, startIndex, maxResult, args, hints, lazyRelations);
            if (result.size() > 0
                && BeanContainer.instance().isPersistable(
                    BeanClass.getDefiningClass(result.iterator().next().getClass()))) {
                //IMPROVE: how to encapsulate this loop?
                for (final IGenericBaseService repService : replicationServices) {
                    doForReplication(new Runnable() {
                        @Override
                        public void run() {
                            Message.send("preparing replication for " + result.size() + " main objects");
                            long totalCount = 0, notpersisted = 0;
                            Tree<Object, BeanValue> container = tree;
                            if (ENV.get("service.replication.singleTransaction", true)) {
                                LinkedList<Object> rep = new LinkedList<Object>();
                                for (Object object : result) {
                                    try {
                                        rep.add(object);
                                        addReplicationEntities(repService, rep, container,
                                            BeanClass.getDefiningClass(object.getClass()));
                                        Message.send("trying to replicate " + container.size() + " objects");
                                        totalCount += container.size();
                                        repService.persistCollection(new ArrayList(container.keySet()));
                                    } catch (Exception e) {
                                        notpersisted += container.size();
                                        Message.send(e.toString());
                                        //give the user a chance to see it before the next message...
                                        ConcurrentUtil.sleep(2000);
                                    }
                                    rep.clear();
                                    container.clear();
                                }
                            } else {
                                LinkedList<Object> rep = new LinkedList<Object>(result);
                                addReplicationEntities(repService, rep, container,
                                    BeanClass.getDefiningClass(result.iterator().next().getClass()));
                                Message.send("trying to replicate " + container.size() + " objects");
                                repService.persistCollection(container.keySet());
                                totalCount = container.size();
                            }
                            Message.send("replication of " + (totalCount - notpersisted) + " / " + totalCount + " objects done!");
                        }

                        @Override
                        public String toString() {
                            return "replication-job [query: " + StringUtil.toString(queryString, 40) + " -- result: "
                                + result.size() + " items]";
                        }
                    });
                }
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

    @SuppressWarnings("rawtypes")
    protected void addReplicationEntities(IGenericBaseService repService,
            List<Object> reps,
            Tree<Object, BeanValue> container, Class... excludes) {
        //WORKAROUND: make it available for replicationservicebean
//        ENV.addService(tree);
        addReplicationEntities(repService, reps, container, true, excludes);
    }

    /**
     * checks recursive the given list of objects to be replicated to the given service.
     * 
     * @param repService service to check the new replication objects
     * @param reps objects to be checked if already persisted
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void addReplicationEntities(IGenericBaseService repService,
            List<Object> reps,
            Tree<Object, BeanValue> container,
            boolean addRepsToContainer,
            Class... excludes) {
        LOG.debug("examining " + reps.size() + " objects for replication. collected replication objects: "
            + container.size());
        Object loadedBean;
        BeanClass bc;
        List<Class> lexcludes = Arrays.asList(excludes);
        ArrayList<Object> replications = new ArrayList<Object>(reps);
        List<Object> myAttrValues = new LinkedList<Object>();
        for (Object e : replications) {
            if (container.contains(e)) {
                continue;
            }
            if (addRepsToContainer) {
                container.add(e);
            }
            bc = BeanClass.getBeanClass(e.getClass());
            Collection<BeanAttribute> relationAttrs = bc.findAttributes(ManyToOne.class);
            relationAttrs.addAll(bc.findAttributes(OneToOne.class));
            myAttrValues.clear();
            for (BeanAttribute attr : relationAttrs) {
                if (lexcludes.contains(attr.getType()))
                    continue;
                Object attrValue = attr.getValue(e);
                if (attrValue != null) {
                    loadedBean = repService.refresh(attrValue);
                    if (loadedBean == null) {
                        if (!container.contains(attrValue)) {
                            myAttrValues.add(attrValue);
                        }
                        container.addDependencies(attrValue, BeanValue.getBeanValue(e, attr.getName()));
                    } else {
                        attr.setValue(e, loadedBean);
                    }
                }
            }
            //do it for oneToMany again --> but add only manyToOne/oneToOne to the container
            relationAttrs = bc.findAttributes(OneToMany.class);
            Collection<Object> values;
            BeanValue bv;
            for (BeanAttribute attr : relationAttrs) {
                if ((values = (Collection<Object>) attr.getValue(e)) != null && values.size() > 0) {
                    bv = BeanValue.getBeanValue(e, attr.getName());
//                    if (bv.composition()) {//set null-ids on compositions
//                        for (Object v : values) {
//                            Bean.getBean(v).getIdAttribute().setValue(v, null);
//                        }
//                    }
                    addReplicationEntities(repService, new ArrayList<Object>(values), container,
                        !bv.composition(), excludes);
                }
            }

            if (myAttrValues.size() > 0) {//--> performance
                if (LOG.isDebugEnabled())
                    LOG.debug(bc.getClazz().getName() + " --> " + StringUtil.toString(myAttrValues, -1));
                addReplicationEntities(repService, myAttrValues, container, true, excludes);
//                container.addAll(0, myAttrValues);
            }

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
                EntityType<? extends Object> entity;
                Object id;

                @Override
                public void run() {
                    repService.persist(bean);
                    if (!connected) {
                        entity = connection().getMetamodel().entity(bean.getClass());
                        String idName = entity.getId(bean.getClass()).getName();
                        id = BeanClass.getValue(bean, idName);
                        repService.persist(new ReplicationChange(entity.getName(), id));
                    }
                }

                @Override
                public String toString() {
                    return "replication-job [persist: " + (entity != null ? entity.getName() + "@" + id : bean.toString()) + "]";
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

                @Override
                public String toString() {
                    return "replication-job [remove: " + bean + "]";
                }
            });
        }
    }
}

/**
 * IMPROVE: use Net+Node from incubation
 * 
 * @param <T>
 * @param <D>
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("serial")
class Tree<T, D> extends HashMap<T, Collection<D>> {
    /** special tree keys to distinguish between standard items and dependencies */
    List<T> items = new LinkedList<T>();
    
    Comparator<T> comparator =
        new Comparator<T>() {
            @Override
            public int compare(Object o1, Object o2) {
                Collection<D> c1 = get(o1);
                Collection<D> c2 = get(o2);
                return c1 == null && c2 == null ? 0 : c1 == null ? 1 : c2 == null ? -1 : c1.size() > c2.size() ? -1 : 1;
            }
        };

    public void add(T node) {
        items.add(node);
    }

    public void addDependencies(T node, D... destination) {
        Collection<D> collection = get(node);
        if (collection == null) {
            collection = new LinkedList<D>();
            put(node, collection);
        }
        for (int i = 0; i < destination.length; i++) {
            collection.add(destination[i]);
        }
    }

    public boolean contains(T node) {
        return items.contains(node);
    }

    @Override
    public int size() {
        return items.size();
    }
    public boolean delete(T node, D destination) {
        items.remove(node);
        Collection<D> collection = get(node);
        if (collection != null) {
            return collection.remove(destination);
        }
        return false;
    }

    /**
     * provides a sorted keyset
     * {@inheritDoc}
     */
    @Override
    public Set<T> keySet() {
        ListSet<T> keys = new ListSet<T>(items);
        Collections.sort(keys, comparator);
        return keys;
    }

    /**
     * doFor
     * 
     * @param node
     * @param action
     */
    public void doFor(T node, ITransformer<D, D> action) {
        Collection<D> c = get(node);
        if (c != null) {
            for (D d : c) {
                action.transform(d);
            }
        }
        remove(node);
    }
}