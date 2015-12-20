/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 13.11.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.simpleframework.xml.ElementList;

/**
 * Workaround for simple-xml serialization throwing an TransformationException on Lists that are not self annotated with
 * ElementList.
 * 
 * @author Tom
 * @version $Revision$
 */
public class ListWrapper<E> implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -4272308851598689048L;

    @ElementList(empty = true, inline = true)
    List<E> list;

    /**
     * constructor
     */
    public ListWrapper() {
        list = new ArrayList<E>();
    }

    /**
     * constructor
     * 
     * @param c
     */
    public ListWrapper(Collection<? extends E> c) {
        list = new ArrayList<E>(c);
    }

    /**
     * constructor
     * 
     * @param initialCapacity
     */
    public ListWrapper(int initialCapacity) {
        list = new ArrayList<E>(initialCapacity);
    }

    public List<E> getList() {
        return list;
    }
    
    @Override
    public String toString() {
        return Util.toString(getClass(), "list: " + list);
    }
}
