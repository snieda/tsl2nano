/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: TS, Thomas Schneider
 * created on: Jan 11, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.service.util;

import static de.tsl2.nano.service.util.ServiceUtil.addMemberExpression;
import static de.tsl2.nano.service.util.ServiceUtil.createBetweenStatement;
import static de.tsl2.nano.service.util.ServiceUtil.createExampleStatement;
import static de.tsl2.nano.service.util.ServiceUtil.createStatement;
import static de.tsl2.nano.service.util.ServiceUtil.getId;
import static de.tsl2.nano.service.util.ServiceUtil.getIdName;
import static de.tsl2.nano.service.util.ServiceUtil.getLogInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.Query;
import javax.security.auth.Subject;

import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.service.util.batch.Part;
import de.tsl2.nano.service.util.finder.AbstractFinder;
import de.tsl2.nano.service.util.finder.Finder;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.serviceaccess.aas.principal.UserPrincipal;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.bean.BeanAttribute;
import de.tsl2.nano.util.bean.BeanClass;

/**
 * provides some basic service access methods to work with beans.
 * 
 * @author TS
 * 
 */
@Stateless
@SuppressWarnings({"rawtypes", "unchecked"})
public class GenericServiceBean extends NamedQueryServiceBean implements IGenericService, IGenericLocalService {

//    private Map<Class<?>, Object> beanIdCache = new Hashtable<Class<?>, Object>();

    /** {@inheritDoc} */
    @Override
    public <T> Collection<T> findAll(Class<T> beanType, Class... lazyRelations) {
        return findAll(beanType, -1, lazyRelations);
    }

    /** {@inheritDoc} */
    @Override
    public <T> Collection<T> findAll(Class<T> beanType, int maxResult, Class... lazyRelations) {
        checkContextSecurity();
        if (isVirtualEntity(beanType)) {
            if (isNamedQuery(beanType))
                return findByNamedQuery(beanType, getNamedQueryByArguments(beanType));
        }
        final StringBuffer qStr = createStatement(beanType);
        LOG.debug(qStr);
        Query query = entityManager.createQuery(qStr.toString());
        query = query.setMaxResults(maxResult != -1 ? maxResult : getMaxResult());
        //a findAll should only be done on 'configuration' tables
        //QUESTION: why does the query perform poor on activated cache????
        query.setHint("org.hibernate.cacheable", new Boolean(true));
        query.setHint("org.hibernate.readOnly", new Boolean(true));
        logTrace(query);
        return fillTree(query.getResultList(), lazyRelations);
    }

    /** {@inheritDoc} */
    @Override
    public <H, T> Collection<T> findMembers(H holder, Class<T> beanType, String attributeName, Class... lazyRelations) {
        checkContextSecurity();
        final StringBuffer qStr = createStatement(beanType);
        addMemberExpression(qStr, holder, beanType, attributeName);
        LOG.debug(qStr);
        Query query = entityManager.createQuery(qStr.toString());
        query = query.setMaxResults(getMaxResult());
        query.setParameter(1, getId(holder));
        logTrace(query);
        return fillTree(query.getResultList(), lazyRelations);
    }

