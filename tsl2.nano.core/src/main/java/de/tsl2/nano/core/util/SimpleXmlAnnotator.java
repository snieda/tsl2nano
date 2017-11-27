/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 25.11.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.core.util;

import org.simpleframework.xml.Element;

/**
 * works as an replacement of a reflection proxy (that can't be instantiated through de-serialization). it is a
 * workaround on simple-xml.
 * 
 * @author Tom
 * @version $Revision$
 */
public class SimpleXmlAnnotator<T> {
    @Element(name="ruleCover")
    Object attribute;
    
    /**
     * @return Returns the attribute.
     */
    public Object getAttribute() {
        return attribute;
    }
}
