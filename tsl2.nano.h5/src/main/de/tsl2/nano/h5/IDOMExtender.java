/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 04.11.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.h5;

import org.w3c.dom.Document;

import de.tsl2.nano.core.ISession;

/**
 * extends a prebuild DOM, given by Document - using informations on the current session
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public interface IDOMExtender {
    /** extends a prebuild DOM, given by Document - using informations on the current session */
    void extend(Document doc, ISession<?> session);
}