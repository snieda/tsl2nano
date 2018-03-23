/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 10.03.2018
 * 
 * Copyright: (c) Thomas Schneider 2018, all rights reserved
 */
package de.tsl2.nano.plugin;

/**
 * All interfaces that should be used by Plugins Proxy have to implement this base interface.
 * 
 * @author Tom
 * @version $Revision$
 */
public interface Plugin {
    default boolean isEnabled() {return true;}
}
