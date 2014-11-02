/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 23.10.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.util.Collection;

import de.tsl2.nano.bean.IConnector;

/**
 * to be extendable...
 * <p/>
 * The pluggable implementation is like a parent for the connectors. The pluggable itself should be the CONNECTOREND.
 * 
 * @author Tom
 * @version $Revision$
 */
public interface IPluggable<CONNECTOREND> {
    /**
     * getPlugins
     * 
     * @return all available connectors to this instance
     */
    Collection<IConnector<CONNECTOREND>> getPlugins();

    /**
     * the given plugin will be connected to *this*, calling plugin.connect(this).
     * 
     * @param plugin The plugin to add.
     */
    void addPlugin(IConnector<CONNECTOREND> plugin);

    /**
     * the given plugin will be disconnected from *this*, calling plugin.disconnect(this).
     * 
     * @param plugin to remove
     * @return true, if plugin was removed
     */
    boolean removePlugin(IConnector<CONNECTOREND> plugin);
}
