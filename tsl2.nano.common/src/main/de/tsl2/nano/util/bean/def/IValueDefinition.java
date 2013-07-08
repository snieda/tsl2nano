/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 16, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

/**
 * 
 * @author Thomas Schneider
 * @version $Revision$ 
 */
public interface IValueDefinition<T> extends IAttributeDefinition<T>, IValueAccess<T> {
    /** bean instance, defining this attribute */
    Object getInstance();
    /** returns the relation value - if the attribute value is another bean */
    IValueDefinition<?> getRelation(String name);
}
