/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts, Thomas Schneider
 * created on: 19.05.2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.service.util;

import static de.tsl2.nano.service.util.ServiceUtil.getId;
import static de.tsl2.nano.service.util.ServiceUtil.getIdName;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;
import javax.persistence.Table;

import de.tsl2.nano.bean.BeanAttribute;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.util.StringUtil;

/**
 * provides services to work on beans with named queries. used by {@link GenericServiceBean}.
 * 
 * @author ts, Thomas Schneider
 * @version $Revision$
 */
@Stateless
public class NamedQueryServiceBean extends AbstractStatelessServiceBean implements INamedQueryService {

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> findByNamedQuery(Class<T> beanType, String namedQuery, Object... args) {
        //if no full path was given, we fill it through the beanType
        if (!namedQuery.contains(".")) {
            namedQuery = getNamedQueryPrefix(beanType) + namedQuery;
        }
        LOG.debug("query on entity '" + beanType
            + "' with named query '"
            + namedQuery
            + "' with arguments: "
            + StringUtil.toString(args, 100));
        Query query = connection().createNamedQuery(namedQuery);
        query = query.setMaxResults(getMaxResult());
        query = setParameter(query, Arrays.asList(args));
        logTrace(query);
        return query.getResultList();
    }

    /**
     * distinguish between an insert and update through the filled or not generic id field.
     * 
     * @param <T> bean type
     * @param bean bean instance
     * @return bean id
     */
    protected <T> T persistByNamedQuery(T bean) {
        final Class<T> beanType = (Class<T>) bean.getClass();
        final Object id = getId(bean);
        String namedQuery;
        /*
         * works only on a simple generic id field!
         */
        if (id != null) {
            namedQuery = getNamedQueryPrefix(beanType) + NAMEDQUERY_UPDATE;
        } else {
            //on inserts, we have to fill a generic id
            final String idName = getIdName(bean);
            BeanAttribute.getBeanAttribute(beanType, idName).setValue(bean, UUID.randomUUID());

            namedQuery = getNamedQueryPrefix(beanType) + NAMEDQUERY_INSERT;
        }
        final Bean b = new Bean(bean);
        findByNamedQuery(beanType, namedQuery, b.getValues());
        return bean;
    }

    /**
     * distinguish between an insert and update through the filled or not generic id field.
     * 
     * @param <T> bean type
     * @param bean bean instance
     * @return bean id
     */
    protected <T> void removeByNamedQuery(T bean) {
        final Class<T> beanType = (Class<T>) bean.getClass();
        final Object id = getId(bean);
        String namedQuery;
        /*
         * works only on a simple generic id field!
         */
        if (id == null) {
            LOG.warn("nothing to do for bean of type " + beanType + " with id:" + id + ". bean is not yet persisted!");
            return;
        } else {
            namedQuery = getNamedQueryPrefix(beanType) + NAMEDQUERY_DELETE;
        }
        findByNamedQuery(beanType, namedQuery, id);
    }

    /**
     * isVirtualEntity
     * 
     * @param <T> type of bean
     * @param beanType bean type
     * @return true, if no table is bound to entity
     */
    protected <T> boolean isVirtualEntity(Class<T> beanType) {
        return !beanType.isAnnotationPresent(Table.class);
    }

    /**
     * isNamedQuery
     * 
     * @param <T> type of bean
     * @param beanType bean type
     * @return true, if entity is annotated with {@link NamedQueries} or {@link NamedQuery} or
     *         {@link NamedNativeQueries} or {@link NamedNativeQuery}.
     */
    protected <T> boolean isNamedQuery(Class<T> beanType) {
        return beanType.isAnnotationPresent(NamedQueries.class) || beanType.isAnnotationPresent(NamedQuery.class)
            || beanType.isAnnotationPresent(NamedNativeQuery.class)
            || beanType.isAnnotationPresent(NamedNativeQueries.class);
    }

    /**
     * evaluates the query name by arguments
     * 
     * @param <T> type of bean
     * @param beanType bean type
     * @param args query arguments
     * @return named query name (like 'MyBeanClassPath.findById')
     */
    protected String getNamedQueryByArguments(Class<?> beanType, Object... args) {
        final String prefix = getNamedQueryPrefix(beanType);
        final int alength = args.length;
        switch (alength) {
        case 0:
            return prefix + NAMEDQUERY_ALL;
        case 1:
            return prefix + NAMEDQUERY_ID;
        case 2:
            return prefix + NAMEDQUERY_BETWEEN;
        default:
            throw FormattedException.implementationError("evaluation of named queries only available for zero or one argument (findAll and findById)",
                args);
        }
    }

    /**
     * getNamedQueryPrefix
     * 
     * @param beanType bean type
     * @return simple class name plus "."
     */
    public static final String getNamedQueryPrefix(Class<?> beanType) {
        return beanType.getName() + ".";
    }
}
