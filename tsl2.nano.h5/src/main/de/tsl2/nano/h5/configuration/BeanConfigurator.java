/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 07.01.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.configuration;

import static de.tsl2.nano.h5.HtmlUtil.ATTR_BGCOLOR;
import static de.tsl2.nano.h5.HtmlUtil.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.tsl2.nano.bean.BeanAttribute;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.Presentable;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.bean.def.ValueGroup;
import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.util.PrivateAccessor;
import de.tsl2.nano.util.Util;

/**
 * wrapper class to handle presentation of a bean-definition.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class BeanConfigurator<T> implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    private BeanDefinition<T> def;
    private PrivateAccessor<BeanDefinition<?>> defAccessor;
    
    /**
     * factory method to create a bean configurator for the given instance type.
     * @param instance to evaluate the type and {@link BeanDefinition} for.
     * @return new bean configurator instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <I  extends Serializable> Bean<BeanConfigurator<I>> create(Class<I> type) {
        //wrap the bean-def into a bean-configurator and pack it into an own bean
        BeanConfigurator<?> configurer = new BeanConfigurator(BeanDefinition.getBeanDefinition(type));
        Bean<?> configBean = Bean.getBean(configurer);
        configBean.getPresentable().setLayout((Serializable) MapUtil.asMap(ATTR_BGCOLOR, COLOR_LIGHT_GRAY));
        return (Bean<BeanConfigurator<I>>) configBean;
    }
    
    /**
     * constructor
     * 
     * @param def bean-def
     */
    protected BeanConfigurator(BeanDefinition<T> def) {
        super();
        this.def = def;
        defAccessor = new PrivateAccessor<BeanDefinition<?>>(def);
    }

    /**
     * @return Returns the attributeDefinitions.
     */
    public List<AttributeConfigurator> getAttributes() {
        //we know that there are attribute-defs inside!
        List<BeanAttribute> attributes = def.getAttributes();
        ArrayList<AttributeConfigurator> cattrs = new ArrayList<AttributeConfigurator>(attributes.size());
        for (BeanAttribute a : attributes) {
            cattrs.add(new AttributeConfigurator((AttributeDefinition<?>) a));
        }
        return cattrs;
    }

    /**
     * @param attributes The attributeDefinitions to set.
     */
    public void setAttributes(List<AttributeConfigurator> attributes) {
        defAccessor.call("getAttributeDefinitions", Map.class).clear();
        for (AttributeConfigurator cattr : attributes) {
            def.addAttribute(cattr.unwrap());
        }
    }

    /**
     * @return Returns the presentable.
     */
    public Presentable getPresentable() {
        return (Presentable) def.getPresentable();
    }

    /**
     * @param presentable The presentable to set.
     */
    public void setPresentable(Presentable presentable) {
        def.setPresentable(presentable);
    }


    /**
     * @return Returns the presentable.
     */
    @SuppressWarnings("unchecked")
    public Collection<ValueGroup> getValueGroups() {
        return defAccessor.member("valueGroups", Collection.class);
    }

    /**
     * @param valueGroups The presentable to set.
     */
    public void setValueGroups(Collection<ValueGroup> valueGroups) {
        defAccessor.set("valueGroups", valueGroups);
    }
    
    /**
     * @return Returns the name.
     */
    public String getName() {
        return def.getName();
    }

    /**
     * @return Returns the valueExpression.
     */
    public String getValueExpression() {
        return def.getValueExpression().getExpression();
    }

    /**
     * @param valueExpression The valueExpression to set.
     */
    public void setValueExpression(String valueExpression) {
        def.setValueExpression(new ValueExpression<T>(valueExpression, def.getClazz()));
    }

    /**
     * saves the current bean configuration.<br/>
     * this method will trigger the bean-framework to provide an action to be presented as button in a gui.
     * @return null
     */
    public Object actionSave() {
        def.saveDefinition();
        //return null to let the session-navigation return to the last element.
        return null;
    }
    
    @Override
    public String toString() {
        return Util.toString(getClass(), def);
    }
}
