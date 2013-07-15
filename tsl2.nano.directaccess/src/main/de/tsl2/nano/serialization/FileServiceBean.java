/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts, Thomas Schneider
 * created on: 12.07.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.serialization;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import de.tsl2.nano.Environment;
import de.tsl2.nano.service.util.IGenericService;
import de.tsl2.nano.service.util.batch.Part;
import de.tsl2.nano.service.util.finder.AbstractFinder;
import de.tsl2.nano.util.FileUtil;

/**
 * Serialization Service for mocking or local storages. only some accessors are implemented
 * 
 * @author ts, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FileServiceBean implements IGenericService {
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
        Collection<?> allEntities = entityCache.get(beanType);
        if (allEntities == null) {
            allEntities = (Collection<?>) FileUtil.load(Environment.getConfigPath(beanType) + ".dat");
            entityCache.put(beanType, allEntities);
        }
        return (Collection<T>) allEntities;
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
        return (T) findAll(exampleBean.getClass());
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
    public <T> Collection<T> findByExampleLike(T exampleBean, boolean caseInsensitive, Class... lazyRelations) {
        //TODO: constrain result
        return (Collection<T>) findAll(exampleBean.getClass());
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
        //TODO: constrain result
        return (Collection<T>) findAll(firstBean.getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> findBetween(T firstBean, T secondBean, boolean caseInsensitive, int startIndex,

    int maxResult, Class... lazyRelations) {
        //TODO: constrain result
        return (Collection<T>) findAll(firstBean.getClass());
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
            int maxresult,
            Object[] args,
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
        return persist(bean, true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T persist(T bean, boolean refreshBean, boolean flush, Class... lazyRelations) {
        return persistNoTransaction(bean, true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T persistNoTransaction(T bean, boolean refreshBean, boolean flush, Class... lazyRelations) {
        Class<?> entityType = bean.getClass();
        FileUtil.save(Environment.getConfigPath(entityType) + ".dat", this.entityCache.get(entityType));
        return bean;
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
        for (int i = 0; i < beans.length; i++) {
            persistNoTransaction(beans[i], true, true);
        }
        return beans;
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
        //save current list to file
        persist(bean);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCollection(Collection<Object> beans) {
        for (Object object : beans) {
            remove(object);
        }
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
    public <T> Collection<T> findByNamedQuery(Class<T> beanType, String namedQuery, Object... args) {
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

}
