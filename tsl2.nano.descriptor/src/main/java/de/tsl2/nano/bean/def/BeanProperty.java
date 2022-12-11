/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 15, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.bean.def;

/**
 * let the proxy bean be changed after proxy creation. using 'get_' and 'set_'
 * as they shoudn't overlay another interface method.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface BeanProperty {
    Object get_(String key);
    void set_(String key, Object value);
}