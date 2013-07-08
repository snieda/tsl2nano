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

import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.incubation.vnet.Connection;
import de.tsl2.nano.incubation.vnet.Node;

/**
 * Implements the {@link #connect(Node, Node, float)} method of {@link AbstractRoutingAStar} to create a new connection
 * between a node and its successor.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class RoutingAStar extends AbstractRoutingAStar<Location, Float> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Connection<Location, Float> connect(Node<Location, Float> currentNode,
            Node<Location, Float> successor,
            float f) {
        try {
            return currentNode.clone().connect(successor, f);
        } catch (CloneNotSupportedException e) {
            ForwardedException.forward(e);
            return null;
        }
    }

}
