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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.StatusInfo;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.util.GraphLog;
import de.tsl2.nano.vnet.Net;
import de.tsl2.nano.vnet.Notification;

/**
 * EJB-Query Navigator reading it's configuration from xml. The Navigator itself uses a parallel-working net
 * implementation on activities.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Workflow extends EntityBrowser implements Cloneable {
    private static final Log LOG = LogFactory.getLog(Workflow.class);
    @Transient Net<BeanAct, Parameter> net;
    @ElementList(entry = "activity", inline = true, required = true)
    Collection<BeanAct> activities;
    @Transient Map<String, BeanDefinition<?>> cache;
    @Transient BeanDefinition<?> login;
    @Transient String asString;
    @Transient Parameter context = new Parameter();
    @Transient StatusInfo status;
    
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
        super(name, new Stack<BeanDefinition<?>>());
        this.activities = activities;
        context = new Parameter();
        net = new Net<BeanAct, Parameter>();
        cache = new HashMap<String, BeanDefinition<?>>();
        setActivities(activities);
        status = new StatusInfo();
    }

	private void setActivities(Collection<BeanAct> activities) {
		if (activities != null) {
	        BeanAct prev = null;
        	for (BeanAct a : activities) {
//        		Message.send(this + " adding activity: " + a);
        		if (prev != null)
        			net.addAndConnect(prev, a, MapUtil.asMap(new Parameter(), "condition", a.getCondition()
        					.replaceAll("[}{)(]", "").replace("&", "&amp;")));
				prev = a;
			}
        }
	}
	
	public String getGraphFileName() {
		return new GraphLog(net.getName()).getFileName();
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(BeanDefinition<?> bean) {
    	throw new UnsupportedOperationException("no beans should be added - only the activities define editable beans!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return (activities == null || activities.isEmpty() || done()) && navigation.isEmpty();
    }

    @Override
    public boolean done() {
        return status.finished();
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public BeanDefinition<?> next(Object userResponseObject) {
        BeanDefinition<?> fromStack;
        if (current == null && !status.finished()) {
            setCurrent(login);
            if (!status.running())
                status.start();
            LOG.info("\n================== STARTING WORKFLOW ==================\n\t" + this);
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
            navigation.addAll((Collection<? extends BeanDefinition<?>>) Util.untyped(result));
        }
        if (current == null) {
            status.stop();
            status.setMsg(name + " finished with status: " + status);
            current = Bean.getBean(status);
            LOG.info("\n================== ENDING WORKFLOW ==================\n\t" + this);
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
    public void setRoot(BeanDefinition<?> login) {
        this.login = login;
    }

    /**
     * status info
     * @return current status info
     */
    public StatusInfo getStatus() {
        return status;
    }
    
    @Persist
    private void initSerializing() {
    	net.graph(null);
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
    public boolean equals(Object obj) {
        return obj instanceof Workflow && hashCode() == obj.hashCode();
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        if (asString == null) {
            String acts = StringUtil.toString(activities, 300);
            if (current != null) {
                String c = current.toString();
                acts.replace(c, "*" + c + "*");
            }
            asString = Util.toString(getClass(), "name: " + name, "login: " + login, "status: " + status, "activities: " + acts);
        }
        return asString;
    }
}
