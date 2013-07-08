/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 29, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.fi;

import java.util.Date;

/**
 * 
 * @author Thomas Schneider
 * @version $Revision$ 
 */
public interface DateCallback {
    Date getDate();
    void setDate(Date date);
}
