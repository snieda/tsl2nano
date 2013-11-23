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

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.service.util.IGenericService;

/**
 * see {@link GenericLocalServiceBean}.
 * 
 * initializes the {@link BeanContainer} singelton, to use a jpa session.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class GenericLocalBeanContainer extends GenericBeanContainer {

    /**
     * initializes the standard bean container to use GenericService methods. it creates an own servicefactory using the
     * given classloader
     * 
     * @param classloader loader to be used inside the own servicefactory instance.
     * @param checkConnection if true, the generic service will be pre-loaded to check the connection
     */
    public static void initLocalContainer(ClassLoader classloader, boolean checkConnection) {
        GenericLocalBeanContainer container = new GenericLocalBeanContainer();
        if (checkConnection)// pre-load the service
            ((GenericLocalServiceBean)container.getGenService()).checkConnection(true);

        initContainer(container, classloader);
    }

    protected IGenericService getGenService() {
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

}
