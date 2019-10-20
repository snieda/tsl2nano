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

import java.util.HashMap;
import java.util.Map;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.core.ENV;
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
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class GenericBeanContainer extends BeanContainerUtil {
    protected Map properties = new HashMap();

    /**
     * initializes the standard bean container to use GenericService methods. it creates an own servicefactory using the
     * given classloader
     * 
     * @param classloader loader to be used inside the own servicefactory instance.
     */
    public static void initContainer(final GenericBeanContainer container, ClassLoader classloader) {
        ENV.addService(IGenericService.class, container.getGenService());
        initGenericServices(container, () -> container.getGenService());
    }

    protected Object hasPermission(String name, String action) {
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
