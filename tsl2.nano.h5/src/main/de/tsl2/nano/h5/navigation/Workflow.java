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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.incubation.vnet.Net;
import de.tsl2.nano.incubation.vnet.Notification;

/**
 * EJB-Query Navigator reading it's configuration from xml. The Navigator itself uses a parallel-working net
 * implementation on activities.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Workflow extends EntityBrowser implements Serializable, Cloneable {
    /** serialVersionUID */
    private static final long serialVersionUID = 4073303598764074543L;
    @Attribute
    String name;
    @Transient Net<BeanAct, Parameter> net;
    @ElementList(entry = "activity", inline = true, required = true)
    Collection<BeanAct> activities;
    @Transient Map<String, BeanDefinition<?>> cache;
    @Transient Bean<?> login;
    @Transient String asString;
    @Transient Parameter context = new Parameter();

    /**
     * constructor
     */
    public Workflow() {
        this(Messages.getString("unnamed"), null);
    }

    /**
     * constructor
     * 
     * @param activities
     */
    public Workflow(String name, Collection<BeanAct> activities) {
        super(new Stack<BeanDefinition<?>>());
        this.name = name;
        this.activities = activities;
        context = new Parameter();
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
        return (activities == null || activities.isEmpty()) && navigation.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public BeanDefinition<?> next(Object userResponseObject) {
        BeanDefinition<?> fromStack;
        if (current == null) {
            setCurrent(login);
        } else if (!super.isEmpty() && (fromStack = super.next(userResponseObject)) != null) {
            return fromStack;
        } else {
            Object entity =
                userResponseObject instanceof Bean ? ((Bean) userResponseObject).getInstance()
                    : userResponseObject instanceof BeanCollector ? ((BeanCollector) userResponseObject)
                        .getCurrentData() : userResponseObject;
            /*
             * send the response to the net of beanacts to evaluate the next navigation pages
             */
            context.put("response", entity);
            Notification n = new Notification(null, context);
            Collection<BeanDefinition> result = net.notifyAndCollect(n, BeanDefinition.class);
            setCurrent(result.isEmpty() ? null : result.iterator().next());
            navigation.addAll((Collection<? extends BeanDefinition<?>>) result);
        }
        return current;
    }

    protected void setCurrent(BeanDefinition<?> newCurrent) {
        this.current = newCurrent;
        asString = null;
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

    @Override
    public Workflow clone() throws CloneNotSupportedException {
        return new Workflow(name, activities);
    }
    
    @Override
    public String toString() {
        if (asString == null) {
            String acts = StringUtil.toString(activities, 300);
            if (current != null) {
                String c = current.toString();
                acts.replace(c, "*" + c + "*");
            }
            asString = name + "(activities: " + acts + ")";
        }
        return asString;
    }
}
