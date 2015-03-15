/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 28.08.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.specification.rules;

import java.io.Serializable;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.bean.def.AbstractDependencyListener;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.core.ENV;

/**
 * dependency listener evaluating its value through a given rule.
 * 
 * @author Tom
 * @version $Revision$
 */
public abstract class RuleDependencyListener<T> extends AbstractDependencyListener<T> implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 5298740573340818934L;
    @Attribute
    String ruleName;

    /**
     * constructor
     */
    public RuleDependencyListener() {
        super();
    }

    /**
     * constructor
     * 
     * @param attribute
     * @param propertyName
     */
    public RuleDependencyListener(AttributeDefinition<T> attribute, String propertyName, String ruleName) {
        super(attribute, propertyName);
        this.ruleName = ruleName;
    }

    /**
     * evaluate a new value through a rule.
     * @param source new foreign value
     * @return result of executing a rule.
     */
    @SuppressWarnings("unchecked")
    protected T evaluate(Object source) {
        return (T) ENV.get(RulePool.class).get(ruleName).run(MapUtil.asMap("master", source));
    }
}
