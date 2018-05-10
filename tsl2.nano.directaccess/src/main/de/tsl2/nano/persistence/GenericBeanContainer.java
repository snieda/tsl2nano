/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Mar 25, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanFindParameters;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.service.util.IGenericService;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;

/**
 * see {@link GenericLocalServiceBean}.
 * 
 * initializes the {@link BeanContainer} singelton, to use an JPA-EntityManager provided by persistence-implementation.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
public abstract class GenericBeanContainer extends BeanContainerUtil {
    protected Map properties = new HashMap();
    protected IGenericService service;

    /**
     * initializes the standard bean container to use GenericService methods. it creates an own servicefactory using the
     * given classloader
     * 
     * @param classloader loader to be used inside the own servicefactory instance.
     */
    public static void initContainer(final GenericBeanContainer container, ClassLoader classloader) {
        ENV.addService(IGenericService.class, container.getGenService());

        IAction idFinder = new CommonAction() {
            @Override
            public Object action() {
                Class entityType = (Class) parameters().getValue(0);
                Object id = parameters().getValue(1);
                if (!BeanClass.getBeanClass(entityType).isAnnotationPresent(Entity.class)) {
                    return null;
                }
                return container.getGenService().findById(entityType, id);
            }
        };
        IAction<Collection<?>> typeFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                if (parameters().getValue(0) instanceof BeanFindParameters) {
                    return container.getGenService().findAll((BeanFindParameters)parameters().getValue(0));
                } else {
                    Class entityType = (Class) parameters().getValue(0);
                    int startIndex = (Integer) parameters().getValue(1);
                    int maxResult = (Integer) parameters().getValue(2);
                    if (!BeanClass.getBeanClass(entityType).isAnnotationPresent(Entity.class)) {
                        return null;
                    }
                    return container.getGenService().findAll(entityType, startIndex, maxResult);
                }
            }
        };
        IAction<Collection<?>> exampleFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                boolean useLike = parameters().getValue(1) instanceof Boolean && ((Boolean) parameters().getValue(1));
                boolean useFindParameters = parameters().getValue(2) instanceof BeanFindParameters;
                if (useLike) {
                    if (useFindParameters) {
                        return container.getGenService().findByExampleLike(parameters().getValue(0), true, (BeanFindParameters) parameters().getValue(2));
                    } else {
                        return container.getGenService().findByExampleLike(parameters().getValue(0), true, (Integer) parameters().getValue(2),
                            (Integer) parameters().getValue(3));
                    }
                } else {
                    return container.getGenService().findByExample(parameters().getValue(0), true);
                }
            }
        };
        IAction<Collection<?>> betweenFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                if (parameters().getValue(0) instanceof BeanFindParameters) {
                    return container.getGenService().findBetween(parameters().getValue(0), parameters().getValue(1), true, (BeanFindParameters)parameters().getValue(0));
                } else {
                    return container.getGenService().findBetween(parameters().getValue(0), parameters().getValue(1), true, (Integer) parameters().getValue(2),
                        (Integer) parameters().getValue(3));
                }
            }
        };
        IAction<Collection<?>> queryFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                return container.getGenService().findByQuery((String) parameters().getValue(0),
                    (Boolean) parameters().getValue(1),
                    (Object[]) parameters().getValue(2),
                    (Class[]) parameters().getValue(3));
            }
        };
        IAction<Collection<?>> queryMapFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                return container.getGenService().findByQuery((String) parameters().getValue(0),
                    (Boolean) parameters().getValue(1),
                    (Map<String, Object>) parameters().getValue(2),
                    (Class[]) parameters().getValue(3));
            }
        };
        IAction lazyrelationResolver = new CommonAction() {
            @Override
            public Object action() {
                //use the weak implementation of BeanClass to avoid classloader problems!
                if (BeanClass.getBeanClass(parameters().getValue(0).getClass()).isAnnotationPresent(Entity.class)) {
                    return container.getGenService().instantiateLazyRelationship(parameters().getValue(0));
                } else {
                    return parameters().getValue(0);
                }
            }
        };
        IAction saveAction = new CommonAction() {
            @Override
            public Object action() {
                return container.getGenService().persist(parameters().getValue(0));
            }
        };
        IAction deleteAction = new CommonAction() {
            @Override
            public Object action() {
                container.getGenService().remove(parameters().getValue(0));
                return null;
            }
        };
        IAction attrAction = new CommonAction() {
            @Override
            public Object action() {
                return getAttributeDefinitions(parameters().getValue(0), (String) parameters().getValue(1));
            }
        };
        IAction permissionAction = new CommonAction() {
            @Override
            public Object action() {
                return hasPermission((String) parameters().getValue(0), (String) (parameters().size() > 1 ? parameters().getValue(1) : null));
            }
        };
        IAction persistableAction = new CommonAction() {
            @Override
            public Object action() {
                return isPersistable((Class<?>) parameters().getValue(0));
            }
        };
        final IAction<Integer> executeAction = new CommonAction<Integer>() {
            @Override
            public Integer action() {
                return container.getGenService().executeQuery((String) parameters().getValue(0),
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

    protected static Object hasPermission(String name, String action) {
        IAuthorization auth = ConcurrentUtil.getCurrent(Authorization.class);
        return auth != null && auth.hasAccess(name, action);
    }

    protected abstract IGenericService getGenService();

    public Object get(Object key) {
        return properties.get(key);
    }

    protected void put(Object key, Object value) {
        properties.put(key, value);
    }

    protected Object remove(Object key) {
        return properties.remove(key);
    }
}
