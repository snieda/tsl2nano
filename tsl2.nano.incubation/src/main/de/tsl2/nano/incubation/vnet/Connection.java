/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 09.11.2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.vnet;

import java.io.Serializable;

import de.tsl2.nano.core.messaging.EventController;
import de.tsl2.nano.core.messaging.IListener;
import de.tsl2.nano.structure.IConnection;

/**
 * a connection is used as a one-direction link from a source-node to a destination-node. the source-node may have a set
 * of connections, knowing each destination and the connection-properties (e.g. the weight).
 * <p/>
 * to create a bidirectional link, you additionally need to create a connection from destination-node to source-node. if
 * you need complex connection properties (=descriptor), you may use {@link FullConnector} as descriptor.
 * 
 * @param <T> type of {@link Node} content (=core)
 * @param <D> connection descriptor (on simple weighted connections, it would be Float)
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Connection<T extends IListener<Notification> & ILocatable & Serializable & Comparable<? super T>, D extends Comparable<? super D>> extends
        Link<Node<T, D>, D> implements IListener<Notification>, ILocatable, IConnection<T, D> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1981668952548892244L;

    EventController eventController;

    /**
     * constructor
     * 
     * @param destination
     * @param descriptor
     */
    public Connection(Node<T, D> destination, D descriptor) {
        super(destination, descriptor);
        eventController = Net.createEventController();
    }

    /**
     * @param destination The destination to set.
     */
    @Override
    public void setDestination(Node<T, D> destination) {
        super.setDestination(destination);
        //TODO: fire change event
    }

    @Override
    public void handleEvent(Notification event) {
        getDestination().handleEvent(new Notification(null, descriptor, event));
    }

    /**
     * numeric representation of current connection. please check your {@link #descriptor}s hashCode() method to
     * evaluate a length as int.
     * 
     * @return
     */
    public float length() {
        return descriptor != null ? Float.intBitsToFloat(descriptor.hashCode()) : 0f;
    }

    @Override
    public String getPath() {
        return getDestination().getPath();
    }
}
