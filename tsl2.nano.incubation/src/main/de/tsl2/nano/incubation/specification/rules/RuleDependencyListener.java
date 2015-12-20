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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;

import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.bean.def.AbstractDependencyListener;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.messaging.ChangeEvent;

/**
 * dependency listener evaluating its value through a given rule.
 * 
 * @author Tom
 * @version $Revision$
 */
public class RuleDependencyListener<T, E extends ChangeEvent> extends AbstractDependencyListener<T, E> implements
        Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 5298740573340818934L;
    private static final Log LOG = LogFactory.getLog(RuleDependencyListener.class);

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
    public RuleDependencyListener(IAttributeDefinition<T> attribute, String propertyName, String ruleName) {
        super(attribute, propertyName);
        this.ruleName = ruleName;
    }

    /**
     * evaluate a new value through a rule. the events source (a beanvalue) and all parents attributes will be given to
     * the rule as arguments.
     * 
     * @param source new foreign value
     * @return result of executing a rule.
     */
    @SuppressWarnings("unchecked")
    protected T evaluate(E source) {
        BeanValue srcValue = (BeanValue) source.getSource();
        if (getAttribute() == null) {
            LOG.info("trying to materialize attribute <" + attributeID + "> with <event.source> instance:"
                + source.getSource());
            setAttribute(BeanValue.getBeanValue(srcValue.getInstance(),
                StringUtil.substring(attributeID, ".", null, true)));
        }
        else if (!getAttribute().getDeclaringClass().isAssignableFrom(srcValue.getDeclaringClass()))
            throw new IllegalArgumentException("event.source beanvalue should have an declaring class "
                + attribute.getDeclaringClass() + " but was: " + srcValue.getDeclaringClass());
        Map<String, Object> master = Bean.getBean(srcValue.getInstance()).toValueMap(null);
        master.put(StringUtil.substring(attributeID, null, ".", true), srcValue.getInstance());
        master.put(srcValue.getId(), source.newValue);
        return (T) ENV.get(RulePool.class).get(ruleName).run(master);
    }

    @Override
    public String toString() {
        return super.toString() + " rule:" + ruleName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleEvent(E event) {
        ((IValueAccess<T>)getAttribute()).setValue(evaluate(event));
    }
}
