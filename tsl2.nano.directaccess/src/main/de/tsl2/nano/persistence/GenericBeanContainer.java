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
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.service.util.IGenericService;
import de.tsl2.nano.serviceaccess.IAuthorization;

/**
 * see {@link GenericLocalServiceBean}.
 * 
 *             initializes the {@link BeanContainer} singelton, to use a hibernate session.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
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
        Environment.addService(IGenericService.class, container.getGenService());

        IAction<Collection<?>> typeFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                Class entityType = (Class) parameter[0];
                int startIndex = (Integer) parameter[1];
                int maxResult = (Integer) parameter[2];
                if (!BeanClass.getBeanClass(entityType).isAnnotationPresent(Entity.class)) {
                    return null;
                }
                return container.getGenService().findAll(entityType, startIndex, maxResult);
            }
        };
        IAction<Collection<?>> exampleFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                return container.getGenService().findByExample(parameter[0], true);
            }
        };
        IAction<Collection<?>> betweenFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                return container.getGenService().findBetween(parameter[0], parameter[1], true, (Integer) parameter[2], (Integer) parameter[3]);
            }
        };
        IAction<Collection<?>> queryFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                return container.getGenService().findByQuery((String) parameter[0],
                    (Boolean) parameter[1],
                    (Object[]) parameter[2],
                    (Class[]) parameter[3]);
            }
        };
        IAction<Collection<?>> queryMapFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                return container.getGenService().findByQuery((String) parameter[0],
                    (Boolean) parameter[1],
                    (Map<String, Object>) parameter[2],
                    (Class[]) parameter[3]);
            }
        };
        IAction lazyrelationResolver = new CommonAction() {
            @Override
            public Object action() {
                //use the weak implementation of BeanClass to avoid classloader problems!
                if (BeanClass.getBeanClass(parameter[0].getClass()).isAnnotationPresent(Entity.class)) {
                    return container.getGenService().instantiateLazyRelationship(parameter[0]);
                } else {
                    return parameter[0];
                }
            }
        };
        IAction saveAction = new CommonAction() {
            @Override
            public Object action() {
                return container.getGenService().persist(parameter[0]);
            }
        };
        IAction deleteAction = new CommonAction() {
            @Override
            public Object action() {
                container.getGenService().remove(parameter[0]);
                return null;
            }
        };
        IAction attrAction = new CommonAction() {
            @Override
            public Object action() {
                return getAttributeDefinitions(parameter[0], (String) parameter[1]);
            }
        };
        IAction permissionAction = new CommonAction() {
            @Override
            public Object action() {
                return hasPermission((String) parameter[0], (String)(parameter.length > 1 ? parameter[1] : null));
            }
        };
        IAction persistableAction = new CommonAction() {
            @Override
            public Object action() {
                return isPersistable((Class<?>) parameter[0]);
            }
        };
        final IAction<Integer> executeAction = new CommonAction<Integer>() {
            @Override
            public Integer action() {
                return container.getGenService().executeQuery((String) parameter[0],
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
            queryMapFinder,
            attrAction,
            permissionAction,
            persistableAction,
            executeAction);
    }

    protected static Object hasPermission(String name, String action) {
        return Environment.get(IAuthorization.class).hasAccess(name, action);
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
