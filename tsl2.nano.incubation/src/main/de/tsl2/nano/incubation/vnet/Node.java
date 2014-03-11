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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.messaging.IListener;

/**
 * Simple abstract network node to be filled with real objects (cores). the real objects will be registered to network
 * changes or notifications by implementing {@link IListener}. The node has to be locatable to fire notifications events
 * to the right locations.
 * <p/>
 * The Node is a wrapper implementing standard interfaces like {@link Serializable}, {@link Comparable},
 * {@link Cloneable} to delegate to its core element of type T.
 * 
 * @param <T> type of {@link Node} content (=core)
 * @param <D> connection descriptor (on simple weighted connections, it would be Float)
 * @author Thomas Schneider
 * @version $Revision$
 */
public class Node<T extends IListener<Notification> & ILocatable & Serializable & Comparable<? super T>, D extends Comparable<? super D>> implements
        IListener<Notification>,
        ILocatable,
        Comparable<Node<T, D>>,
        Cloneable,
        Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 3229162660341988123L;
    /** the nodes real object */
    T core;
    /** connections to other nodes in a net */
    Set<Connection<T, D>> connections;

    /** controller, handling notification events for other nodes */
    EventController controller;
    private AtomicInteger status = new AtomicInteger(STATUS_IDLE);
    NodeStatistics statistics;

    private static final int STATUS_IDLE = 0;
    private static final int STATUS_NOTIFIED = 1;
    private static final int STATUS_WORKING = 2;
    private static final int STATUS_DELEGATING = 3;

    /**
     * constructor
     * 
     * @param core {@link #core}
     * @param connections {@link #connections}
     */
    public Node(T core, Set<Connection<T, D>> connections) {
        super();
        controller = Net.createEventController();
        statistics = new NodeStatistics();

        this.core = core;
        if (connections != null) {
            this.connections = connections;
            for (Connection<T, D> connection : connections) {
                controller.addListener(connection);
            }
        } else {
            this.connections = new HashSet<Connection<T, D>>();
        }
    }

    /**
     * @return Returns the {@link #core}.
     */
    public T getCore() {
        return core;
    }

    /**
     * @return Returns the {@link #connections}.
     */
    public Set<Connection<T, D>> getConnections() {
        return connections;
    }

    /**
     * getConnection
     * 
     * @param destination
     * @return
     */
    public Connection<T, D> getConnection(Node<T, D> destination) {
        for (Connection<T, D> c : getConnections()) {
            if (c.getDestination().equals(destination))
                return c;
        }
        return null;
    }

    /**
     * connect
     * 
     * @param destination node to connect to
     * @param descriptor connection description
     * @return new created connection
     */
    public Connection<T, D> connect(Node<T, D> destination, D descriptor) {
        getController().addListener(destination);

        Connection<T, D> connection = new Connection<T, D>(destination, descriptor);
        connections.add(connection);
        return connection;
    }

    /**
     * @return Returns the controller.
     */
    public EventController getController() {
        return controller;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() {
        return core.getPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleEvent(Notification event) {
        waitToBeIdle();
        increaseStatus();
        Net.log_("node " + core + " starts working on " + event + "...");
        try {
            long start = System.currentTimeMillis();
            core.handleEvent(event);
            long workingTime = System.currentTimeMillis() - start;
//        decreaseStatus();
            Net.log("work done on " + event + " in " + workingTime + " msecs");
            statistics.addWorkingTime(workingTime);

            /*
             * send the notification to all neighbours
             * each connection has a different weight, the connection.handle() will handle that!
             */
            event.path = null;
            controller.fireEvent(event);
        } finally {
            setIdle();
        }
    }

    private void waitToBeIdle() {
        long start = System.currentTimeMillis();
        if (isWorking())
            Net.log("waiting for working node " + this);
        while (isWorking()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                ManagedException.forward(e);
            }
        }
        Net.log("node " + this
            + " is ready to working (after waiting for "
            + (System.currentTimeMillis() - start)
            + " msec");
    }

    /**
     * notify
     * 
     * @param notification
     */
    public void notify(Notification notification) {
        //sets the status to notified - the handle-runner will reset it to idle after finishing
        increaseStatus();
        controller.handle(this, notification);
    }

    /**
     * @return Returns the idle.
     */
    public boolean isIdle() {
        return status.get() == STATUS_IDLE;
    }

    /**
     * @return Returns the idle.
     */
    public boolean isWorking() {
        return status.get() >= STATUS_WORKING;
    }

    private void setIdle() {
        this.status.set(STATUS_IDLE);
    }

    private void increaseStatus() {
        if (status.get() > STATUS_WORKING)
            throw new IllegalStateException();
        status.incrementAndGet();
    }

    private void decreaseStatus() {
        if (status.get() < STATUS_IDLE)
            throw new IllegalStateException();
        status.decrementAndGet();
    }

    public NodeStatistics getStatistics() {
        return statistics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((core == null) ? 0 : core.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        return compareTo((Node<T, D>) obj) == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Node<T, D> o) {
        return core.compareTo(o.core);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node<T, D> clone() throws CloneNotSupportedException {
        Node<T, D> c = (Node<T, D>) super.clone();
        c.connections = new HashSet<Connection<T, D>>(connections);
        c.core = core;
        return c;
    }

    public String dump() {
        return toString() + " " + statistics.toString() + ", connections: " + StringUtil.toString(connections, 200);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return core + "(x" + connections.size() + ")";
    }
}
