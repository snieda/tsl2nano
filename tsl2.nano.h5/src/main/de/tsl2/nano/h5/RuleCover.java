/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 17.10.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5;

import java.util.Map;

import de.tsl2.nano.bean.def.AttributeCover;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.incubation.specification.Pool;

/**
 * See {@link AttributeCover}, using the {@link RulePool} from specification.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RuleCover<T> extends AttributeCover<T> {
    private static final long serialVersionUID = -4157641723681767640L;

    /**
     * constructor
     */
    protected RuleCover() {
    }

    /**
     * constructor
     * 
     * @param delegate
     * @param context
     */
    public RuleCover(String name, Map<String, String> rules) {
        super(name, rules);
    }

    /**
     * convenience to create and connect a rulecover to an attribute through a string descriptor.
     * 
     * <pre>
     * syntax: <attribute>:<child>--><rule>
     * 
     * Example: value:presentable.layoutconstraints-->%presValueColor
     * </pre>
     * 
     * @param ruleCoverDescriptor
     * @return {@link RuleCover} instance
     */
    public static RuleCover cover(Class cls, String ruleCoverDescriptor) {
        return AttributeCover.cover(RuleCover.class, cls, ruleCoverDescriptor);
    }

    /**
     * covers the given child (as member!) of the instance attribute with the given rule
     * 
     * @param cls class holding the attribute
     * @param attr class attribute
     * @param child child (member!) of attribute
     * @param rule rule to cover the attribute child
     * @return {@link RuleCover} instance
     */
	public static RuleCover cover(Class cls, String attr, String child, String rule) {
        return cover(BeanDefinition.getBeanDefinition(cls).getAttribute(attr), child, rule);
    }

	public static RuleCover cover(IAttributeDefinition<?> attr, String child, String rule) {
        RuleCover cover = new RuleCover<>(rule, MapUtil.asMap(child, rule));
        cover.connect(attr);
        return cover;
	}

    @Override
    public Object connect(IAttributeDefinition<?> connectionEnd) {
        return super.connect(connectionEnd);
    }
    
    @Override
    protected boolean checkRule(String ruleName) {
        return ENV.get(Pool.class).get(ruleName) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object eval(String propertyPath) {
        return ENV.get(Pool.class).get(rules.get(propertyPath)).run(getContext());
    }

}
