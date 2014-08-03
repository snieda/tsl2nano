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
import static de.tsl2.nano.h5.HtmlUtil.COLOR_LIGHT_GRAY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.CollectionExpressionTypeFormat;
import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.bean.def.Presentable;
import de.tsl2.nano.bean.def.ValueColumn;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.bean.def.ValueGroup;
import de.tsl2.nano.collection.Entry;
import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.Html5Presentable;
import de.tsl2.nano.util.PrivateAccessor;

/**
 * wrapper class to handle presentation of a bean-definition. at a time, only one BeanConfigurator is active. this
 * instance will be registered to the environment to be usable as something like a singleton from outside.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class BeanConfigurator<T> implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    BeanDefinition<T> def;
    private PrivateAccessor<BeanDefinition<?>> defAccessor;

    /**
     * factory method to create a bean configurator for the given instance type.
     * 
     * @param instance to evaluate the type and {@link BeanDefinition} for.
     * @return new bean configurator instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <I extends Serializable> Bean<BeanConfigurator<I>> create(Class<I> type) {
        //wrap the bean-def into a bean-configurator and pack it into an own bean
        BeanConfigurator<?> configurer = new BeanConfigurator(BeanDefinition.getBeanDefinition(type));
        //register it to be used by creating new AttributeConfigurators 
        Environment.addService(BeanConfigurator.class, configurer);

        //define the presentation
        Bean<?> configBean = Bean.getBean(configurer);

        if (configBean.isDefault()) {
            Serializable layout = (Serializable) MapUtil.asMap(ATTR_BGCOLOR, COLOR_LIGHT_GRAY);

            BeanDefinition<Html5Presentable> configPres = BeanDefinition.getBeanDefinition(Html5Presentable.class);
            configPres.setAttributeFilter("label", "description", "icon", "type", "style", "visible", "width",
                "height", "layout",
                "layoutConstraints");
            configPres.getPresentable().setLayout(layout);
//            configPres.saveDefinition();

            BeanDefinition<ValueGroup> configValueGroup = BeanDefinition.getBeanDefinition(ValueGroup.class);
            configValueGroup.setAttributeFilter("label", "description", "icon", "type", "style", "width", "height",
                "layout",
                "layoutConstraints", "attributes");
            configValueGroup.getPresentable().setLayout(layout);
//            configValueGroup.saveDefinition();

            BeanDefinition<ValueColumn> configColDef = BeanDefinition.getBeanDefinition(ValueColumn.class);
            configColDef.setAttributeFilter("name", "description", "index", "sortIndex", "sortUpDirection", "format",
                "width");
            configColDef.getPresentable().setLayout(layout);
//            configColDef.saveDefinition();

            BeanDefinition<Constraint> configConstraint = BeanDefinition.getBeanDefinition(Constraint.class);
            configConstraint.setAttributeFilter("type", "minimum", "maximum", "format", "length", "scale", "precision",
                "nullable");
            configConstraint.getPresentable().setLayout(layout);
//            configConstraint.saveDefinition();

            defineAction(layout);

            BeanDefinition<Entry> configEntry = BeanDefinition.getBeanDefinition(Entry.class);
            configEntry.setAttributeFilter("key", "value");
            configEntry.getPresentable().setLayout(layout);
//            configEntry.saveDefinition();

            BeanDefinition<AttributeConfigurator> configAttr =
                    BeanDefinition.getBeanDefinition(AttributeConfigurator.class);
                configAttr.setAttributeFilter("name", "description", "type",
                    "constraint"/*, "length", "format", "min", "max"*/,
                    "presentable", "columnDefinition", "declaration");
                configAttr.getPresentable().setLayout(layout);
                configAttr.setValueExpression(new ValueExpression("{name}", AttributeConfigurator.class));
//                configAttr.saveDefinition();
            
            configBean.setAttributeFilter("name", "valueExpression", "presentable", "valueGroups", "attributes");
            configBean.getPresentable().setLayout(layout);
            ((CollectionExpressionTypeFormat) configBean.getAttribute("attributes").getFormat()).getValueExpression()
                .setExpression("{name}");
//          configBean.saveDefinition();

        }
        return (Bean<BeanConfigurator<I>>) configBean;
    }

    /**
     * defineAction
     * 
     * @param layout
     */
    @SuppressWarnings("rawtypes")
    public static void defineAction(Serializable layout) {
        BeanDefinition<CommonAction> configAction = BeanDefinition.getBeanDefinition(CommonAction.class);
        configAction.setAttributeFilter("id", "shortDescription", "longDescription", "keyStroke", "enabled", "default",
            "imagePath");
        configAction.setIdAttribute("id");
        configAction.setValueExpression(new ValueExpression<CommonAction>("{shortDescription}", CommonAction.class));
        if (layout != null)
            configAction.getPresentable().setLayout(layout);
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
        List<IAttribute> attributes = def.getAttributes();
        ArrayList<AttributeConfigurator> cattrs = new ArrayList<AttributeConfigurator>(attributes.size());
        for (IAttribute<?> a : attributes) {
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
     * 
     * @return null
     */
    public Object actionSave() {
        defAccessor.set("isdefault", false);
        def.saveDefinition();
        Environment.removeService(BeanConfigurator.class);

        /*
         * refresh all beans
         */
        Bean.clearCache();

        //return null to let the session-navigation return to the last element.
        return null;
    }

    public Object actionReset() {
        defAccessor.set("isdefault", true);
        def.deleteDefinition();
        Environment.removeService(BeanConfigurator.class);
        /*
         * refresh all beans
         */
        Bean.clearCache();

        return null;
    }

    @Override
    public int hashCode() {
        return def.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof BeanConfigurator ? def.equals(((BeanConfigurator) obj).def) : false;
    }

    @Override
    public String toString() {
        return Util.toString(getClass(), def);
    }
}
