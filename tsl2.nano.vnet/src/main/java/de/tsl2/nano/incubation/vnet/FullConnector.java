/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Nov 29, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.vnet;


/**
 * sample implementation to be used as connection descriptor.
 * <p/>
 * 
 * @param <EDGE> see {@link #edge}
 * @param <VERTEX> describes the docking properties on each sided of a connection. on class diagrams it would be
 *            something like oneToMany and a name.
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class FullConnector<EDGE, VERTEX> {
    /** the edge between the vertexes of a connection. may be a string or/and a weight. */
    EDGE edge;
    /** docking properties on source side of connection */
    VERTEX srcVertex;
    /** docking properties on destination side of connection */
    VERTEX destVertex;
}
