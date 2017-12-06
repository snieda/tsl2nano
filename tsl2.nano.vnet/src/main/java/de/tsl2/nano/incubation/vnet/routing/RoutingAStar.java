/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Dec 2, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.vnet.routing;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.incubation.vnet.Connection;
import de.tsl2.nano.incubation.vnet.Node;
import de.tsl2.nano.structure.IConnection;

/**
 * Implements the {@link #connect(Node, Node, float)} method of {@link AbstractRoutingAStar} to create a new connection
 * between a node and its successor.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class RoutingAStar extends AbstractRoutingAStar<Location, Float> {
    static final Float TRACK_MARKER = 1000000f;
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Connection<Location, Float> connect(Node<Location, Float> currentNode,
            Node<Location, Float> successor,
            float f) {
        try {
            //connect copies of both nodes in both directions
            Node<Location, Float> n1 = currentNode.clone();
            Node<Location, Float> n2 = successor.clone();
            //connect the way back with a sign to identify it as a track
            n2.connect(n1, TRACK_MARKER + currentNode.getConnection(successor).getDescriptor());
            //return the requested connection
            return (Connection<Location, Float>) n1.connect(n2, f);
        } catch (CloneNotSupportedException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    @Override
    protected boolean isTrack(IConnection<Location, Float> con) {
        return con.getDescriptor().compareTo(TRACK_MARKER) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void removeTrackMarker(IConnection<Location, Float> connection) {
        ((Connection)connection).setDescriptor(connection.getDescriptor() - TRACK_MARKER);
    }
}
