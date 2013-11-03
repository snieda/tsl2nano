/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 02.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.h5.navigation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.incubation.vnet.Net;
import de.tsl2.nano.incubation.vnet.Notification;
import de.tsl2.nano.util.StringUtil;

/**
 * EJB-Query Navigator reading it's configuration from xml. The Navigator itself uses a parallel-working net
 * implementation on acitivities.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Workflow implements IBeanNavigator {
    transient Net<BeanAct, Parameter> net;
    @ElementList(entry = "activity", inline = true, required = true)
    Collection<BeanAct> activities;
    transient BeanDefinition<?> current;
    transient Map<String, BeanDefinition<?>> cache;
    transient Bean<?> login;

    /**
     * constructor
     */
    public Workflow() {
        this(null);
    }

    /**
     * constructor
     * 
     * @param activities
     */
    public Workflow(Collection<BeanAct> activities) {
        super();
        this.activities = activities;
        net = new Net<BeanAct, Parameter>();
        cache = new HashMap<String, BeanDefinition<?>>();
        if (activities != null)
            net.addAll(activities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(BeanDefinition<?> bean) {
        //no beans will be added - only the activities define editable beans.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return activities == null || activities.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BeanDefinition<?> current() {
        return current;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public BeanDefinition<?> next(Object userResponseObject) {
        if (current == null) {
            current = login;
        } else {
            Object entity = userResponseObject instanceof Bean ? ((Bean) userResponseObject).getInstance()
                : userResponseObject;
            Parameter p = new Parameter();
            p.put("response", entity);
            Notification n = new Notification(null, p);
            Collection<BeanDefinition> result = net.notifyAndCollect(Arrays.asList(n), BeanDefinition.class);
            //all results may be equal
            current = result.isEmpty() ? null : result.iterator().next();
        }
        return current;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BeanDefinition<?> fromUrl(String uri) {
        String link = StringUtil.substring(uri, "/", null, true);
        return cache.get(link);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BeanDefinition<?>[] toArray() {
        return cache.values().toArray(new BeanDefinition[0]);
    }

    /**
     * setLogin
     * 
     * @param login
     */
    public void setLogin(Bean<?> login) {
        this.login = login;
    }

    @Commit
    private void initDeserializing() {
        net.addAll(activities);
    }

}
