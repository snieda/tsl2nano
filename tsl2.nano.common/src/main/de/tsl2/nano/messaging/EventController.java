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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * manages bean typed or untyped events. registers {@link IListener}s and fires {@link ChangeEvent} or other events to
 * be handled by the listeners.<br/>
 * please see {@link IListener} fore more details.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class EventController {
    /** all registered value change listeners */
//    private Collection<IListener> listener;
    private Map<Class, Collection<IListener>> listener;

    /** true, if only object-event-type listeners where registered */
    boolean onlyObjectListeners = true;

    /**
     * internal access method to guarantee a listener collection instance
     * 
     * @return collection of registered listeners
     */
    protected final <T> Collection<IListener> listener(Class<T> eventType) {
        if (listener == null) {
            listener = new LinkedHashMap<Class, Collection<IListener>>();
        }

        Class<?> type = onlyObjectListeners ? Object.class : eventType;
        Collection<IListener> typedListener = listener.get(type);
        if (typedListener == null) {
            typedListener = new ArrayList<IListener>();
            listener.put(type, typedListener);
        }
        return typedListener;
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
        addListener(l, Object.class);
    }

    /**
     * adds a typed listener. you may register listeners for different event types.
     * 
     * @param l listener to be registered
     * @param eventType event type. if you only use one type of events, please give {@link Object} or use
     *            {@link #addListener(IListener)}.
     */
    public <T> void addListener(IListener<T> l, Class<T> eventType) {
        if (onlyObjectListeners && !eventType.isAssignableFrom(Object.class))
            onlyObjectListeners = false;
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
        return removeListener(l, Object.class);
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
        return listener(eventType).remove(l);
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
        Collection<IListener> listeners = new ArrayList(listener(e.getClass()));
        for (final IListener l : listeners) {
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
        onlyObjectListeners = true;
    }
}
