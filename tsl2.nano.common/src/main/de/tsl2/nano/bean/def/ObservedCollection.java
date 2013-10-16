/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 29, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.lang.reflect.Proxy;
import java.util.Collection;

import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.util.DelegatorProxy;

/**
 * Defines an Observable for a collection - using the {@link DelegatorProxy}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ObservedCollection extends DelegatorProxy {

    /** serialVersionUID */
    private static final long serialVersionUID = 8558587171095667581L;
    transient EventController eventController;

    /**
     * constructor. please use {@link #observe(Collection)} to create an instance
     * 
     * @param delegators
     */
    protected ObservedCollection(Object... delegators) {
        super(delegators);
        eventController = new EventController();

    }

    /**
     * @return Returns the eventController.
     */
    public EventController changeHandler() {
        return eventController;
    }

    /**
     * observe
     * 
     * @param <T> collection
     * @param c collection to be observable
     * @return observable collection. Do a cast to {@link ObservedCollection} to access the {@link #changeHandler()}.
     */
    public static final <T extends Collection<?>> T observe(T c) {
        Observable obs = new Observable();
        obs.c = c;
        obs.changeHandler = new EventController();
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            c.getClass().getInterfaces(),
            new ObservedCollection(obs, c));
    }
}

/**
 * defines the delegator methods for changing a collection.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
class Observable {
    Collection c;
    EventController changeHandler;

    public boolean add(Object o) {
        boolean r = c.add(o);
        changeHandler.fireValueChange(c, null, o, true);
        return r;
    };

    public boolean remove(Object o) {
        boolean r = c.remove(o);
        changeHandler.fireValueChange(c, o, null, true);
        return r;
    };
}