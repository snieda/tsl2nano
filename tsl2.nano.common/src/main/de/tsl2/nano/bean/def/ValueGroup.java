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

import java.util.LinkedHashMap;

import org.simpleframework.xml.ElementMap;

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

    /** attributes of this group.attribute name to show details for - useful for sub-panels */
    @ElementMap(inline = true, attribute=true, entry = "attribute", key = "name", keyType = String.class, value = "details", valueType = Boolean.class, required=false)
    LinkedHashMap<String, Boolean> attributes;

    /**
     * constructor to be serializable
     */
    protected ValueGroup() {
        this("");
    }

    public ValueGroup(String label, String... attributeNames) {
        this.label = label;
        attributes = new LinkedHashMap<String, Boolean>();
        for (int i = 0; i < attributeNames.length; i++) {
            add(attributeNames[i]);
        }
        add("country", true);
        type = TYPE_FORM;
    }

    /**
     * setAttributeNames
     * 
     * @param attributeNames
     */
    public void setAttributes(LinkedHashMap<String,Boolean> attributes) {
        this.attributes = attributes;
    }

    /**
     * getAttributeNames
     * 
     * @return child attributes
     */
    public LinkedHashMap<String,Boolean> getAttributes() {
        return attributes;
    }

    /**
     * adds the given attribute
     * 
     * @param name attribute name
     */
    public void add(String name) {
        add(name, false);
    }

    /**
     * adds the given attribute
     * 
     * @param name attribute name
     */
    public void add(String name, boolean showDetails) {
        attributes.put(name, showDetails);
    }

    /**
     * isDetail
     * 
     * @param attributeName name to look for
     * @return true, if this attribute name should be showed with details (sub-panel).
     */
    public boolean isDetail(String attributeName) {
        return attributes.get(attributeName);
    }

    public void addNesting(BeanValue<?> attributeNestingBean) {
        addNesting(attributeNestingBean.getInstance(), attributeNestingBean);
    }

    /**
     * adds all bean attributes of the given attribute, that has to be an instance of another bean.
     * 
     * @param nestingBean nesting attribute bean
     */
    public void addNesting(Object instance, BeanAttribute attributeNestingBean) {
        Object value = attributeNestingBean.getValue(instance);
        if (value != null) {
            BeanDefinition<? extends Object> beanDefinition = BeanDefinition.getBeanDefinition(value.getClass());
            String bname = beanDefinition.getName();
            for (String attr : beanDefinition.getAttributeNames()) {
                attributes.put(bname + "." + attr, false);
            }
        }
    }
}
