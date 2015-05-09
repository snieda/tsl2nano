/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 16, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import de.tsl2.nano.bean.IValueAccess;

/**
 * combination of {@link IAttributeDefinition} and {@link IValueAccess}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IValueDefinition<T> extends IAttributeDefinition<T>, IValueAccess<T> {
    /** bean instance, defining this attribute */
    Object getInstance();

    /** returns true, if this attribute is a relation (foreign key) to another bean */
    @Override
    boolean isRelation();
    
    /** returns the relation value - if the attribute value is another bean */
    IValueDefinition<?> getRelation(String name);

    /** returns true, if attribute is of type {@link IValueAccess} - means, it belongs not to the parents bean-class. */
    @Override
    public boolean isVirtual();
}
