/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 15, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

/**
 * let the proxy bean be changed after proxy creation.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface BeanProperty {
    Object getProperty(String key);

    void setProperty(String key, Object value);
}