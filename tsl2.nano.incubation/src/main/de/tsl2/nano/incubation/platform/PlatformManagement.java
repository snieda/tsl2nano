/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 14.07.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.incubation.platform;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.management.InstanceNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Provides some generic informations from managed beans through {@link ManagementFactory}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class PlatformManagement {

    /**
     * getManagedValues
     * 
     * @param classNameFilter (optional) regular expression for the full classname. E.g.: .*Thread.*
     * @return all managed beans
     */
    public static Map<String, PlatformManagedObject> getManagedMBeans(String classNameFilter) {
        Map<String, PlatformManagedObject> mbeans = new TreeMap<String, PlatformManagedObject>();
        Method[] methods =
            BeanClass.getBeanClass(ManagementFactory.class).getMethods(PlatformManagedObject.class, true);
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getParameterTypes().length > 0)
                continue;
            if (classNameFilter == null || methods[i].getName().matches(classNameFilter)) {
                try {
                    mbeans.put(BeanAttribute.getName(methods[i]), (PlatformManagedObject) methods[i].invoke(null));
                } catch (Exception e) {
                    ManagedException.forward(e);
                }
            }
        }
        return mbeans;
    }

    /**
     * printMBeans
     * 
     * @param out output
     * @param classNameFilter (optional) regular expression for the full classname
     */
    public static void printMBeans(PrintStream out, String classNameFilter) {
        out.print(getMBeanInfo(classNameFilter));
    }

    /**
     * getMBeanInfo
     * @param classNameFilter (optional) regular expression for the full classname
     * @return information of all MBeans
     */
    public static StringBuilder getMBeanInfo(String classNameFilter) {
        Map<String, PlatformManagedObject> managedMBeans = getManagedMBeans(classNameFilter);
        Set<String> keys = managedMBeans.keySet();
        Bean bean;
        StringBuilder buf = new StringBuilder();
        for (String name : keys) {
            if (classNameFilter == null || name.toLowerCase().matches(classNameFilter)) {
                buf.append(name + ":\n");
                bean = Bean.getBean(managedMBeans.get(name));
                buf.append(StringUtil.toFormattedString(bean.toValueMap(new Properties()), -1, true));
            }
        }
        return buf;
    }

    /**
     * logNotifications
     * 
     * @param classNameFilter (optional) regular expression for the full classname
     */
    public static void logNotifications(final String classNameFilter) {
        Map<String, PlatformManagedObject> managedMBeans = getManagedMBeans(classNameFilter);
        Collection<PlatformManagedObject> mbeans = managedMBeans.values();
        for (PlatformManagedObject mb : mbeans) {
            if (mb instanceof NotificationEmitter) {
                NotificationFilter filter = new NotificationFilter() {
                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        return classNameFilter == null || notification.getClass().getName().matches(classNameFilter);
                    }
                };
                try {
                    ManagementFactory.getPlatformMBeanServer().addNotificationListener(mb.getObjectName(),
                        new NotificationLogger(), filter, null);
                } catch (InstanceNotFoundException e) {
                    ManagedException.forward(e);
                }
            }
        }
    }
}

class NotificationLogger implements NotificationListener {
    private static final Log LOG = LogFactory.getLog(NotificationListener.class);

    @Override
    public void handleNotification(Notification notification, Object handback) {
        LOG.info(StringUtil.toFormattedString(Bean.getBean(notification), -1));
    }
}