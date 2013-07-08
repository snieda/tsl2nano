/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 25, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.collection;

import java.util.Collection;
import java.util.Iterator;

/**
 * It is an endless looping iterator - useful for switches and more.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class CyclingIterator<E> implements Iterator<E> {
    Collection<E> parent;
    Iterator<E> parentIt;
    
    /**
     * constructor
     * @param parent
     */
    public CyclingIterator(Collection<E> parent) {
        super();
        this.parent = parent;
        this.parentIt = parent.iterator();
    }

    @Override
    public boolean hasNext() {
        return parent.size() > 0;
    }

    @Override
    public E next() {
        return parentIt.hasNext() ? parentIt.next() : (parentIt = parent.iterator()).next();
    }

    @Override
    public void remove() {
        parentIt.remove();
    }

}
