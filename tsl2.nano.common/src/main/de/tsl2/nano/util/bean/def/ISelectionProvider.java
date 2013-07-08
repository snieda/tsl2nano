/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 20, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

import java.util.Collection;

/**
 * 
 * @author Thomas Schneider
 * @version $Revision$ 
 */
public interface ISelectionProvider<T> extends IValueAccess<Collection<T>> {
    /**
     * getFirstElement
     * @return first selected element or null
     */
    T getFirstElement();
    /**
     * isEmpty
     * @return true, if no selection available
     */
    boolean isEmpty();
}
