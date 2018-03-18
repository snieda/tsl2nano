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

import java.util.List;

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
@SuppressWarnings("rawtypes")
interface IGenericLocalService extends IGenericBaseService, IQueryService, IBatchService {
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
    <T> T instantiateLazyRelationship(Class<T> clazz, Object beanId, String[] attributes);

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
    <T> T instantiateLazyRelationship(Class<T> clazz, Object beanId, List<Class> fillTypes);

    /**
     * fills recursive all attribute-relations of type fillType.
     * 
     * @param clazz type of bean to get through beanId
     * @param beanId bean id to load
     * @param attributes (optional) relations. if null, all attributes will be instantiated.
     * @param fillTypes (optional) relation types to fill. if null, all types will be filled.
     */
    <T> T instantiateLazyRelationship(T bean, String[] attributes, List<Class> fillTypes);

    /**
     * tries to find the right user entity through the user principal
     * 
     * @param <T> user entitiy
     * @param subject current subject
     * @param userEntity user entity type
     * @param userIdAttribute user name attribute
     * @return bean representing current subject
     */
    <T> T getUser(Subject subject, Class<T> userEntity, String userIdAttribute);

    /**
     * only for tests - creates an empty server side factory.
     */
    @Override
    void initServerSideFactories();

}
