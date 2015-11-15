/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 13.11.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.messaging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.simpleframework.xml.ElementList;

/**
 * Workaround for simple-xml serialization throwing an TransformationException on Map<Class, Arraylist> in
 * EventController.
 * 
 * @author Tom
 * @version $Revision$
 */
public class ListenerList<E> implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -4272308851598689048L;

    @ElementList(empty=true, inline=true)
    List<E> list;
    /**
     * constructor
     */
    protected ListenerList() {
        list = new ArrayList<E>();
    }

    /**
     * constructor
     * 
     * @param c
     */
    protected ListenerList(Collection<? extends E> c) {
        list = new ArrayList<E>(c);
    }

    /**
     * constructor
     * 
     * @param initialCapacity
     */
    protected ListenerList(int initialCapacity) {
        list = new ArrayList<E>(initialCapacity);
    }

    public List<E> getList() {
        return list;
    }
}
