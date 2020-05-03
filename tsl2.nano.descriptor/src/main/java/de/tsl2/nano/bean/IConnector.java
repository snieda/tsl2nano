/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 17.05.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean;

import java.io.Serializable;


/**
 * connects the current instance to the given reference.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public interface IConnector<REFERENCE> extends Serializable {
    /** connects this object to the given connection end. returns any connection info. */
    Object connect(REFERENCE connectionEnd);
    /** does some cleaning on dis-connection */
    void disconnect(REFERENCE connectionEnd);
    default boolean isConnected() { throw new UnsupportedOperationException();}
}
