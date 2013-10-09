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

import javax.ejb.Remote;
import javax.security.auth.Subject;

/**
 * provides some basic service access methods to work with entity beans.
 * 
 * @author TS
 * 
 */
@Remote
@SuppressWarnings("rawtypes")
public interface IGenericService extends IGenericBaseService, IQueryService , IBatchService {
    /**
     * calls any method on the relation to invoke an instantiation of the given relation. this can't be done on a bean,
     * that was already serialized, so we have to get a 'fresh' source bean to call any method on the given attribute.
     * <p>
     * WARNING: only useful on calling remote interfaces, where serialization will be done! if you call a service on the
     * same jvm, you should use the IGenericLocalService - and the {@link #instantiateLazyRelationship(Object)} is
     * obsolete.
     * 
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
     * <p>
     * WARNING: only useful on calling remote interfaces, where serialization will be done! if you call a service on the
     * same jvm, you should use the IGenericLocalService - and the {@link #instantiateLazyRelationship(Object)} is
     * obsolete.
     * 
     * 
     * @param <T> type of bean
     * @param clazz type of source bean
     * @param beanId id of source bean
     * @param fillTypes optional relation types to instantiate. if null, all attributes will be instantiated.
     * @return new bean with given id.
     */
    public <T> T instantiateLazyRelationship(Class<T> clazz, Object beanId, List<Class> fillTypes);

    /**
     * convenience method for {@linkplain #instantiateLazyRelationship(Class, Object, String[])}. using reflection on
     * id-annotation to get the id. if not implemented, it will throw a notimplemented exception - then use
     * {@linkplain #instantiateLazyRelationship(Class, Object, String[])} instead.
     * <p>
     * WARNING: only useful on calling remote interfaces, where serialization will be done! if you call a service on the
     * same jvm, you should use the IGenericLocalService - and the {@link #instantiateLazyRelationship(Object)} is
     * obsolete.
     * 
     * @param <T> type of bean
     * @param bean bean instance with unloaded lazy relations.
     * @return bean instance with loaded relations.
     */
    public <T> T instantiateLazyRelationship(T bean);

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
}
