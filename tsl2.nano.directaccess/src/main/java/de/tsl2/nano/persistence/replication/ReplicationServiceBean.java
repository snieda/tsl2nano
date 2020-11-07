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
import javax.persistence.EntityNotFoundException;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ITransformer;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.persistence.GenericLocalServiceBean;
import de.tsl2.nano.service.util.ServiceUtil;

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
     * inserts all unpersisted relations - but in an undefined order. so we have to try to persist the collection for
     * several times. if no element was persisted in one loop, the trial and error job stops.
     */
    @Override
    public <T> Collection<T> persistCollection(Collection<T> beans, Class... lazyRelations) {
//        connection().getTransaction().begin();
        final Collection<T> newBeans = new LinkedHashSet<T>(beans.size());
        int count;
        int i = 0;
        while (beans.size() > 0) {
            count = beans.size();
            Message.send("starting replication-iteration " + ++i + " to persist " + count + " beans...");
            for (Iterator<T> it = beans.iterator(); it.hasNext();) {
                T bean = it.next();
                try {
                    bean = persistRep(bean);
//                    connection().detach(bean);
                    newBeans.add(bean);
                    it.remove();
                } catch (EntityNotFoundException ex) {
                    LOG.error(ex.toString());
                    //try to persist the not found entity first
//                    Object d = getDependentBean(ex);
//                    if (d != null) {
//                        persistRep(d);
//                        persistRep(bean);
//                    }
                } catch (Exception ex) {
                    LOG.error(ex.toString());
                    //some relations weren't saved yet - we do that hoping to find it later in the list
                }
            }
            if (count == beans.size()) {
                throw new RuntimeException("replication couldn't be done on:\n" + StringUtil.toFormattedString(beans, 300, true));
            }
        }
//        connection().getTransaction().commit();
        Message.send("replication done on " + newBeans.size() + " beans");
        return newBeans;
    }

    /**
     * persistRep
     * 
     * @param bean
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected <T> T persistRep(final T bean) {
        Object id = ServiceUtil.getId(bean);
        final T newBean;
        if (ENV.get("service.replication.autocommit", true))
            newBean = persist/*NoTransaction*/(bean);//, true, true));
        else
            newBean = persistNoTransaction(bean, true, true);
        Object pid = ServiceUtil.getId(bean);
        //on @generatedvalue jpa will always create it's own id on new items
        if (id != null && !id.equals(pid)) {
            String entity = bean.getClass().getSimpleName();
            String idColumn = ServiceUtil.getIdName(bean);
            executeQuery("update " + entity + " set " + idColumn + " = ?1 where " + idColumn + " = ?2", true,
                new Object[] { id, pid });
        }
        final Tree tree = GenericReplicatingServiceBean.tree;
        if (tree != null) {
            tree.doFor(bean, new ITransformer<BeanValue, BeanValue>() {
                @Override
                public BeanValue transform(BeanValue t) {
                    t.setValue(newBean);
                    return t;
                }
            });
        }
        return bean;
    }

//    private Object getDependentBean(Exception ex) {
//        String txt = ex.toString();
//        String stype = StringUtil.substring(txt, "Unable to find ", " with");
//        String id = StringUtil.substring(txt, "with id ", null);
//        if (stype == null || id == null)
//            return null;
//        BeanClass bc = BeanClass.createBeanClass(stype);
//        Object instance = bc.createInstance();
//        bc.setValue(instance, BeanContainer.getIdAttribute(instance).getName(), id);
//        Collection<Object> result = BeanContainer.instance().getBeansByExample(instance);
//        return result.size() > 0 ? result.iterator().next() : null;
////        return ServiceFactory.getGenService().findById(bc.getClazz(), id);
//    }

    @Override
    public <T> T persistNoTransaction(T bean, boolean refreshBean, boolean flush, Class... lazyRelations) {
        return super.persistNoTransaction(/*BeanUtil.copy(*/bean/*)*/, refreshBean, flush, lazyRelations);
    }
}
