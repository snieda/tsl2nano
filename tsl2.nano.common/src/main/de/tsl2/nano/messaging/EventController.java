/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Feb 16, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.messaging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ListWrapper;

/**
 * manages bean typed or untyped events. registers {@link IListener}s and fires {@link ChangeEvent} or other events to
 * be handled by the listeners.<br/>
 * please see {@link IListener} fore more details.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Default(value = DefaultType.FIELD, required = false)
public class EventController implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(EventController.class);

    /** all registered value change listeners */
    @ElementMap(entry = "listener", key = "type", keyType = Class.class, valueType = ListWrapper.class, attribute = true, inline = true, required = false)
    //TODO: remove transient if you found a workaround on simple-xml problem with map(collection).
    Map<Class, ListWrapper<IListener>> listener;

    /** if listeners were only added through {@link #addListener(IListener)} - without event type, this is false */
    transient boolean hasTypedListener = false;

    /**
     * internal access method to guarantee a listener collection instance
     * 
     * @return collection of registered listeners
     */
    protected final <T> Collection<IListener> listener(Class<T> eventType) {
        if (listener == null) {
            listener = new LinkedHashMap<Class, ListWrapper<IListener>>();
        }

        if (eventType == null)
            return allListener();
        Class<?> type = hasTypedListener || listener.size() > 1 ? eventType : Object.class;
        ListWrapper<IListener> typedListener = listener.get(type);
        if (typedListener == null) {
            typedListener = new ListWrapper<IListener>();
            listener.put(type, typedListener);
        }
        return typedListener.getList();
    }

    protected final <T> Collection<IListener> allListener() {
        ArrayList<IListener> all = new ArrayList<IListener>();
        if (listener != null) {
            for (ListWrapper<IListener> l : listener.values()) {
                if (l != null) //TODO: why can l be null?
                    all.addAll((Collection<? extends IListener>) l.getList());
            }
        }
        return all;
    }

    /**
     * hasListeners
     * 
     * @return true, if at least one listener was registered
     */
    public boolean hasListeners() {
        return listener != null && listener.size() > 0;
    }

    /**
     * adds a listener for object events. to work on different/typed events, please use
     * {@link #addListener(IListener, Class)}.
     * 
     * @param l listener to register
     */
    public void addListener(IListener l) {
        Class type = BeanUtil.getGenericInterfaceType(l.getClass(), IListener.class, 0);
        addListener(l, type != null ? type : ChangeEvent.class);
    }

    /**
     * adds a typed listener. you may register listeners for different event types.
     * 
     * @param l listener to be registered
     * @param eventType event type. if you only use one type of events, please give {@link Object} or use
     *            {@link #addListener(IListener)}.
     */
    public <T> void addListener(IListener<T> l, Class<T> eventType) {
        hasTypedListener = true;
        //TODO: check type
        listener(eventType).add(l);
    }

    /**
     * unregisters the given listener (for event type {@link Object}). if you work on different types off events, please
     * use {@link #removeListener(IListener, Class)}.
     * 
     * @param l listener to unregister
     * @return true, if existing listener could be removed, otherwise false.
     */
    public boolean removeListener(IListener l) {
        return removeListener(l, null);
    }

    /**
     * unregisters the given listener for the given event type. if you don't work on different event types, please use
     * {@link #removeListener(IListener)} or give {@link Object} as event type.
     * 
     * @param l listener to be unregistered / removed
     * @param eventType event type of listener
     * @return true, if the listener could be removed
     */
    public <T> boolean removeListener(IListener<T> l, Class<T> eventType) {
        if (listener == null) {
            return false;
        }
        if (eventType == null) {
            //search in all eventTypes
            for (ListWrapper<IListener> ls : listener.values()) {
                if (ls != null) {
                    if (ls.getList().remove(l))
                        return true;
                }
            }
            return false;
        } else {
            return listener(eventType).remove(l);
        }
    }

    /**
     * searches for the given listener in the registered listeners and returns the event type of the stored listener.
     * 
     * @param l listener to search
     * @return event type of this listener or null, if not found or undefined
     */
    public Class<?> getEventType(IListener l) {
        if (listener != null) {
            ListWrapper<IListener> ll;
            for (Class type : listener.keySet()) {
                if ((ll = listener.get(type)) != null && ll.getList().contains(l))
                    return type;
            }
        }
        return null;
    }

    /**
     * fires a value change event ( {@link ChangeEvent}) to all registered listeners.
     * 
     * @param source {@link ChangeEvent#getSource()}
     * @param oldValue {@link ChangeEvent#oldValue}
     * @param newValue {@link ChangeEvent#newValue}
     * @param changed {@link ChangeEvent#hasChanged}
     */
    public void fireValueChange(Object source, Object oldValue, Object newValue, boolean changed) {
        fireEvent(new ChangeEvent(source, changed, false, oldValue, newValue));
    }

    /**
     * fires the given event to all listeners, hearing to the type of the given event. if you don't work on different
     * event types, all listeners will be called.
     * 
     * @param e new event to be fired.
     */
    public void fireEvent(Object e) {
        //use a copy of listeners to avoid problems on handlers removing listeners from the list.
        ListWrapper<IListener> listeners = new ListWrapper(listener(e.getClass()));
        for (final IListener l : listeners.getList()) {
            handle(l, e);
        }
    }

    /**
     * calls the handler l with event e. overwrite that method extend the handling.
     * 
     * @param l listener
     * @param e event
     */
    public void handle(IListener l, Object e) {
        LOG.debug("sending event '" + e + "' to listener " + l);
        l.handleEvent(e);
    }

    /**
     * all listeners will be removed. the initial state will be build.
     */
    public void dispose() {
        if (listener != null) {
            listener.clear();
            listener = null;
        }
    }

    public void reset() {
        if (listener != null) {
            Collection<IListener> listeners = getListeners(null);
            for (IListener l : listeners) {
                if (l instanceof IStatefulListener)
                    ((IStatefulListener)l).reset();
            }
        }
    }

    /**
     * getListeners
     * 
     * @param type listener type to evaluate. if type is null, all listeners will be returned
     * @return copy of typed listeners or all listeners
     */
    public Collection<IListener> getListeners(Class eventType) {
        return new ArrayList<IListener>(listener(eventType));
    }
}
