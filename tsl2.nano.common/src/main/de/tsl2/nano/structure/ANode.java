/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 22.01.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.structure;

import java.util.List;

import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.ITransformer;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.messaging.IListener;

/**
 * see {@link INode}
 * 
 * @author Tom
 * @version $Revision$
 */
public class ANode<T, D> implements INode<T, D> {
    /** the nodes real object */
    protected T core;
    /** connections to other nodes in a net */
    protected List<IConnection<T, D>> connections;
    /** controller, handling notification events for other nodes */
    protected EventController controller;

    /**
     * @return Returns the {@link #core}.
     */
    @Override
    public T getCore() {
        return core;
    }

    /**
     * @return Returns the controller.
     */
    public EventController getController() {
        return controller;
    }

    /**
     * @return Returns the {@link #connections}.
     */
    @Override
    public List<IConnection<T, D>> getConnections() {
        return connections;
    }

    /**
     * getConnection
     * 
     * @param destination
     * @return
     */
    public IConnection<T, D> getConnection(INode<T, D> destination) {
        for (IConnection<T, D> c : getConnections()) {
            if (c.getDestination().equals(destination)) {
                return c;
            }
        }
        return null;
    }

    /**
     * convenience to add a connections. delegates to {@link #connect(ANode, Object)} with null descriptor.
     */
    public IConnection<T, D> add(ANode<T, D> destination) {
        return connect(destination, null);
    }

    /**
     * creates a new connection
     * 
     * @param destination node to connect to
     * @param descriptor connection description
     * @return new created connection
     */
    @SuppressWarnings("rawtypes")
    public IConnection<T, D> connect(ANode<T, D> destination, D descriptor) {
        IConnection<T, D> connection = createConnection(destination, descriptor);
        connections.add(connection);
        if (connection instanceof IListener) {
            getController().addListener(((IListener) connection));
        }

        return connection;
    }

    /**
     * creates a connection instance
     * 
     * @param destination node to connect to
     * @param descriptor connection description
     * @return new created connection
     */
    protected IConnection<T, D> createConnection(ANode<T, D> destination, D descriptor) {
        return new AConnection<T, D>(destination, descriptor);
    }

//    public List<T> createChildNodes(List<T> items) {
//        this.connections = new ArrayList<IConnection<CORE, Number>>(items.size());
//        for (CORE c : items) {
//            this.connections.add(new ANode(null, c));
//        }
//        return this.connections;
//    }

    /**
     * getChildNodes
     * 
     * @return transformed list of children-nodes
     */
    protected List<T> getConnectionItems() {
        return CollectionUtil.getTransforming(getConnections(), new ITransformer<IConnection<T, D>, T>() {
            @Override
            public T transform(IConnection<T, D> toTransform) {
                return toTransform.getDestination().getCore();
            }
        });
    }

    protected INode<T, D> getConnection(T item) {
        for (IConnection<T, D> c : getConnections()) {
            if (item.equals(c.getDestination().getCore())) {
                return c.getDestination();
            }
        }
        return null;
    }

    @Override
    public INode<T, D> path(String... nodeFilters) {
        List<T> filter = CollectionUtil.getFiltering(getConnectionItems(), new StringBuilder(nodeFilters[0]));
        if (filter.size() == 0) {
            return null;
        } else if (filter.size() > 1) {
            throw new IllegalArgumentException("node filter " + nodeFilters[0] + " was not unique for tree-children "
                + StringUtil.toString(getConnections(), 100));
        } else {
            return getConnection(filter.iterator().next()).path(
                CollectionUtil.copyOfRange(nodeFilters, 1, nodeFilters.length));
        }
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
        if (this == obj) {
            return true;
        }
        return hashCode() == obj.hashCode();
    }
}
