/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts, Thomas Schneider
 * created on: 12.07.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.configuration;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.Subject;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.service.util.IGenericService;
import de.tsl2.nano.service.util.batch.Part;
import de.tsl2.nano.service.util.finder.AbstractFinder;

/**
 * Configuration Service to configure bean types. only findBetween is implemented - only for the beanType
 * {@link BeanAttribute}, to return a full list of available attributes.
 * 
 * @author ts, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class ConfigServiceBean implements IGenericService {
    Map<Class<?>, Collection<?>> entityCache = new Hashtable<Class<?>, Collection<?>>();

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> findAll(Class<T> beanType, Class... lazyRelations) {
        return findAll(beanType, 0, Integer.MAX_VALUE, lazyRelations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> findAll(Class<T> beanType, int startIndex, int maxResult, Class... lazyRelations) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <H, T> Collection<T> findMembers(H holder, Class<T> beanType, String attributeName, Class... lazyRelations) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <H, T> Collection<H> findHolders(T member, Class<H> holderType, String attributeName, Class... lazyRelations) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T findByExample(T exampleBean, Class... lazyRelations) {
        //TODO: constrain result
        return (T) findAll(exampleBean.getClass()).iterator().next();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> findByExample(T exampleBean, boolean caseInsensitive, Class... lazyRelations) {
        //TODO: constrain result
        return (Collection<T>) findAll(exampleBean.getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> findByExampleLike(T exampleBean, boolean caseInsensitive, int startIndex, int maxResult, Class... lazyRelations) {
        //TODO: constrain result
        return (Collection<T>) findAll(exampleBean.getClass(), startIndex, maxResult);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T findById(Class<T> beanType, Object id, Class... lazyRelations) {
        Collection<T> all = findAll(beanType);
        for (T bean : all) {
            if (bean.hashCode() == (Integer) id) {
                return bean;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> findBetween(T firstBean, T secondBean, Class... lazyRelations) {
        return findBetween(firstBean, secondBean, true, 0, -1, lazyRelations);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> Collection<T> findBetween(T firstBean,
            T secondBean,
            boolean caseInsensitive,
            int startIndex,
            int maxResult,
            Class... lazyRelations) {
        if (firstBean instanceof IAttribute) {
            Class declaringClass = ((IAttribute) firstBean).getDeclaringClass();
            Bean bean = new Bean(declaringClass);
            return bean.getAttributes();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int executeQuery(String queryString, boolean nativeQuery, Object[] args) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<?> findByQuery(String queryString, boolean nativeQuery, Object[] args, Class... lazyRelations) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<?> findByQuery(String queryString,
            boolean nativeQuery,
            int startIndex,
            int maxresult,
            Object[] args,
            Map<String, ?> hints,
            Class... lazyRelations) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<?> findByQuery(String queryString,
            boolean nativeQuery,
            Map<String, ?> args,
            Class... lazyRelations) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object findItemByQuery(String queryString, boolean nativeQuery, Object[] args, Class... lazyRelations) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object findValueByQuery(String queryString, boolean nativeQuery, Object... args) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T persist(T bean, Class... lazyRelations) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T persist(T bean, boolean refreshBean, boolean flush, Class... lazyRelations) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T persistNoTransaction(T bean, boolean refreshBean, boolean flush, Class... lazyRelations) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> persistCollection(Collection<T> beans, Class... lazyRelations) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] persistAll(Object... beans) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T refresh(T bean) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Object bean) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCollection(Collection<Object> beans) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T instantiateLazyRelationship(Class<T> clazz, Object beanId, String[] attributes) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T instantiateLazyRelationship(Class<T> clazz, Object beanId, List<Class> fillTypes) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T instantiateLazyRelationship(T bean) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getUser(Subject subject, Class<T> userEntity, String userIdAttribute) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initServerSideFactories() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> findByNamedQuery(Class<T> beanType, String namedQuery, int maxResult, Object... args) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <FINDER extends AbstractFinder<T>, T> Collection<T> find(FINDER... finder) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Part<T>[] findBatch(Part<T>... batchParts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Properties getServerInfo() {
        return System.getProperties();
    }

}
