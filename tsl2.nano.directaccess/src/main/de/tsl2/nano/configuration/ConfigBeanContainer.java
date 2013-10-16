/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Mar 25, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.configuration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.persistence.GenericBeanContainer;
import de.tsl2.nano.persistence.GenericLocalServiceBean;
import de.tsl2.nano.service.util.IGenericService;

/**
 * see {@link GenericLocalServiceBean}.
 * 
 *             initializes the {@link BeanContainer} singelton, to use a hibernate session.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({"rawtypes"})
public class ConfigBeanContainer extends GenericBeanContainer {
    protected Map properties = new HashMap();
    protected IGenericService service;

    /**
     * initializes the standard bean container to use GenericService methods. it creates an own servicefactory using the
     * given classloader
     * 
     * @param classloader loader to be used inside the own servicefactory instance.
     */
    public static void initFileContainer(ClassLoader classloader) {
        initContainer(new ConfigBeanContainer(), classloader);
    }

    protected IGenericService getService() {
        if (service == null) {
            service = new ConfigServiceBean();
        }
        return service;
    }
    public static boolean isPersistable(Class<?> beanClass) {
        return Serializable.class.isAssignableFrom(beanClass);
    }
}
