/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 17.10.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.tsl2.nano.bean.def.IAttributeDefinition;

/**
 * This cover overrules its delegate through evaluating given rules.
 * 
 * @author Tom
 * @version $Revision$
 */
public interface IRuleCover<T> {
    /**
     * checks, whether for the given property a rule is 'covered'. If a rule is defined, this method should check,
     * whether the rule is accessible.
     * 
     * @param propertyPath a property of the origin object (see {@link #getDelegate()}.
     * @return true, if a rule is defined for that property and the rule is accessible.
     */
    boolean hasRule(String propertyPath);

    /**
     * evaluates the rule result for the given property of the covered object.
     * 
     * @param propertyPath
     * @return
     */
    Object eval(String propertyPath);

    /**
     * normally the type name of the origin object (see {@link #getDelegate()}.
     * 
     * @return cover name
     */
    String getName();

    /**
     * getDelegate
     * 
     * @return the origin object (that is covered)
     */
    T getDelegate();

    /**
     * see {@link #setContext(Object)}.
     * 
     * @return the evaluated properties of the context object.
     */
    Map<String, Object> getContext();

    /**
     * the context object is any object with any attributes which values has to be evaluated on runtime e.g. through
     * BeanClass.toValueMap(..).
     * 
     * @param contextObject
     */
    void setContext(Serializable contextObject);
    
    /* technical workaround for performance */
    
    /** to perform on injecting instances into rule-covers, this set holds all types that have to be covered */
    static Set<Class<?>> cachedConnectionEndTypes = new HashSet<>();
    
    public static void resetTypeCache() {
        cachedConnectionEndTypes.clear();
    }
}
