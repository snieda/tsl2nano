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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.service.util.IGenericService;
import de.tsl2.nano.util.bean.BeanClass;
import de.tsl2.nano.util.bean.BeanContainer;

/**
 * see {@link GenericLocalServiceBean}.
 * 
 *             initializes the {@link BeanContainer} singelton, to use a hibernate session.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public class HibernateBeanContainer extends BeanContainerUtil {
    protected Map properties = new HashMap();
    protected IGenericService service;

    private static final Log LOG = LogFactory.getLog(HibernateBeanContainer.class);

    /**
     * initializes the standard bean container to use GenericService methods. it creates an own servicefactory using the
     * given classloader
     * 
     * @param classloader loader to be used inside the own servicefactory instance.
     */
    public static void initHibernateContainer(ClassLoader classloader) {
        final HibernateBeanContainer hibContainer = new HibernateBeanContainer();

        IAction<Collection<?>> typeFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                Class entityType = (Class) parameter[0];
                int maxResult = (Integer) parameter[1];
                if (!new BeanClass(entityType).isAnnotationPresent(Entity.class)) {
                    return null;
                }
                return hibContainer.getService().findAll(entityType, maxResult);
            }
        };
        IAction<Collection<?>> exampleFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                return hibContainer.getService().findByExample(parameter[0], true);
            }
        };
        IAction<Collection<?>> betweenFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                return hibContainer.getService().findBetween(parameter[0], parameter[1], true, (Integer) parameter[2]);
            }
        };
        IAction<Collection<?>> queryFinder = new CommonAction<Collection<?>>() {
            @Override
            public Collection<?> action() {
                return hibContainer.getService().findByQuery((String) parameter[0],
                    (Boolean) parameter[1],
                    (Object[]) parameter[2],
                    (Class[]) parameter[3]);
            }
        };
        IAction lazyrelationResolver = new CommonAction() {
            @Override
            public Object action() {
                //use the weak implementation of BeanClass to avoid classloader problems!
                if (new BeanClass(parameter[0].getClass()).isAnnotationPresent(Entity.class)) {
                    return hibContainer.getService().instantiateLazyRelationship(parameter[0]);
                } else {
                    return parameter[0];
                }
            }
        };
        IAction saveAction = new CommonAction() {
            @Override
            public Object action() {
                return hibContainer.getService().persist(parameter[0]);
            }
        };
        IAction deleteAction = new CommonAction() {
            @Override
            public Object action() {
                hibContainer.getService().remove(parameter[0]);
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
                return true;//BeanContainer.hasPermission(beanType, packedInList, (String) parameter[0]);
            }
        };
        IAction persistableAction = new CommonAction() {
            @Override
            public Object action() {
                return BeanContainerUtil.isPersistable((Class<?>) parameter[0]);
            }
        };
        final IAction<Integer> executeAction = new CommonAction<Integer>() {
            @Override
            public Integer action() {
                return hibContainer.getService().executeQuery((String) parameter[0],
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

    protected IGenericService getService() {
        if (service == null) {
//            EntityManager entityManager = (EntityManager) getSession(false);
//            service = new GenericLocalServiceBean(entityManager);
            service = new GenericLocalServiceBean();
        }
        return service;
    }

    /**
     * @deprecated: we use direct jpa access through {@link GenericLocalServiceBean}.
     * @param newSession
     * @return
     */
//    @Deprecated
//    protected Session getSession(boolean newSession) {
//        Session session = (Session) get("session");
//        if (session == null || !session.isOpen() || newSession) {
//            LOG.info("Creating new Hibernate-Session");
//            try {
//                SessionFactory sf = (SessionFactory) get("sessionfactory");
//                Configuration cfg = (Configuration) get("configuration");
//                if (sf == null || cfg == null) {
//                    cfg = new AnnotationConfiguration();
//                    cfg = cfg.configure();
//                    Collection beantypes = (Collection) Environment.get("loadedBeanTypes");
//                    Iterator beans = beantypes.iterator();
//                    int i = 0;
//                    Class clazz = null;
//                    while (beans.hasNext()) {
//                        try {
//                            clazz = (Class) beans.next();
//                            LOG.info((int) (i++ / (float) beantypes.size() * 100));
//                            if (clazz.isAnnotationPresent(Entity.class) || clazz.isAnnotationPresent(MappedSuperclass.class)) {
//                                ((AnnotationConfiguration) cfg).addAnnotatedClass(clazz);
//                            } else {
//                                cfg.addClass(clazz);
//                            }
//                        } catch (Exception ex) {
//                            ForwardedException.forward(ex);
//                        }
//                    }
//                    put("configuration", cfg);
//                    sf = cfg.buildSessionFactory();
//                    put("sessionfactory", sf);
//                }
//                if (session != null && session.isOpen()) {
//                    session = session.getSession(EntityMode.POJO);
//                } else {
//                    session = sf.openSession();
//                }
//                put("session", session);
//            } catch (Exception ex) {
//                ForwardedException.forward(ex);
//            }
//        }
//        return session;
//    }

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
