/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 21.01.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.structure;

import java.util.List;

/**
 * simplified generic tree or net interface
 * 
 * @param <CORE> nodes core or content
 * @param <CONNECTOR> nodes connection type
 * @author Tom
 * @version $Revision$
 */
public interface INode<CORE, CONNECTOR> {
    /** return a list of all children */
    List<IConnection<CORE, CONNECTOR>> getConnections();

    /** current nodes core or content */
    CORE getCore();

    /** goes through the given path filter */
    INode<CORE, CONNECTOR> path(String... nodeFilters);

	/**
	 * creates a new connection
	 * 
	 * @param destination node to connect to
	 * @param descriptor connection description
	 * @return new created connection
	 */
	IConnection<CORE, CONNECTOR> connect(ANode<CORE, CONNECTOR> destination, CONNECTOR descriptor);
}
