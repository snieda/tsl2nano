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

import javax.ejb.Local;
import javax.security.auth.Subject;

/**
 * provides some basic service access methods to work with entity beans.
 * <p>
 * all finders are able to fill an object tree through given attribute types (-->layzRelations). only oneToMany
 * relations are lazy relations on default. it is possible to reload the object tree after having a bean:<br>
 * - {@link #instantiateLazyRelationship(Object, String[], List)Object)}: the bean will be used and all relationships
 * will be resolved<br>
 * - {@link #instantiateLazyRelationship(Class, Object, List)}: a bean will be loaded with object tree given through
 * types.<br>
 * - {@link #instantiateLazyRelationship(Class, Object, String[])}: a bean will be loaded with given attributes.<br>
 * <p>
 * TODO: seems to be not possible to inherit service methods from {@link IGenericService}, because the remote interface
 * {@link IGenericService} inherits that interface already. the server checks for unique used interfaces!
 * 
 * @author TS
 * 
 */
@Local
public interface IGenericLocalService extends IQueryService, IBatchService {
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
    public <T> Collection<T> findAll(Class<T> beanType, int maxResult, Class... lazyRelations);

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
    public <T> Collection<T> findBetween(T firstBean, T secondBean, boolean caseInsensitive, Class... lazyRelations);

    /**
     * finds all beans, having properties between firstBean and secondBean.
     * 
     * @param <T> beantype
     * @param firstBean minimum bean
     * @param secondBean maximum bean
     * @param caseInsensitive whether to search strings case insensitive
     * @param maxResult (optional: set -1 to use no definition) maximum result count
     * @param lazyRelations (optional) pre-loaded lazy-relation types
     * @return filled collection with beans of type T
     */
    public <T> Collection<T> findBetween(T firstBean,
            T secondBean,
            boolean caseInsensitive,
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

    /**
     * calls any method on the relation to invoke an instantiation of the given relation. this can't be done on a bean,
     * that was already serialized, so we have to get a 'fresh' source bean to call any method on the given attribute.
     * 
     * @param <T> type of bean
     * @param clazz type of source bean
     * @param beanId id of source bean
     * @param attributes optional relation attributes to instantiate. if null, all attributes will be instantiated.
     * @return new bean with given id.
     */
    public <T> T instantiateLazyRelationship(Class<T> clazz, Object beanId, String[] attributes);

    /**
     * calls any method on the relation to invoke an instantiation of the given relation. this can't be done on a bean,
     * that was already serialized, so we have to get a 'fresh' source bean to call any method on the given attribute.
     * <p>
     * WARNING: works recursive! max recurs level is 10. if fillTypes is null, only the attributes of the current bean
     * will be instantiated. if fillTypes is not null, all other relations will be evaluated through all its attributes!
     * All beans will only be instantiated once!
     * 
     * @param <T> type of bean
     * @param clazz type of source bean
     * @param beanId id of source bean
     * @param fillTypes optional relation types to instantiate. if null, all attributes will be instantiated.
     * @return new bean with given id.
     */
    public <T> T instantiateLazyRelationship(Class<T> clazz, Object beanId, List<Class> fillTypes);

    /**
     * fills recursive all attribute-relations of type fillType.
     * 
     * @param clazz type of bean to get through beanId
     * @param beanId bean id to load
     * @param attributes (optional) relations. if null, all attributes will be instantiated.
     * @param fillTypes (optional) relation types to fill. if null, all types will be filled.
     */
    public <T> T instantiateLazyRelationship(T bean, String[] attributes, List<Class> fillTypes);

    /**
     * tries to find the right user entity through the user principal
     * 
     * @param <T> user entitiy
     * @param subject current subject
     * @param userEntity user entity type
     * @param userIdAttribute user name attribute
     * @return bean representing current subject
     */
    public <T> T getUser(Subject subject, Class<T> userEntity, String userIdAttribute);

    /**
     * only for tests - creates an empty server side factory.
     */
    void initServerSideFactories();

    /**
     * used to work with named queries - on entities without table or view binding.
     * 
     * @param <T> virtual entity type
     * @param beanType entity without table or view binding
     * @param namedQuery named query (see annotation on given entity)
     * @param args query arguments
     * @return list of entities
     */
    <T> Collection<T> findByNamedQuery(Class<T> beanType, String namedQuery, Object... args);

}
