/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 28.07.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.util.ArrayList;
import java.util.Collection;

import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;

/**
 * On relational models, a composition child (like the uml-composition) needs a connection to it's parent. If a new
 * child is created, this child has to be put into the attribute list of it's parent.
 * <p/>
 * Example: Customer(1)==>(*)Adress. But the adress cannot exist without it's parent, the customer.
 * <p/>
 * If a relation model needs to resolve a connection of type: Group (*) ==> (*) Player, a new table 'PlayersGroup'
 * should be created: Group (*) ==> (1) PlayersGroup (1) ==> (*) Player. The #target is required to resolve this
 * many-to-many relation. In this example, the members are:<br/>
 * parent = Group<br/>
 * child = PlayersGroup<br/>
 * target = Player
 * 
 * @author ts
 * @version $Revision$
 */
public class Composition<C> {
    /** parent-to-child attribute, holding the parent instance */
    BeanValue<C> parent;
    /** child-to-target attribute definition */ 
    BeanAttribute<?> target;

    /**
     * constructor
     * 
     * @param parent see {@link #parent}
     */
    Composition(BeanValue<C> parent) {
        this(parent, null);
    }

    /**
     * constructor
     * 
     * @param parent see {@link #parent}
     * @param target see {@link #target}
     */
    Composition(BeanValue<C> parent, BeanAttribute<?> target) {
        super();
        this.parent = parent;
        this.target = target;
    }

    /**
     * @return Returns the parent.
     */
    public Object getParent() {
        return parent.getInstance();
    }

    /**
     * getParentContainer
     * @return parents container holding the child items
     */
    @SuppressWarnings("unchecked")
    protected Collection<C> getParentContainer() {
        return (Collection<C>)parent.getValue();
    }
    
    /**
     * add new item to parents container
     * @param newItem new child item
     */
    public void add(C newItem) {
        add(newItem, null);
    }
    
    /**
     * add new item to parents container
     * @param newItem new child item
     * @param target (optional) target to be assigned to the child item
     */
    public void add(C newItem, Object target) {
        if (target != null) {
            this.target.setValue(newItem, target);
        }
        getParentContainer().add(newItem);
    }
    
    /**
     * remove  item from its parent container
     * @param item to be removed
     */
    public void remove(C item) {
        getParentContainer().remove(item);
    }
    
    /**
     * @return Returns the target.
     */
    public Class<?> getTargetType() {
        return target != null ? target.getType() : null;
    }

    /**
     * creates a new child, connecting it to the parent instance
     * @return new child
     */
    public C createChild() {
        return createChild(null);
    }

    /**
     * creates a new child, connecting it to the parent instance and to the given target instance
     * @param target (optional) target instance
     * @return new child
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public C createChild(Object target) {
        BeanClass bc = BeanClass.getBeanClass(parent.getType());
        C child = (C) bc.createInstance();
        add(child, target);
        return child;
    }

    /**
     * creates new childs (see {@link #createChild(Object)}) for all given targets.
     * @param targets targets to create childs for
     * @return new childs, connected to parent and target
     */
    public Collection<C> createChilds(Collection<?> targets) {
        Collection<C> childs = new ArrayList<C>(targets.size());
        for (Object t : targets) {
            childs.add(createChild(t));
        }
        return childs;
    }
}
