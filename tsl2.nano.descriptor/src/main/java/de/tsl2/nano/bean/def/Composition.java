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

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.util.StringUtil;

/**
 * <pre>
 * On relational models, a composition child (like the uml-composition) needs a connection to it's parent. If a new
 * child is created, this child has to be put into the attribute list of it's parent.
 * 
 * Example: Customer(1)==>(*)Address. But the address cannot exist without it's parent, the customer.
 *
 * If a relation model needs to resolve a connection of type: Group (*) ==> (*) Player, a new table 'PlayersGroup'
 * should be created as resolver: Group (*) ==> (1) PlayersGroup (1) ==> (*) Player. The #target is required to resolve this
 * many-to-many relation. In this example, the members are:<br/>
 * parent = Group<br/>
 * child = PlayersGroup<br/>
 * target = Player
 * 
 * a second case is:
 * 
 * Group (1) ==> Players (*) ==> (1) Type
 * 
 * here the resolver is a collection.
 * 
 * </pre> 
 * @author ts
 * @version $Revision$
 */
public class Composition<C> {
    /** parent-to-child (base) attribute, holding the parent (base)instance. the child works as a resolver in the middle */
    BeanValue<C> parent;
    /** child-to-target attribute definition. the child works as a resolver in the middle */
    @SuppressWarnings("rawtypes")
    IAttribute target;

    /**
     * constructor
     * 
     * @param parent see {@link #parent}
     */
    public Composition(BeanValue<C> parent) {
        this(parent, null);
    }

    /**
     * constructor
     * 
     * @param parent see {@link #parent}
     * @param target see {@link #target}
     */
    public Composition(BeanValue<C> parent, IAttribute<?> target) {
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
     * 
     * @return parents container holding the child items
     */
    @SuppressWarnings("unchecked")
    protected Collection<C> getParentContainer() {
        return (Collection<C>) parent.getValue();
    }

    /**
     * add new item to parents container
     * 
     * @param newItem new child item
     */
    public void add(C newItem) {
        add(newItem, null);
    }

    /**
     * add new item to parents container
     * 
     * @param newItem new child item  (resolver)
     * @param targetValue (optional) target to be assigned to the child item
     */
    @SuppressWarnings("unchecked")
    public void add(C newItem, Object targetValue) {
        if (this.target != null && targetValue != null) {
            this.target.setValue(newItem, targetValue);
        }
        getParentContainer().add(newItem);
    }

    /**
     * remove item from its parent container
     * 
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
     * 
     * @return new child
     */
    public C createChild() {
        return createChild(null);
    }

    /**
     * creates a new child (resolver), connecting it to the parent instance and to the given target instance. call this method, if
     * your target describes the middle of the connection. assigns the target value to the child instance
     * 
     * @param targetValue (optional) target value
     * @return new child
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public C createChild(Object targetValue) {
        BeanClass bc = BeanClass.getBeanClass(parent.getType());
        C child = (C) bc.createInstance();
        add(child, targetValue);//assign the target value to the child instance
        return child;
    }

    /**
     * creates a new child (resolver), connecting it to the parent instance and to the given target instance. call this when your
     * target attribute describes the right side of the connection. assigns the child to the target instance
     * 
     * @param targetInstance (optional) target instance
     * @return new resolving child
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public C createChildOnTarget(Object targetInstance) {
        BeanDefinition<C> bc = BeanDefinition.getBeanDefinition(Collection.class.isAssignableFrom(parent.getType()) ? parent.getGenericType(0) : parent.getType());
        C child = (C) ((target == null && targetInstance != null) || (bc.getClazz().isAssignableFrom(targetInstance.getClass()) 
        		&& !(targetInstance instanceof Collection)) 
        		? targetInstance : bc.createInstance());
        if (this.target != null && targetInstance != null) {
            if (Collection.class.isAssignableFrom(this.target.getType())) {
                ((Collection)this.target.getValue(targetInstance)).add(child);
                bc.setValue(child, StringUtil.toFirstLower(target.getDeclaringClass().getSimpleName()), targetInstance);
            } else {// standard: only a single target instance 
                BeanContainer.createId(child);
                if (this.target.getType().isAssignableFrom(child.getClass()))
                    this.target.setValue(targetInstance, child);
            }
	        IAttribute attrTarget = bc.getAttribute(target.getDeclaringClass());
	        if (attrTarget != null && targetInstance != null)
	            attrTarget.setValue(child, targetInstance);
        } else {
            BeanContainer.createId(child);
        }
        IAttribute attrParent = bc.getAttribute(parent.getDeclaringClass());
        if (attrParent != null)
            attrParent.setValue(child, parent.getInstance());
        
//        if (parent.isMultiValue() && parent.getValue() != null)
//        	getParentContainer().add(child);
//        else if (parent.getType().isAssignableFrom(child.getClass()))
//        	parent.setValue(child);
//        else
//        	;//TODO: throw new ....
        return child;
    }

    /**
     * creates new childs (see {@link #createChild(Object)}) for all given targets.
     * 
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
