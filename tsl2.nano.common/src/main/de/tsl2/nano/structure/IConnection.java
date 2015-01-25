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



/**
 * 
 * @author Tom
 * @version $Revision$ 
 */
public interface IConnection<CORE, DESCRIPTOR> {
    INode<CORE, DESCRIPTOR> getDestination();
    DESCRIPTOR getDescriptor();
}
