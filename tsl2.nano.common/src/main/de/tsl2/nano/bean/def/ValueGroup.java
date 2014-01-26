/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 10, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.tsl2.nano.bean.BeanAttribute;


/**
 * description of a value group inside a bean. usable to create sub panel informations. you can define a full set of
 * child attribute names or only the first and the last one. normally, the order of attributes is constrained, so the
 * attributes between the first and the last are defined by order.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ValueGroup extends Presentable {
    /** serialVersionUID */
    private static final long serialVersionUID = -6371693652730976950L;
    
    List<String> attributes;

    /**
     * constructor to be serializable
     */
    protected ValueGroup() {
        this("");
    }

    public ValueGroup(String label, String... attributeNames) {
        this.label = label;
        this.attributes = new ArrayList<String>(Arrays.asList(attributeNames));
        type = TYPE_FORM;
    }

    /**
     * getAttributeNames
     * 
     * @return child attributes
     */
    public List<String> getAttributeNames() {
        return attributes;
    }
    
    /**
     * adds the given attribute
     * @param name attribute name
     */
    public void add(String name) {
        attributes.add(name);
    }
    
    /**
     * adds all bean attributes of the given attribute, that has to be an instance of another bean.
     * @param nestingBean nesting attribute bean
     */
    public void addNesting(BeanAttribute attributeNestingBean, Object instance) {
        Object value = attributeNestingBean.getValue(instance);
        if (value != null) {
            BeanDefinition<? extends Object> beanDefinition = BeanDefinition.getBeanDefinition(value.getClass());
            String bname = beanDefinition.getName();
            for (String attr : beanDefinition.getAttributeNames()) {
                attributes.add(bname + "." + attr);
            }
        }
    }
}
