/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 14.08.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

import java.util.LinkedList;
import java.util.List;

import de.tsl2.nano.util.bean.BeanContainer;

/**
 * While instances of {@link Bean} are not cached and {@link BeanCollector}s hold collections of java instances, we have
 * to store compositions in an extra cache. Please call {@link #createComposition(BeanValue)} to create a new
 * composition and finish the composition handling calling {@link #persist(Object)}.
 * 
 * @author ts
 * @version $Revision$
 */
public class CompositionFactory {
    private static CompositionFactory self;

    List<Composition<?>> allCompositions;

    /**
     * constructor
     */
    private CompositionFactory() {
        super();
        allCompositions = new LinkedList<Composition<?>>();
    }

    public static final CompositionFactory instance() {
        if (self == null)
            self = new CompositionFactory();
        return self;
    }

    /**
     * createComposition
     * 
     * @param parent
     * @return new composition
     */
    public static final Composition createComposition(BeanValue<?> parent) {
        Composition composition = new Composition(parent);
        instance().allCompositions.add(composition);
        return composition;
    }

    /**
     * searches the given child in the composition cache and persists the child through it's parent. the composition
     * will be removed from cache.
     * 
     * @param compChild child to persist
     * @return true, if child was found and persisted, otherwise false
     */
    public static final boolean persist(Object compChild) {
        for (Composition<?> c : instance().allCompositions) {
            if (contains(c, compChild)/*c.getParentContainer().contains(compChild)*/) {
                BeanContainer.instance().save(c.parent.getInstance());
                return instance().allCompositions.remove(c);
            }
        }
        return false;
    }
    
    //Workaround while hashset.contains(child) always returns true!
    static private boolean contains(Composition c, Object child) {
        for (Object obj : c.getParentContainer()) {
            if (obj == child)
                return true;
        }
        return false;
    }
}