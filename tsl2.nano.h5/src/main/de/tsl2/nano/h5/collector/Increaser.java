/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 24.07.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.h5.collector;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.IAttributeDefinition;

/**
 * holds properties to create new instances with an increasing attribute
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Increaser<T> implements IItemProvider<T>, Serializable {

    private static final long serialVersionUID = 4984470230289140956L;

    
    private int count;
    private int step;
    private String name;

    protected Increaser() {
    }

    /**
     * constructor
     */
    public Increaser(String name, int count, int step) {
        this.name = name;
        this.count = count;
        this.step = step;
    }

    @Override
    public T createItem(T srcInstance, Map context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<? extends T> createItems(T srcInstance, Map context) {
        return BeanUtil.create(srcInstance, name, null, count, step);
    }

    public String getName() {
        return name;
    }

    public void check(BeanCollector<Collection<T>, T> caller) {
        IAttributeDefinition attribute = caller.getAttribute(getName());
        createItems(caller.createItem(null), null);
    }

    public int getCount() {
        return count;
    }
}
