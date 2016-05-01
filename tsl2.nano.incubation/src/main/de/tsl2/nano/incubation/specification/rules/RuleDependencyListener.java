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

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.bean.def.AbstractDependencyListener;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.messaging.ChangeEvent;
import de.tsl2.nano.util.PrivateAccessor;

/**
 * dependency listener evaluating its value through a given rule.
 * 
 * @author Tom
 * @version $Revision$
 */
public class RuleDependencyListener<T, E extends ChangeEvent> extends AbstractDependencyListener<T, E> implements Serializable {
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

    @SuppressWarnings("unchecked")
    @Override
    public void handleEvent(E event) {
        initAttribute(event);
        ((IValueAccess<T>) getAttribute()).setValue(evaluate(event));
    }

    /**
     * evaluate a new value through a rule. the events source (a beanvalue) and all parents attributes will be given to
     * the rule as arguments.
     * 
     * @param evt event holding the new foreign value
     * @return result of executing a rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected T evaluate(E evt) {
        BeanValue srcValue = initAttribute(evt);
        Bean<Object> bean = registerChange(evt);
        Map<String, Object> context = bean.toValueMap(null, false, false, false);
        context.put(StringUtil.substring(attributeID, null, ".", true), srcValue.getInstance());
//        context.put(srcValue.getName(), source.newValue);
        return (T) ENV.get(RulePool.class).get(ruleName).run(context);
    }

    protected BeanValue initAttribute(E source) {
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
        return srcValue;
    }

    /**
     * register change on the transient change bean of this listener. this changes are not assigned to the entity yet!
     * 
     * @param evt
     * @param evt event
     * @return transient change bean
     */
    @SuppressWarnings("rawtypes")
    private Bean<Object> registerChange(E evt) {
        BeanValue srcValue = (BeanValue) evt.getSource();
        if (changes == null) //workaround, if no server has set called setChangeObject before
            changes = BeanUtil.copy(srcValue.getInstance());
//        else
//            BeanUtil.merge(srcValue.getInstance(), changes, false);
        Bean<Object> bean = Bean.getBean(changes);
        Object lastValue = bean.getValue(srcValue.getName()); 
        //if nothing was changed, consume it and return
        if (lastValue != null && lastValue.equals(evt.newValue)) {
            evt.breakEvent = true;
            return bean;
        }
            
        //don't call the listener on setting a new transient value
        new PrivateAccessor(bean.getAttribute(srcValue.getName())).set("eventController", null);
//        bean.getAttribute(srcValue.getName()).changeHandler().dispose();
        if (evt.newValue instanceof String)
            bean.setParsedValue(srcValue.getName(), (String) evt.newValue);
        else
            bean.setValue(srcValue.getName(), evt.newValue);
        return bean;
    }

    @Override
    public String toString() {
        return super.toString() + " rule:" + ruleName;
    }
}