    /** {@inheritDoc} */
    @Override
    public <H, T> Collection<H> findHolders(T member, Class<H> holderType, String attributeName, Class... lazyRelations) {
        checkContextSecurity();
        final StringBuffer qStr = createStatement(holderType);
        final String idAttribute = getIdName(member);
        qStr.append(", " + member.getClass().getSimpleName()
            + " t1\n where t1."
            + idAttribute
            + " = ? and t1 member of t."
            + attributeName);
        LOG.debug(qStr);
        Query query = entityManager.createQuery(qStr.toString());
        query = query.setMaxResults(getMaxResult());
        query.setParameter(1, getId(member));
        logTrace(query);
        return fillTree(query.getResultList(), lazyRelations);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T findById(Class<T> beanType, Object id, Class... lazyRelations) {
        checkContextSecurity();
        if (isVirtualEntity(beanType)) {
            if (isNamedQuery(beanType)) {
                final Collection<T> result = findByNamedQuery(beanType, getNamedQueryByArguments(beanType, id), id);
                if (result.size() > 1) {
                    LOG.warn("findById (" + beanType
                        + ", "
                        + id
                        + ") found more than one unbound entity:"
                        + StringUtil.toString(result, 100));
                }
                return result.size() > 0 ? result.iterator().next() : null;
            }
        }
        final T bean = entityManager.find(beanType, id);

        if (bean == null) {
            return null;
        }
        return fillTree(Arrays.asList(bean), lazyRelations).iterator().next();
    }

    /**
     * helper for all finders
     * 
     * @param <T> bean type
     * @param result instance
     * @param lazyRelations relation types to fill
     * @return perhaps filled object tree
     */
    protected final <T> Collection<T> fillTree(Collection<T> result, Class... lazyRelations) {
        if (lazyRelations != null && lazyRelations.length > 0) {
            List<Class> relations = Arrays.asList(lazyRelations);
            for (T bean : result) {
                //if an object array is returned we do it for all objects
                Object[] b = (Object[]) (bean instanceof Object[] ? bean : new Object[] { bean });
                for (int i = 0; i < b.length; i++) {
                    resetTreeVars(b[i]);
                    fillTree(b[i], null, relations);
                    finishTree(b[i], lazyRelations);
                }
            }
        }
        return result;
    }

    /**
     * because fillTree() is recursive, we use this method by every caller to clean the data.
     * 
     * @param bean entity
     * @param lazyRelations relations to be filled
     */
    private final void finishTree(Object bean, Class... lazyRelations) {
        if (recurseLevel > 0 || instantiatedEntities.size() > 0) {
            LOG.debug("==> fillTree finished on bean:" + bean
                + ", recursionLevel="
                + recurseLevel
                + ", instantiated entities:"
                + instantiatedEntities.size());
        }
//        recurseLevel = 0;
//        instantiatedEntities.clear();
    }

    /**
     * fills recursive all given attribute-relations of type fillType.
     * 
     * @param bean bean, that must not already be serialized.
     * @param attributes relations. if null, all attributes will be filled
     * @param fillTypes (optional) relation types to fill. if null, all types will be filled.
     */
    private int recurseLevel = 0;
    private final int MAX_RECURSLEVEL = 10;
    private final Collection<BeanAttribute> accessedBeanAttributes = new LinkedHashSet<BeanAttribute>();
    private final Collection<Object> instantiatedEntities = new LinkedHashSet<Object>();

    private void fillTree(Object bean, String[] attributes, List<Class> fillTypes) {
        //TODO: to many checks through change-history. please clean.
        final Class<?> clazz = bean.getClass();
        Object relation;
        /*
         * do we use to instantiate non-collections? we do it to fulfill any mapping strategy.
         * the checkTypesOnly is used, if no attribute names were given
         * TODO: include the commented code after distributing kion milestone 0.7.0.
         */
        boolean checkTypesOnly = false;
        if (attributes == null) {
            attributes = /*isLazyLoadingOnlyOnOneToMany() ? new BeanClass(clazz).getMultiValueAttributes() : */new BeanClass(clazz).getAttributeNames();
            if (fillTypes != null && fillTypes.size() > 0)
                checkTypesOnly = true;
        }
        try {
            BeanAttribute beanAttribute;
            for (final String attribute : attributes) {
                beanAttribute = BeanAttribute.getBeanAttribute(clazz, attribute);
//                Class<?> relationType;
                /*
                 * for performance aspects, ask, if attribute-type is contained in given relation types!
                 * for collections, the above solution is available, as it is now possible to evaluate the content type
                 * of a collection on runtime without loading the collection (eager).
                 */
                Class<?> relationType = beanAttribute.getType();
                if (Collection.class.isAssignableFrom(relationType)) {
                    relationType = beanAttribute.getGenericType();
                }
                if (checkTypesOnly && !fillTypes.contains(relationType))
                    continue;

                /*
                 * now, check for eager loading
                 */
                relation = beanAttribute.getValue(bean);
                if (relation != null) {
                    // call any method to instantiate the indirect object
                    Collection<?> relationSet;
                    if (relation instanceof Collection) {
                        relationSet = (Collection<?>) relation;
                        /*
                         * fill it through size call. it is not possible to evaluate the collections
                         * content type on runtime without eager-loading the collection
                         */
                        if (relationSet.size() == 0 || fillTypes == null) {
                            continue;
                        }
                        relationType = relationSet.iterator().next().getClass();
                    } else {//simulate a collection to use the same code
                        relationSet = Arrays.asList(relation);
                        //fill it through object method call
                        relation.hashCode();
                        relationType = beanAttribute.getType();
                    }
                    if ((fillTypes != null && fillTypes.contains(relationType)) && BeanContainerUtil.isPersistable(relationType)) {
                        for (final Object item : relationSet) {
                            if (instantiatedEntities.contains(item)) {
                                continue;
                            }
                            instantiatedEntities.add(item);
                            if (!accessedBeanAttributes.contains(beanAttribute)) {
                                accessedBeanAttributes.add(beanAttribute);
                                //on model cycles or to many fillTypes, we do a break on MAX_RECURSLEVEL!
                                LOG.debug("==> fillTree: attribute=" + beanAttribute.getName()
                                    + ", recursionLevel="
                                    + recurseLevel
                                    + ", instantiated entities:"
                                    + instantiatedEntities.size());
                                if (recurseLevel++ > MAX_RECURSLEVEL) {
                                    throw new FormattedException("instantiateLazyRelationship: max recurs level " + MAX_RECURSLEVEL
                                        + " exceeded evaluating attribute "
                                        + beanAttribute.getName()
                                        + ". Please check datamodel for cycles!\ninstantiated entities:\n"
                                        + StringUtil.toFormattedString(instantiatedEntities, 20, true));
                                }
                            }
                            fillTree(item, null, fillTypes);
                        }
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error(e);
            ForwardedException.forward(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T instantiateLazyRelationship(T bean) {
        final Object id = getId(bean);
        if (id != null) {
            return (T) instantiateLazyRelationship(bean.getClass(), id, null, null);
        } else {
            LOG.warn("no bean id found ==> nothing to do!");
            return bean;
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> T instantiateLazyRelationship(Class<T> clazz, Object beanId, String[] attributes) {
        return instantiateLazyRelationship(clazz, beanId, attributes, null);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T instantiateLazyRelationship(Class<T> clazz, Object beanId, List<Class> fillTypes) {
        return instantiateLazyRelationship(clazz, beanId, null, fillTypes);
    }

    /** {@inheritDoc} */
    protected <T> T instantiateLazyRelationship(Class<T> clazz,
            Object beanId,
            String[] attributes,
            List<Class> fillTypes) {
        LOG.info("instantiating lazy relation on " + clazz
            + " with id:"
            + beanId
            + "--> (special-attributeNames)"
            + attributes);
        final T bean = findById(clazz, beanId);
        if (bean == null) {
            throw new FormattedException("couldn''t find bean of type " + clazz
                + " with id: "
                + beanId
                + ".\nPossible causes are: null values on not-nullable attributes of that bean - or a relation!");
        }
        return instantiateLazyRelationship(bean, attributes, fillTypes);
    }

    /**
     * fills recursive all attribute-relations of type fillType.
     * 
     * @param clazz type of bean to get through beanId
     * @param beanId bean id to load
     * @param attributes (optional) relations
     * @param fillTypes (optional) relation types to fill. if null, all types will be filled.
     */
    @Override
    public <T> T instantiateLazyRelationship(T bean, String[] attributes, List<Class> fillTypes) {
        resetTreeVars(bean);
        fillTree(bean, attributes, fillTypes);
        if (fillTypes != null) {
            finishTree(bean, fillTypes.toArray(new Class[0]));
        }
        return bean;
    }

    /**
     * resetTreeVars
     * 
     * @param <T>
     * @param bean
     */
    private <T> void resetTreeVars(T bean) {
        recurseLevel = 0;
        accessedBeanAttributes.clear();
        instantiatedEntities.clear();
        instantiatedEntities.add(bean);
    }

    /** {@inheritDoc} */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T> T persist(T bean, Class... lazyRelations) {
        return persist(bean, true, true, lazyRelations);
    }

    /** {@inheritDoc} */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T> T persist(T bean, boolean refreshBean, boolean flush, Class... lazyRelations) {
        return persistNoTransaction(bean, refreshBean, flush, lazyRelations);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T persistNoTransaction(T bean, boolean refreshBean, boolean flush, Class... lazyRelations) {
        checkContextSecurity();
        final Class<T> beanType = (Class<T>) (bean != null ? bean.getClass() : null);
        if (isVirtualEntity(beanType)) {
            if (isNamedQuery(beanType))
                return persistByNamedQuery(bean);
        }
        // try {
        /*
         * The merge operation is clever enough to automatically detect whether
         * the merging of the detached instance has to result in an insert or
         * update. In other words, you don't have to worry about passing a new
         * instance (and not a detached instance) to merge(), the entity manager
         * will figure this out for you.
         */
        //WORKAOURND (for TopLink!): on new items with new relations it doesn't work
//        if (getId(bean) == null)
//            entityManager.persist(bean);
//        else
        bean = entityManager.merge(bean);

        if (flush) {
            entityManager.flush(); // force the SQL insert and triggers to run
        }
        if (refreshBean) {
            entityManager.refresh(bean); //re-read the state (after the trigger executes)
        }
        return fillTree(Arrays.asList(bean), lazyRelations).iterator().next();
        // } catch (Exception ex) {
        // //catch it and throw a new one. otherwise, the server (toplink) will
        // //catch it prints only a warning. the client would only see a
        // transaction exception
        // throw new EJBException(ex);
        // }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T> Collection<T> persistCollection(Collection<T> beans, Class... lazyRelations) {
        final Collection<T> newBeans = new ArrayList<T>(beans.size());
        for (final T bean : beans) {
            newBeans.add(persistNoTransaction(bean, false, false, lazyRelations));
        }
        entityManager.flush();
        return newBeans;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Object[] persistAll(Object... beans) {
        final Object[] newBeans = new Object[beans.length];
        for (int i = 0; i < newBeans.length; i++) {
            newBeans[i] = persistNoTransaction(beans[i], false, false);
        }
        entityManager.flush();
        return newBeans;
    }

    /** {@inheritDoc} */
    @Override
    // @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(Object bean) {
        checkContextSecurity();
        final Class<?> beanType = (bean != null ? bean.getClass() : null);
        if (isVirtualEntity(beanType)) {
            if (isNamedQuery(beanType))
                removeByNamedQuery(bean);
            return;
        }
        // try {
        /*
         * first: refresh the bean. perhaps it is loaded in a transaction that was marked as rollbackonly
         */
        bean = refresh(bean);
//        bean = entityManager.merge(bean);
        entityManager.remove(bean);
        entityManager.flush(); // force the SQL insert and triggers to run
        // } catch (Exception ex) {
        // //catch it and throw a new one. otherwise, the server (toplink) will
        // //catch it prints only a warning. the client would only see a
        // transaction exception
        // throw new ForwardedException(ex);
        // }
    }

    /** {@inheritDoc} */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeCollection(Collection<Object> beans) {
        if (beans != null) {
            for (Object bean : beans) {
                remove(bean);
            }
        }
    }

    /** {@inheritDoc} */
    // @Override
    @Override
    public <T> T refresh(T bean) {
        //the following works only, if bean was loaded in the current transaction
        //entityManager.refresh(bean);

        /*
         * reloads the bean - to be loaded in the current transaction!
         */
        return (T) entityManager.find(bean.getClass(), getId(bean));
    }

    /** {@inheritDoc} */
    @Override
    public <T> Collection<T> findByExample(T exampleBean, boolean caseInsensitive, Class... lazyRelations) {
        return findByExample(exampleBean, caseInsensitive, false, lazyRelations);
    }

    /** {@inheritDoc} */
    @Override
    public <T> Collection<T> findByExampleLike(T exampleBean, boolean caseInsensitive, Class... lazyRelations) {
        return findByExample(exampleBean, caseInsensitive, true, lazyRelations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T findByExample(T exampleBean, Class... lazyRelations) {
        Collection<T> collection = findByExample(exampleBean, false);
        if (collection.size() > 1) {
            throw new FormattedException("swartifex.multiple.items", new Object[] { exampleBean });
        }
        collection = fillTree(collection, lazyRelations);
        return collection.size() > 0 ? collection.iterator().next() : null;
    }

    /** {@inheritDoc} */
    public <T> Collection<T> findByExample(T exampleBean,
            boolean caseInsensitive,
            boolean useLike,
            Class... lazyRelations) {
        checkContextSecurity();
        StringBuffer qStr = new StringBuffer();
        final Collection<?> parameter = createExampleStatement(qStr, exampleBean, useLike, caseInsensitive);
        Query query = entityManager.createQuery(qStr.toString());
        query = query.setMaxResults(getMaxResult());
        query = setParameter(query, parameter);
        logTrace(query);
        return fillTree(query.getResultList(), lazyRelations);
    }

    /** {@inheritDoc} */
    @Override
    public <T> Collection<T> findBetween(T firstBean, T secondBean, boolean caseInsensitive, Class... lazyRelations) {
        return findBetween(firstBean, secondBean, caseInsensitive, -1, lazyRelations);
    }

    /** {@inheritDoc} */
    @Override
    public <T> Collection<T> findBetween(T firstBean,
            T secondBean,
            boolean caseInsensitive,
            int maxResult,
            Class... lazyRelations) {
        checkContextSecurity();
        final Class<T> beanType = (Class<T>) (firstBean != null ? firstBean.getClass()
            : secondBean != null ? secondBean.getClass() : null);
        if (isVirtualEntity(beanType)) {
            if (isNamedQuery(beanType))
                return findByNamedQuery(beanType, getNamedQueryByArguments(beanType));
        }
        StringBuffer qStr = new StringBuffer();
        Collection<?> parameter = createBetweenStatement(qStr, firstBean, secondBean, caseInsensitive);
        Query query = entityManager.createQuery(qStr.toString());
        query = query.setMaxResults(maxResult != -1 ? maxResult : getMaxResult());
        query = setParameter(query, parameter);
        logTrace(query);
        return fillTree(query.getResultList(), lazyRelations);
    }

    /** {@inheritDoc} */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int executeQuery(String queryString, boolean nativeQuery, Object[] args) {
        checkContextSecurity();
        Query query;
        LOG.debug(queryString);
        if (nativeQuery) {
            query = entityManager.createNativeQuery(queryString);
        } else {
            query = entityManager.createQuery(queryString);
        }
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                //parameters are 1-based!
                query.setParameter(i + 1, args[i]);
            }
        }
        logTrace(query);
        return query.executeUpdate();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<?> findByQuery(String queryString, boolean nativeQuery, Object[] args, Class... lazyRelations) {
        return findByQuery(queryString, nativeQuery, -1, args, lazyRelations);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<?> findByQuery(String queryString,
            boolean nativeQuery,
            int maxResult,
            Object[] args,
            Class... lazyRelations) {
        checkContextSecurity();
        Query query;
        LOG.debug(queryString);
        if (nativeQuery) {
            query = entityManager.createNativeQuery(queryString);
        } else {
            query = entityManager.createQuery(queryString);
        }
        query = query.setMaxResults(maxResult != -1 ? maxResult : getMaxResult());
        if (args != null) {
            if (ServiceUtil.useNamedParameters(queryString))
                query = ServiceUtil.setNamedParameters(query, args);
            else
                query = ServiceUtil.setParameters(query, args);
        }
        logTrace(query);
        return fillTree(query.getResultList(), lazyRelations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object findValueByQuery(String queryString, boolean nativeQuery, Object... args) {
        checkContextSecurity();
        Query query;
        LOG.debug(queryString);
        if (nativeQuery) {
            query = entityManager.createNativeQuery(queryString);
        } else {
            query = entityManager.createQuery(queryString);
        }
        if (ServiceUtil.useNamedParameters(queryString))
            query = ServiceUtil.setNamedParameters(query, args);
        else
            query = ServiceUtil.setParameters(query, args);
        logTrace(query);
        return query.getSingleResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object findItemByQuery(String queryString, boolean nativeQuery, Object[] args, Class... lazyRelations) {
        Collection<?> collection = findByQuery(queryString, nativeQuery, args, lazyRelations);
        if (collection.size() > 1) {
            throw new FormattedException("swartifex.multiple.items", new Object[] { StringUtil.fixString(queryString,
                25,
                ' ',
                true) });
        }
        collection = fillTree(collection, lazyRelations);
        return collection.size() > 0 ? collection.iterator().next() : null;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<?> findByQuery(String queryString,
            boolean nativeQuery,
            Map<String, ?> args,
            Class... lazyRelations) {
        checkContextSecurity();
        Query query;
        LOG.debug(queryString);
        if (nativeQuery) {
            query = entityManager.createNativeQuery(queryString);
        } else {
            query = entityManager.createQuery(queryString);
        }
        query = query.setMaxResults(getMaxResult());
        final Set<String> nameSet = args.keySet();
        for (final String name : nameSet) {
            query.setParameter(name, args.get(name));
        }
        logTrace(query);
        return fillTree(query.getResultList(), lazyRelations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <FINDER extends AbstractFinder<T>, T> java.util.Collection<T> find(FINDER... finder) {
        LinkedList<Object> parameter = new LinkedList<Object>();
        LinkedList<Class<Object>> lazyRelations = new LinkedList<Class<Object>>();
        String qStr = Finder.createQuery(parameter, lazyRelations, finder);
        Query query = entityManager.createQuery(qStr);
        query = query.setMaxResults(getMaxResult());
        query = setParameter(query, parameter);
        logTrace(query);
        return fillTree(query.getResultList(), lazyRelations.toArray(new Class[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Part<T>[] findBatch(Part<T>... batchParts) {
        for (int i = 0; i < batchParts.length; i++) {
            batchParts[i].setResult((Collection<T>) find(batchParts[i].getFinders()));
        }
        return batchParts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getUser(Subject subject, Class<T> userEntity, String userIdAttribute) {
        final Set<UserPrincipal> principals = subject.getPrincipals(UserPrincipal.class);
        final UserPrincipal userPrincipal = principals.iterator().next();

        T transUser = null;
        try {
            transUser = userEntity.newInstance();
        } catch (final Exception e) {
            ForwardedException.forward(e);
        }
        BeanAttribute.getBeanAttribute(userEntity, userIdAttribute).setValue(transUser, userPrincipal.getName());
        final Collection<T> bcol = findByExample(transUser, true);
        if (bcol.size() != 1) {
            throw new FormattedException("swartifex.multiple.items", new Object[] { userPrincipal.getName(),
                userEntity,
                userEntity });
        }

        return bcol.iterator().next();
    }

    /**
     * only for tests - creates an empty server side factory.
     */
    @Override
    public void initServerSideFactories() {
        if (!ServiceFactory.isInitialized()) {
            ServiceFactory.createInstance(this.getClass().getClassLoader());
            ServiceFactory.instance().createSession(null,
                null,
                null,
                new LinkedList<String>(),
                new LinkedList<String>(),
                null);
        }
    }

}