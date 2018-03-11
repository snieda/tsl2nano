/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 10.03.2018
 * 
 * Copyright: (c) Thomas Schneider 2018, all rights reserved
 */
package de.tsl2.nano.inspection;

/**
 * All interfaces that should be used by Inspectors Proxy have to implement this base interface.
 * 
 * @author Tom
 * @version $Revision$
 */
public interface Inspector {
    boolean isEnabled();
}
