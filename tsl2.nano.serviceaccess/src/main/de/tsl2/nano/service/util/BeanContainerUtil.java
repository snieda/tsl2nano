/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Jun 7, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.service.util;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanFindParameters;
import de.tsl2.nano.bean.BeanProxy;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IAttributeDef;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.serviceaccess.ServiceLocator;

/**
 * JPA 2.0 BeanContainer, using the stateless session bean {@link IGenericService}.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
//TODO: refactore BeanContainerUtil to BeanContainerInitializer while its not a util anymore...
@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
public class BeanContainerUtil {
    private static final Map<String, IAttributeDef> attrDefCache = new HashMap<String, IAttributeDef>();

    private static final Log LOG = LogFactory.getLog(BeanContainerUtil.class);

    public static ClassLoader initProxyServiceFactory() {
        System.setProperty(ServiceLocator.NO_JNDI, "true");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (!ServiceFactory.isInitialized()) {
            ServiceFactory.createInstance(cl);
        }
        IGenericService service = BeanProxy.createBeanImplementation(IGenericService.class);
        BeanProxy.setReturnEmptyCollections(service, true);
        ServiceFactory.instance().setInitialServices(
            MapUtil.asMap(IGenericService.class.getName(), service));
        ENV.addService(IGenericService.class, service);
        return cl;
    }

    /**
     * provides a beancontainer using empty proxy genericservices - without initialContext - for test purposes!
     */
    public static void initEmptyProxyServices() {
        initGenericServices(initProxyServiceFactory());    
    }

    /**
     * initializes the standard bean container to use GenericService methods. it creates an own servicefactory using the
     * given classloader
     * 
     * @param classloader loader to be used inside the own servicefactory instance.
     */
    public static void initGenericServices(ClassLoader classloader) {
        initGenericServices(new BeanContainerUtil(), classloader);
    }
    // TODO: refactore the BeanContainerUtil.init...() calls not to be static!
    public static <I extends BeanContainerUtil> void initGenericServices(final I _impl, ClassLoader classloader) {
        if (!ServiceFactory.isInitialized()) {
            ServiceFactory.createInstance(classloader);
        }
        final IGenericService service = ServiceFactory.instance().getService(IGenericService.class);
        ENV.addService(IGenericService.class, service);
        initGenericServices(_impl, () -> service);
    }

    public static <I extends BeanContainerUtil> void initGenericServices(I _impl, Supplier<IGenericService> service) {
        final IAction idFinder = new CommonAction() {
            @Override
            public Object action() {
                final Class entityType = (Class) parameters().getValue(0);
                final Object id = parameters().getValue(1);
                if (!BeanClass.getBeanClass(entityType).isAnnotationPresent(Entity.class)) {
                    return null;
                }
                return service.get().findById(entityType, id);
            }
        };
        final IAction<Collection<?>> typeFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                if (parameters().getValue(0) instanceof BeanFindParameters) {
                    return service.get().findAll((BeanFindParameters)parameters().getValue(0));
                } else {
                    final Class entityType = (Class) parameters().getValue(0);
                    final int startIndex = (Integer) parameters().getValue(1);
                    final int maxResult = (Integer) parameters().getValue(2);
                    if (!BeanClass.getBeanClass(entityType).isAnnotationPresent(Entity.class)) {
                        return null;
                    }
                    return service.get().findAll(entityType, startIndex, maxResult);
                }
            }
        };
        final IAction<Collection<?>> exampleFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                boolean useLike = parameters().getValue(1) instanceof Boolean && ((Boolean) parameters().getValue(1));
                boolean useFindParameters = parameters().getValue(2) instanceof BeanFindParameters;
                if (useLike) {
                    if (useFindParameters) {
                        return service.get().findByExampleLike(parameters().getValue(0), true, (BeanFindParameters) parameters().getValue(2));
                    } else {
                        return service.get().findByExampleLike(parameters().getValue(0), true, (Integer) parameters().getValue(2),
                            (Integer) parameters().getValue(3));
                    }
                } else {
                        return service.get().findByExample(parameters().getValue(0), true);
                }
            }
        };
        final IAction<Collection<?>> betweenFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                if (parameters().getValue(2) instanceof BeanFindParameters) {
                    return service.get().findBetween(parameters().getValue(0), parameters().getValue(1), true, (BeanFindParameters)parameters().getValue(2));
                } else {
                    return service.get().findBetween(parameters().getValue(0), parameters().getValue(1), true, (Integer) parameters().getValue(2),
                        (Integer) parameters().getValue(3));
                }
            }
        };
        final IAction<Collection<?>> queryFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                return service.get().findByQuery((String) parameters().getValue(0),
                    (Boolean) parameters().getValue(1),
                    (Object[]) parameters().getValue(2),
                    (Class[]) parameters().getValue(3));
            }
        };
        final IAction<Collection<?>> queryMapFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                return service.get().findByQuery((String) parameters().getValue(0),
                    (Boolean) parameters().getValue(1),
                    (Map<String, Object>) parameters().getValue(2),
                    (Class[]) parameters().getValue(3));
            }
        };
        final IAction lazyrelationResolver = new CommonAction() {
            @Override
            public Object action() {
                //use the weak implementation of BeanClass to avoid classloader problems!
                if (BeanClass.getBeanClass(parameters().getValue(0).getClass()).isAnnotationPresent(Entity.class)) {
                    return service.get().instantiateLazyRelationship(parameters().getValue(0));
                } else {
                    return parameters().getValue(0);
                }
            }
        };
        final IAction saveAction = new CommonAction() {
            @Override
            public Object action() {
                return service.get().persist(parameters().getValue(0));
            }
        };
        final IAction deleteAction = new CommonAction() {
            @Override
            public Object action() {
                service.get().remove(parameters().getValue(0));
                return null;
            }
        };
        final IAction attrAction = new CommonAction() {
            @Override
            public Object action() {
                return getAttributeDefinitions(parameters().getValue(0), (String) parameters().getValue(1));
            }
        };
        final IAction permissionAction = new CommonAction() {
            @Override
            public Object action() {
                return _impl.hasPermission((String) parameters().getValue(0), (String) (parameters().size() > 1 ? parameters().getValue(1) : null));
            }
        };
        final IAction persistableAction = new CommonAction() {
            @Override
            public Object action() {
                return isPersistable((Class<?>) parameters().getValue(0));
            }
        };
        final IAction<Integer> executeAction = new CommonAction<Integer>() {
            @Override
            public Integer action() {
                return service.get().executeQuery((String) parameters().getValue(0),
                    (Boolean) parameters().getValue(1),
                    (Object[]) parameters().getValue(2));
            }
        };
        BeanContainer.initServiceActions(idFinder,
            typeFinder,
            lazyrelationResolver,
            saveAction,
            deleteAction,
            exampleFinder,
            betweenFinder,
            queryFinder,
            queryMapFinder,
            attrAction,
            permissionAction,
            persistableAction,
            executeAction);
    }

    protected Object hasPermission(String name, String action) {
        return ServiceFactory.instance().hasRole(name);
    }

    /**
     * isEntity
     * 
     * @param beanClass bean class
     * @return true, if class is entity
     */
    public static boolean isPersistable(Class<?> beanClass) {
        return BeanClass.getBeanClass(BeanClass.getDefiningClass(beanClass)).isAnnotationPresent(Entity.class);
    }

    /**
     * checks, if given attribute is annotated with {@link GeneratedValue}.
     * 
     * @param beanClass
     * @param attribute
     * @return true, if attribute (field or method) is annotated with {@link GeneratedValue}.
     */
    public static boolean isGeneratedValue(Class<?> beanClass, String attribute) {
        return BeanAttribute.getBeanAttribute(BeanClass.getDefiningClass(beanClass), attribute).getAnnotation(
            GeneratedValue.class) != null;
    }

    /**
     * delegates to {@link #getAttributeDefinitions(Object, String)}
     * 
     * @param attribute filled bean attribute
     * @return filled attribute definitions
     */
    public static IAttributeDef getAttributeDefinitions(BeanAttribute attribute) {
        return getAttributeDefinitions(attribute.getDeclaringClass(), attribute.getName());
    }

    /**
     * evaluates attributes annotations like ManyToOne, Column and Id and packs the informations to
     * {@link IAttributeDef}.
     * <p>
     * because we are on client or server side - we may not have the right classloader to cast the annotations, so we do
     * a soft implementation - means: not the class instances will be asked but their names.
     * <p>
     * TODO: use metadatainf of ejb 3.1
     * 
     * @param beanOrType bean instance or bean type
     * @param attribute attribute name
     * @return filled {@link IAttributeDef}
     */
    public static IAttributeDef getAttributeDefinitions(final Object beanOrType, final String attribute) {
        try {
            //soft implementations to evaluate annotations will be used 
            //to avoid classloader problems (ClassCastExceptions on same Class!)
            //normally you should eval the annotations on server side, but we shift it in cause of performance
            final Class clazz = BeanClass.getDefiningClass((Class) ((beanOrType instanceof Class) ? beanOrType : beanOrType.getClass()));
            final String key = attrKey(clazz, attribute);
            IAttributeDef def;
            //the stored key may have a value == null
            if (attrDefCache.containsKey(key)) {
                def = attrDefCache.get(key);
                return def;
            }
            LOG.debug("evaluating attribute annotations 'getAttributeDefinitions(" + beanOrType
                + ", "
                + attribute
                + ")'");
            final BeanAttribute battr = BeanAttribute.getBeanAttribute(clazz, attribute);
            final/*Column*/Annotation column = battr
                .getAnnotation(Column.class);
            final/*JoinColumn*/Annotation joinColumn = battr
                .getAnnotation(JoinColumn.class);
            final/*OneToMany*/Annotation oneToMany = battr
                .getAnnotation(OneToMany.class);
            /*Id*/Annotation id0 = battr.getAnnotation(Id.class);
            /*Id*/Annotation id1 = battr.getAnnotation(EmbeddedId.class);
            final/*Id*/Annotation id = id0 != null ? id0 : id1 != null ? id1 : battr.getAnnotation(MapsId.class);
            final/*Temporal*/Annotation temporal = battr.getAnnotation(Temporal.class);
            if (column == null) {
                if (joinColumn != null) {
                    def = new IAttributeDef() {
                        BeanClass joinColumnBC = BeanClass.getBeanClass(joinColumn.getClass());
                        Boolean nullable;
                        Class<? extends Date> temporalType;
                        Boolean composition;
                        Boolean cascading;
                        Boolean generatedValue;
                        Boolean isTransient;

                        @Override
                        public int scale() {
                            return -1;
                        }

                        @Override
                        public int precision() {
                            return -1;
                        }

                        @Override
                        public boolean nullable() {
                            if (nullable == null) {
                                nullable = oneToMany != null || (Boolean) joinColumnBC.callMethod(joinColumn, "nullable");
                            }
                            return nullable;
                        }

                        @Override
                        public int length() {
                            return -1;
                        }

                        @Override
                        public boolean id() {
                            return id != null;
                        }

                        @Override
                        public boolean unique() {
                            return false;
                        }

                        @Override
                        public Class<? extends Date> temporalType() {
                            if (temporalType == null && temporal != null) {
                                temporalType = getTemporalType(temporal);
                            }
                            return temporalType;
                        }

                        @Override
                        public boolean composition() {
                            if (composition == null) {
                                //nullable() would only return true, if oneToMany is null!
                                composition = !(Boolean) joinColumnBC.callMethod(joinColumn, "nullable") && oneToMany != null;
                            }
                            return composition;
                        }

                        @Override
                        public boolean cascading() {
                            if (cascading == null) {
                                // check for @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true)
                                if (oneToMany != null) {
                                    CascadeType[] ctype = (CascadeType[]) BeanClass.call(oneToMany, "cascade");
                                    cascading =
                                        Util.contains(ctype, CascadeType.ALL, CascadeType.MERGE)
                                            && (Boolean) BeanClass.call(oneToMany, "orphanRemoval");
                                } else {
                                    cascading = false;
                                }
                            }
                            return cascading;
                        }

                        @Override
                        public boolean generatedValue() {
                            if (generatedValue == null) {
                                generatedValue = isGeneratedValue(clazz, attribute);
                            }
                            return generatedValue;
                        }

                        @Override
                        public boolean isTransient() {
                            if (isTransient == null) {
                                isTransient = battr.getAnnotation(Transient.class) != null;
                            }
                            return isTransient;
                        }
                    };
                } else {//column == null && joincolumn == null, if oneToMany == null, it may be not persistable!
                    def = new IAttributeDef() {
                        Class<? extends Date> temporalType;
                        Boolean composition;
                        Boolean cascading;
                        Boolean generatedValue;
                        Boolean isTransient;

                        @Override
                        public int scale() {
                            return -1;
                        }

                        @Override
                        public int precision() {
                            return -1;
                        }

                        @Override
                        public boolean nullable() {
                            return true;
                        }

                        @Override
                        public int length() {
                            return -1;
                        }

                        @Override
                        public boolean id() {
                            return id != null;
                        }

                        @Override
                        public boolean unique() {
                            return false;
                        }

                        @Override
                        public Class<? extends Date> temporalType() {
                            if (temporalType == null && temporal != null) {
                                temporalType = getTemporalType(temporal);
                            }
                            return temporalType;
                        }

                        @Override
                        public boolean composition() {
                            if (composition == null) {
                                composition = !nullable() && oneToMany != null;
                            }
                            return composition;
                        }

                        @Override
                        public boolean cascading() {
                            if (cascading == null) {
                                // check for @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true)
                                if (oneToMany != null) {
                                    CascadeType[] ctype = (CascadeType[]) BeanClass.call(oneToMany, "cascade");
                                    cascading =
                                        Util.contains(ctype, CascadeType.ALL, CascadeType.MERGE)
                                            && (Boolean) BeanClass.call(oneToMany, "orphanRemoval");
                                } else {//perhaps non-persistable attribute
                                    cascading = !BeanUtil.isSingleValueType(battr.getType());
                                }
                            }
                            return cascading;
                        }

                        @Override
                        public boolean generatedValue() {
                            if (generatedValue == null) {
                                generatedValue = isGeneratedValue(clazz, attribute);
                            }
                            return generatedValue;
                        }

                        @Override
                        public boolean isTransient() {
                            if (isTransient == null) {
                                isTransient = battr.getAnnotation(Transient.class) != null;
                            }
                            return isTransient;
                        }
                    };
                }
            } else {//column != null
                def = new IAttributeDef() {
                    BeanClass columnBC = BeanClass.getBeanClass(column.getClass());
                    Integer scale;
                    Integer precision;
                    Integer length;
                    Boolean nullable;
                    Boolean unique;
                    Class<? extends Date> temporalType;
                    Boolean generatedValue;
                    Boolean isTransient;

                    @Override
                    public int scale() {
//                    return column.scale();
                        if (scale == null) {
                            scale = (Integer) columnBC.callMethod(column, "scale");
                        }
                        return scale;
                    }

                    @Override
                    public int precision() {
//                    return column.precision();
                        if (precision == null) {
                            precision = (Integer) columnBC.callMethod(column, "precision");
                        }
                        return precision;
                    }

                    @Override
                    public boolean nullable() {
//                    return column.nullable();
                        if (nullable == null) {
                            nullable = oneToMany != null || (Boolean) columnBC.callMethod(column, "nullable");
                        }
                        return nullable;
                    }

                    @Override
                    public int length() {
//                    return column.length();
                        if (length == null) {
                            length = (Integer) columnBC.callMethod(column, "length");
                        }
                        return length;
                    }

                    @Override
                    public boolean id() {
                        return id != null;
                    }

                    @Override
                    public boolean unique() {
                        if (unique == null) {
                            unique = (Boolean) columnBC.callMethod(column, "unique");
                        }
                        return unique;
                    }

                    @Override
                    public java.lang.Class<? extends java.util.Date> temporalType() {
                        if (temporalType == null && temporal != null) {
                            temporalType = getTemporalType(temporal);
                        }
                        return temporalType;
                    }

                    @Override
                    public boolean composition() {
                        return false;
                    }

                    @Override
                    public boolean cascading() {
                        return false;
                    }

                    @Override
                    public boolean generatedValue() {
                        if (generatedValue == null) {
                            generatedValue = isGeneratedValue(clazz, attribute);
                        }
                        return generatedValue;
                    }

                    @Override
                    public boolean isTransient() {
                        if (isTransient == null) {
                            isTransient = battr.getAnnotation(Transient.class) != null;
                        }
                        return isTransient;
                    }
                };
            }
            attrDefCache.put(attrKey(clazz, attribute), def);
            return def;
        } catch (final Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * getTemporalType
     * 
     * @param temporal
     */
    protected static Class<? extends Date> getTemporalType(final Annotation temporal) {
        BeanClass temporalBC = BeanClass.getBeanClass(temporal.getClass());
        TemporalType t = (TemporalType) temporalBC.callMethod(temporal, "value");
        return t.equals(TemporalType.DATE) ? Date.class
            : t.equals(TemporalType.TIME) ? Time.class : Timestamp.class;
    }

    private static final String attrKey(Class<?> clazz, String attribute) {
        return clazz.getName() + "." + attribute;
    }

    /**
     * clear cache
     */
    public static final void clear() {
        LOG.info("removing " + attrDefCache.size() + " cached IAttrDefinitions");
        attrDefCache.clear();
        ENV.removeService(IGenericService.class);
        resetServices();
    }

    /**
     * On a database with real IDs (not generated synthetic IDs), these IDs are composition of one or more fields. An
     * entity generation tool like hibernate creates embedded composite id objects that are not synchronized with
     * changes on the standard fields of this entity.
     * <p/>
     * Example:
     * 
     * <pre>
     * &#064;Entity
     * class Person {
     *     &#064;Id
     *     PersonID id;
     *     &#064;JoinColumn(COLUMNNAME)
     *     Organization org;
     * 
     *     &#064;Embedded
     *     class PersonID {
     *         String name;
     *         &#064;Column(COLUMNNAME)
     *         String orgid;
     *     }
     * }
     * 
     * If attribute 'org' gets another reference, this change should be done on 'PersonID.orgid', too.
     * </pre>
     * 
     * TODO: create a performance optimized solution
     * 
     * @param entity entity having a composite id to be synchronized with value changes on other attributes
     * @param attributeNames attributes to synchronize. if no attributeName is given, all attributeNames of the entity
     *            will be synchronized.
     */
    public static void synchronizeEmbeddedCompositeID(Serializable entity, String... attributeNames) {
        /*
         * get the entities id to check, whether synchronization has to be done.
         */
        Bean<Serializable> bean = Bean.getBean(entity);
        Serializable id = (Serializable) bean.getId();
        // a composite key is not a standard type like String or Number
        if (id == null || BeanUtil.isStandardType(id)) {
            return;
        }

        if (attributeNames.length == 0) {
            attributeNames = bean.getAttributeNames();
        }

        for (int i = 0; i < attributeNames.length; i++) {
            /*
             * get the JoinColumn name - the database foreign key name
             */
            BeanAttribute attribute = BeanAttribute.getBeanAttribute(bean.getDeclaringClass(), attributeNames[i]);
            JoinColumn jc = (JoinColumn) attribute.getAnnotation(JoinColumn.class);

            if (jc != null) {
                /*
                 * check the id object to have an attribute with a column annotation with same foreign key
                 */
                Bean idBean = Bean.getBean(id);
                String[] idAttrNames = idBean.getAttributeNames();
                BeanAttribute idAttr;
                Column c;
                Object valueObject, value;
                for (int j = 0; j < idAttrNames.length; j++) {
                    idAttr = BeanAttribute.getBeanAttribute(idBean.getDeclaringClass(), idAttrNames[j]);
                    c = (Column) idAttr.getAnnotation(Column.class);
                    if (c != null && c.name().equals(jc.name())) {
                        valueObject = attribute.getValue(entity);
                        if (valueObject != null) {
                            value = Bean.getBean((Serializable)valueObject).getId();
                        } else {
                            value = null;
                        }
                        //TODO: shell we do a check against the types?
                        idAttr.setValue(id, value);
                    }
                }
            }
        }
    }
    
    public static void resetServices() {
        if (ServiceFactory.isInitialized())
            ServiceFactory.instance().reset(true);
    }
    
}
