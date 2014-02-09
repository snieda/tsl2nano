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

import java.lang.annotation.Annotation;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.logging.Log;

import de.tsl2.nano.Environment;
import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanAttribute;
import de.tsl2.nano.bean.BeanClass;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.IAttributeDef;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.util.Util;

/**
 * JPA 2.0 BeanContainer, using the stateless session bean {@link IGenericService}.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
public class BeanContainerUtil {
    private static final Map<String, IAttributeDef> attrDefCache = new HashMap<String, IAttributeDef>();

    private static final Log LOG = LogFactory.getLog(BeanContainerUtil.class);

    /**
     * initializes the standard bean container to use GenericService methods. it creates an own servicefactory using the
     * given classloader
     * 
     * @param classloader loader to be used inside the own servicefactory instance.
     */
    public static void initGenericServices(ClassLoader classloader) {
        if (!ServiceFactory.isInitialized()) {
            ServiceFactory.createInstance(classloader);
        }
        final IGenericService service = ServiceFactory.instance().getService(IGenericService.class);
        Environment.addService(IGenericService.class, service);

        final IAction<Collection<?>> typeFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                final Class entityType = (Class) parameter[0];
                final int startIndex = (Integer) parameter[1];
                final int maxResult = (Integer) parameter[2];
                if (!BeanClass.getBeanClass(entityType).isAnnotationPresent(Entity.class)) {
                    return null;
                }
                return service.findAll(entityType, startIndex, maxResult);
            }
        };
        final IAction<Collection<?>> exampleFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                return service.findByExample(parameter[0], true);
            }
        };
        final IAction<Collection<?>> betweenFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                return service.findBetween(parameter[0], parameter[1], true, (Integer) parameter[2],
                    (Integer) parameter[3]);
            }
        };
        final IAction<Collection<?>> queryFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                return service.findByQuery((String) parameter[0],
                    (Boolean) parameter[1],
                    (Object[]) parameter[2],
                    (Class[]) parameter[3]);
            }
        };
        final IAction lazyrelationResolver = new CommonAction() {
            @Override
            public Object action() {
                //use the weak implementation of BeanClass to avoid classloader problems!
                if (BeanClass.getBeanClass(parameter[0].getClass()).isAnnotationPresent(Entity.class)) {
                    return service.instantiateLazyRelationship(parameter[0]);
                } else {
                    return parameter[0];
                }
            }
        };
        final IAction saveAction = new CommonAction() {
            @Override
            public Object action() {
                return service.persist(parameter[0]);
            }
        };
        final IAction deleteAction = new CommonAction() {
            @Override
            public Object action() {
                service.remove(parameter[0]);
                return null;
            }
        };
        final IAction attrAction = new CommonAction() {
            @Override
            public Object action() {
                return getAttributeDefinitions(parameter[0], (String) parameter[1]);
            }
        };
        final IAction permissionAction = new CommonAction() {
            @Override
            public Object action() {
                return ServiceFactory.instance().hasRole((String) parameter[0]);
            }
        };
        final IAction persistableAction = new CommonAction() {
            @Override
            public Object action() {
                return BeanContainerUtil.isPersistable((Class<?>) parameter[0]);
            }
        };
        final IAction<Integer> executeAction = new CommonAction<Integer>() {
            @Override
            public Integer action() {
                return service.executeQuery((String) parameter[0],
                    (Boolean) parameter[1],
                    (Object[]) parameter[2]);
            }
        };
        BeanContainer.initServiceActions(typeFinder,
            lazyrelationResolver,
            saveAction,
            deleteAction,
            exampleFinder,
            betweenFinder,
            queryFinder,
            attrAction,
            permissionAction,
            persistableAction,
            executeAction);
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
            final Class clazz = (Class) ((beanOrType instanceof Class) ? beanOrType : beanOrType.getClass());

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
            final/*Id*/Annotation id = battr.getAnnotation(Id.class);
            final/*Temporal*/Annotation temporal = battr.getAnnotation(Temporal.class);
            if (column == null) {
                if (joinColumn != null) {
                    def = new IAttributeDef() {
                        BeanClass joinColumnBC = BeanClass.getBeanClass(joinColumn.getClass());
                        Boolean nullable;
                        Boolean composition;
                        Boolean cascading;
                        Class<? extends Date> temporalType;

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
                            //return manyToOne.optional();
                            if (nullable == null)
                                nullable = (Boolean) joinColumnBC.callMethod(joinColumn, "nullable");
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
                                composition = !nullable() && oneToMany != null;
                            }
                            return composition;
                        }

                        @Override
                        public boolean cascading() {
                            if (cascading == null) {
                                // check for @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true)
                                if (composition()) {
                                    CascadeType[] ctype = (CascadeType[]) BeanClass.call(oneToMany, "cascade");
                                    cascading =
                                        Util.contains(ctype, CascadeType.ALL, CascadeType.MERGE)
                                            && (Boolean) BeanClass.call(oneToMany, "orphanRemoval");
                                } else {
                                    cascading = false;
                                }
                            }
                            return composition;
                        }
                    };
                } else {
                    def = new IAttributeDef() {
                        Class<? extends Date> temporalType;

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
                            return false;
                        }
                        @Override
                        public boolean cascading() {
                            return false;
                        }
                    };
                }
                attrDefCache.put(attrKey(clazz, attribute), def);
                return def;
            }

            def = new IAttributeDef() {
                BeanClass columnBC = BeanClass.getBeanClass(column.getClass());
                Integer scale;
                Integer precision;
                Integer length;
                Boolean nullable;
                Boolean unique;
                Class<? extends Date> temporalType;

                @Override
                public int scale() {
//                    return column.scale();
                    if (scale == null)
                        scale = (Integer) columnBC.callMethod(column, "scale");
                    return scale;
                }

                @Override
                public int precision() {
//                    return column.precision();
                    if (precision == null)
                        precision = (Integer) columnBC.callMethod(column, "precision");
                    return precision;
                }

                @Override
                public boolean nullable() {
//                    return column.nullable();
                    if (nullable == null)
                        nullable = (Boolean) columnBC.callMethod(column, "nullable");
                    return nullable;
                }

                @Override
                public int length() {
//                    return column.length();
                    if (length == null)
                        length = (Integer) columnBC.callMethod(column, "length");
                    return length;
                }

                @Override
                public boolean id() {
                    return id != null;
                }

                @Override
                public boolean unique() {
                    if (unique == null)
                        unique = (Boolean) columnBC.callMethod(column, "unique");
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
            };
            attrDefCache.put(attrKey(clazz, attribute), def);
            return def;
        } catch (final Exception e) {
            ForwardedException.forward(e);
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
        return (Class<? extends Date>) (t.equals(TemporalType.DATE) ? Date.class
            : t.equals(TemporalType.TIME) ? Time.class : Timestamp.class);
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
    }
}
