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

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * provides some basic service access methods to work with entity beans.
 * <p>
 * all finders are able to fill an object tree through given attribute types (-->layzRelations). only oneToMany
 * relations are lazy relations on default. it is possible to reload the object tree after having a bean:<br>
 * - {@link #instantiateLazyRelationship(Object)}: the bean will be reloaded and all relationships will be resolved
 * (poor performance)<br>
 * - {@link #instantiateLazyRelationship(Class, Object, List)}: a bean will be loaded with object tree given through
 * types.<br>
 * - {@link #instantiateLazyRelationship(Class, Object, String[])}: a bean will be loaded with given attributes.<br>
 * 
 * @author TS
 * 
 */
@SuppressWarnings("rawtypes")
public interface IGenericBaseService extends IStatelessService, INamedQueryService {
    /** find all beans of type beanType (statement cache will be used!) */
    public <T> Collection<T> findAll(Class<T> beanType, Class... lazyRelations);

    /**
     * find all beans of type beanType (statement cache will be used!)
     * 
     * @param <T> bean type
     * @param beanType bean type
     * @param maxResult (optional: set -1 to use no definition) maximum result count
     * @param lazyRelations (optional) pre-loaded lazy-relation types
     * @return filled collection with beans of type beantype
     */
    public <T> Collection<T> findAll(Class<T> beanType, int startIndex, int maxResult, Class... lazyRelations);

    /**
     * find all beans of type beanType beeing members of holder. useful if your beanType has no access to the holder.
     * <p>
     * 
     * <pre>
     * f.e.: 
     *   Parent (1) <-- (*) Child
     *   ==> but you want to get the parents children!
     * will result in:
     *   select t from Child t, Parent t1 
     *   where t1.ID = holder.ID 
     *   and t member of t1.{attributeName}
     * </pre>
     * 
     * @param <H> holder type
     * @param <T> member type
     * @param beanType member type to be collected
     * @param holder holder instance to get the members of (without direct access!)
     * @param attributeName
     * @return members of holder (member given by attributeName)
     */
    <H, T> Collection<T> findMembers(H holder, Class<T> beanType, String attributeName, Class... lazyRelations);

    /**
     * find all holders of the given member instance. useful if your member has no access to the holder. on composites
     * and aggregations you will get a collection holding only one instance.
     * <p>
     * 
     * <pre>
     * f.e.: 
     *   Parent (1) --> (*) Child
     *   ==> but you want to get a childs parent!
     * will result in:
     *   select t from Child t, Parent t1 
     *   where t.ID = member.ID 
     *   and t member of t1.{attributeName}
     * </pre>
     * 
     * @param <H> holder type
     * @param <T> member type
     * @param beanType member type to be collected
     * @param holder holder instance to get the members of (without direct access!)
     * @param attributeName
     * @return members of holder (member given by attributeName)
     */
    <H, T> Collection<H> findHolders(T member, Class<H> holderType, String attributeName, Class... lazyRelations);

    /**
     * tries to find the given bean - if more than one bean was found, an exception will be thrown.
     * 
     * @param <T> bean type
     * @param exampleBean example bean
     * @return exactly one bean!
     */
    <T> T findByExample(T exampleBean, Class... lazyRelations);

    /** find all beans with same attributes (only single value attributes!) as exampleBean */
    public <T> Collection<T> findByExample(T exampleBean, boolean caseInsensitive, Class... lazyRelations);

    /** find all beans with similar (like) attributes (only single value attributes!) as exampleBean */
    public <T> Collection<T> findByExampleLike(T exampleBean, boolean caseInsensitive, Class... lazyRelations);

    /** find bean with given id */
    public <T> T findById(Class<T> beanType, Object id, Class... lazyRelations);

    /** find all beans with same attributes (only single value attributes!) between first and second bean */
    public <T> Collection<T> findBetween(T firstBean, T secondBean, Class... lazyRelations);

    /**
     * finds all beans, having properties between firstBean and secondBean.
     * 
     * @param <T> beantype
     * @param firstBean minimum bean
     * @param secondBean maximum bean
     * @param caseInsensitive whether to search strings case insensitive
     * @param startIndex query start index
     * @param maxResult (optional: set -1 to use no definition) maximum result count
     * @param lazyRelations (optional) pre-loaded lazy-relation types
     * @return filled collection with beans of type T
     */
    public <T> Collection<T> findBetween(T firstBean,
            T secondBean,
            boolean caseInsensitive,
            int startIndex,
            int maxResult,
            Class... lazyRelations);

    /**
     * executes a given query. may contain create/alter/insert/update/delete statements.
     * @param queryString may be a jpa-ql (nativeQuery=false!) or sql string (nativeQuery=true)
     * @param nativeQuery should only be true, if you use pure sql
     * @param args if your queryString contains parameters (represented by questionmarks ('?'), they will be
     *            sequentially filled with the values of args
     * @return count of changed rows
     */
    public int executeQuery(String queryString, boolean nativeQuery, Object[] args);
    
    /**
     * find items by query. args are optional. if nativeQuery is true, a standard sql-query will be done
     * 
     * @param queryString may be a jpa-ql (nativeQuery=false!) or sql string (nativeQuery=true)
     * @param nativeQuery should only be true, if you use pure sql
     * @param args if your queryString contains parameters (represented by questionmarks ('?'), they will be
     *            sequentially filled with the values of args
     * @param lazyRelations (optional) one-to-many types to be filled before returning
     * @return result of query
     */
    public Collection<?> findByQuery(String queryString, boolean nativeQuery, Object[] args, Class... lazyRelations);

    /**
     * find items by query. args are optional. if nativeQuery is true, a standard sql-query will be done
     * 
     * @param queryString may be a jpa-ql (nativeQuery=false!) or sql string (nativeQuery=true)
     * @param nativeQuery should only be true, if you use pure sql
     * @param maxResult (optional: set -1 to use no definition) maximum result count
     * @param args if your queryString contains parameters (represented by questionmarks ('?'), they will be
     *            sequentially filled with the values of args
     * @param lazyRelations (optional) one-to-many types to be filled before returning
     * @return result of query
     */
    public Collection<?> findByQuery(String queryString, boolean nativeQuery, int maxresult, Object[] args, Class... lazyRelations);

    /**
     * find items by query. args are optional. if nativeQuery is true, a standard sql-query will be done
     * 
     * @param queryString may be a jpa-ql (nativeQuery=false!) or sql string (nativeQuery=true)
     * @param nativeQuery should only be true, if you use pure sql
     * @param args if your queryString contains parameters (represented by ':' + varname (e.g. :myvar), they will be
     *            filled with the values of args
     * @param lazyRelations (optional) one-to-many types to be filled before returning
     * @return result of query
     */
    public Collection<?> findByQuery(String queryString,
            boolean nativeQuery,
            Map<String, ?> args,
            Class... lazyRelations);

    /**
     * find one item by query. args are optional. if nativeQuery is true, a standard sql-query will be done. for further
     * informations, see {@link #findByQuery(String, boolean, Object[], Class...)}.
     */
    public Object findItemByQuery(String queryString, boolean nativeQuery, Object[] args, Class... lazyRelations);

    /**
     * find one value by query - fast way to get a single value like through 'count(*)' without packing it to a bean. 
     * BE SURE TO RETURN EXACTLY ONE VALUE! Little bit faster than {@link #findItemByQuery(String, boolean, Object[], Class...)}.
     * args are optional. if nativeQuery is true, a standard sql-query will be done. for further
     * informations, see {@link #findByQuery(String, boolean, Object[], Class...)}.
     */
    public Object findValueByQuery(String queryString, boolean nativeQuery, Object... args);

    /**
     * persists or merges the given object - committing a transaction and calling refresh and flush after.
     * {@link #persistNoTransaction(Object, boolean, boolean)} to work on bean-managed transactions
     */
    public <T> T persist(T bean, Class... lazyRelations);

    /**
     * persists or merges the given object - committing a transaction. if refresh and flush are false, you have a high
     * performance. use {@link #persistNoTransaction(Object, boolean, boolean)} to work on bean-managed transactions
     */
    public <T> T persist(T bean, boolean refreshBean, boolean flush, Class... lazyRelations);

    /**
     * persists or merges the given object without accessing a transaction. if refresh and flush are false, you have a
     * high performance.
     */
    public <T> T persistNoTransaction(T bean, boolean refreshBean, boolean flush, Class... lazyRelations);

    /** persists or merges the objects of the given collection - using one transaction. */
    public <T> Collection<T> persistCollection(Collection<T> beans, Class... lazyRelations);
    
    /** persists or merges the given objects - perhaps different entity types - and returns the new elements. Goal: doing that in one transaction! */
    public Object[] persistAll(Object... beans);

    /** refreshes the given object - reloads it in the current transaction / session! */
    public <T> T refresh(T bean);

    /** removes the given object */
    public void remove(Object bean);

    /** removes the objects of the given collection - using one transaction. */
    public void removeCollection(Collection<Object> beans);

}
