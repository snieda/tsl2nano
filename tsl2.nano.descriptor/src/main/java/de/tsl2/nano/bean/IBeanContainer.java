/*
 * Copyright © 2002-2009 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.bean;

import java.util.Collection;
import java.util.Map;

/**
 * To be implemented by a bean container.
 * 
 * @author ts 01.12.2008
 * @version $Revision$
 */
public interface IBeanContainer {
    /**
     * returns bean instance of given id
     * 
     * @param <T> bean type
     * @param type class of bean type
     * @param id bean id
     * @return bean or null
     */
    <T> T getByID(Class<T> type, Object id);
    
    /**
     * returns all bean instances of given type
     * 
     * @param <T> bean type
     * @param type class of bean type
     * @param startIndex (optional: set 0 as standard) used with maxresult to fetch data blocks
     * @param maxResult (optional: set -1 to use no definition) maximum result count
     * @return all beans of type <T> in container
     */
    <T> Collection<T> getBeans(Class<T> type, int startIndex, int maxResult);

    <T> Collection<T> getBeans(BeanFindParameters<T> parameters);

    /**
     * returns all beans that match the attributes of exampleBean
     * 
     * @param <T> bean type
     * @param exampleBean example bean with optional filled attributes
     * @return matching beans
     */
    <T> Collection<T> getBeansByExample(T exampleBean);

    /**
     * returns all beans that match the attributes of exampleBean
     * 
     * @param <T> bean type
     * @param exampleBean example bean with optional filled attributes
     * @param startIndex (optional: set 0 as standard) used with maxresult to fetch data blocks
     * @param maxResult (optional: set -1 to use no definition) maximum result count
     * @return matching beans
     */
    <T> Collection<T> getBeansByExample(T exampleBean, Boolean useLike, int startIndex, int maxResult );

    <T> Collection<T> getBeansByExample(T exampleBean, Boolean useLike, BeanFindParameters<T> parameters);

    /**
     * returns all beans that match the range of firstBean and secondBean. if firstBean equals secondBean, it is a
     * simple findByExample.
     * 
     * @param <T> bean type
     * @param firstBean minimal bean
     * @param secondBean maximum bean
     * @param startIndex (optional: set 0 as standard) used with maxresult to fetch data blocks
     * @param maxResult (optional: set -1 to use no definition) maximum result count
     * @return matching beans
     */
    <T> Collection<T> getBeansBetween(T firstBean, T secondBean, int startIndex, int maxResult);

    <T> Collection<T> getBeansBetween(T firstBean, T secondBean, BeanFindParameters<T> parameters);
    
    /**
     * getBeansByQuery
     * 
     * @param <T> bean type
     * @param query sql or ejb-ql
     * @param nativeQuery if false, query must be pure sql, if true, query must be ejb-ql
     * @param args query arguments
     * @param lazyRelations optional types to preload
     * @return query result
     */
    <T> Collection<T> getBeansByQuery(String query, Boolean nativeQuery, Object[] args, Class... lazyRelations);
    
    /**
     * getBeansByQuery
     * 
     * @param <T> bean type
     * @param query sql or ejb-ql
     * @param nativeQuery if false, query must be pure sql, if true, query must be ejb-ql
     * @param par query arguments
     * @param lazyRelations optional types to preload
     * @return query result
     */
    <T> Collection<T> getBeansByQuery(String query, Boolean nativeQuery, Map<String, Object> par, Class... lazyRelations);
    /**
     * execute a statement changing structure or data (like insert, update, delete, create)
     * 
     * @param <T> bean type
     * @param query sql or ejb-ql
     * @param nativeQuery if false, query must be pure sql, if true, query must be ejb-ql
     * @param args query arguments
     * @return query result
     */
    Integer executeStmt(String query, Boolean nativeQuery, Object[] args);

    /**
     * createBean
     * 
     * @return a new bean instance
     */
    <T> T createBean(Class<T> type);

    /**
     * saves the current bean and returns the new instance.
     * 
     * @return the new saved instance
     */
    <T> T save(T bean);

    /**
     * delete the current bean
     */
    <T> void delete(T bean);

    /**
     * returns a new instance of given bean, but with filled relations
     * 
     * @param bean bean instance with lazy relations
     * @return same bean but new instance with fetched relations.
     */
    <T> T resolveLazyRelations(T bean);

    /**
     * getAttributeDef
     * 
     * @param bean bean
     * @param attributeName name
     * @return attribute definitions
     */
    IAttributeDef getAttributeDef(Object bean, String attributeName);

    /**
     * hasPermission
     * 
     * @param roleName user role to check
     * @param action action on role to be checked
     * @return true, if user has role
     */
    Boolean hasPermission(String roleName, String action);

    /**
     * isEntity
     * 
     * @param beanClass bean class
     * @return true, if class is entity
     */
    boolean isPersistable(Class<?> beanClass);
}
