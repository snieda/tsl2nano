/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: TS, IDV-AG
 * created on: Jan 19, 2010
 * 
 * Copyright: (c) IDV-AG 2010, all rights reserved
 */
package de.tsl2.nano.service.util;


/**
 * toplink specific helper class to resolve lazy instantiated relations. this class is no direct content of any
 * tsl2nano library, because it is toplink specific!
 * 
 * toplink (oracle) specific and at the moment not needed. to re-animate this class, you have to add the library
 * 'toplink-essentials.jar' (in glassfish/lib) to your classpath.<br>
 * 
 * @author TS, IDV-AG
 * @version $Revision$
 * 
 */
public class PersistenceUtil {

    /**
     * checks, if the given relation is instantiated by toplink. if not, the given bean will be used to instantiate the
     * relation. you have to used the return value!
     * 
     * @param <T> bean type
     * @param bean bean instance, holding the given relation
     * @param beanId id of bean
     * @param relation relation of bean
     * @param relationAttributeNames relation attribute names of bean (to be used on server side instantiation)
     * @return new instantiated bean. use that instead of your current bean.
     */
//    public static <T> T instantiateRelation(T bean,
//            Object beanId,
//            Collection<?> relation,
//            String[] relationAttributeNames) {
//        if (!isIndirectionInstantiated(relation)) {
//            IGenericService service = ServiceFactory.instance().getService(IGenericService.class);
//            bean = (T) service.instantiateLazyRelationship(bean.getClass(), beanId, relationAttributeNames);
//        }
//        return bean;
//    }

    /**
     * toplink (oracle) specific and at the moment not needed. to reanimate this class, you have to add the library
     * 'toplink-essentials.jar' (in glassfish/lib) to your classpath.<br>
     * 
     * checks, if the given container (perhaps a lazy instantiated relation) is already instantiated.
     * 
     * @param indirectContainer bean relation
     * @return true, if indirectContainer is already instantiated.
     */
//    public static boolean isIndirectionInstantiated(Collection<?> indirectContainer) {
//        if (!(indirectContainer instanceof IndirectContainer)) {
//            return true;
//        }
//        IndirectContainer c = (IndirectContainer) indirectContainer;
//        return c.isInstantiated();
//    }
}
