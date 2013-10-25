/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 11.11.2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.vnet.workflow;

import java.io.Serializable;
import java.util.Map;

import de.tsl2.nano.incubation.vnet.ILocatable;
import de.tsl2.nano.incubation.vnet.Notification;
import de.tsl2.nano.messaging.IListener;

/**
 * Technical extension of {@link Activity} to fulfill the preconditions of a node core in vnet.
 * 
 * @author ts
 * @version $Revision$
 */
public abstract class VActivity<S, T> extends Activity<S, T> implements
        IListener<Notification>,
        ILocatable,
        Serializable,
        Comparable<VActivity> {

    /** serialVersionUID */
    private static final long serialVersionUID = 7844715887456324364L;

    public VActivity() {
        init();
    }

    /**
     * constructor
     * @param condition
     * @param expression
     */
    public VActivity(String name, S condition, S expression) {
        super(name, condition, expression);
        init();
    }

    /**
     * to be overridden for initializations - perhaps on inner anonymous classes.
     */
    protected void init() {
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void handleEvent(Notification event) {
        if (canActivate((Map)event.getNotification())) {
            T r = activate((Map)event.getNotification());
            event.addResponse(getPath(), r);
        }
    }

    @Override
    public String getPath() {
        return getShortDescription();
    }

    @Override
    public int compareTo(VActivity o) {
        return getPath().compareTo(o.getPath());
    }
}
