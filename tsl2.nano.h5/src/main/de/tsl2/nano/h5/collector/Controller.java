/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 26.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.collector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.simpleframework.xml.Transient;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Special {@link BeanCollector} to show a list of beans providing their actions. the defined bean should have some
 * actions to change attributes in an easy way - like de- or increasing a value. The best use-case is to show a set of
 * buttons with pictures on a touch-screen.
 * <p/>
 * Through the bean-definition of {@link #beanName}, a special bean, holding the actions, can start a selected action
 * through {@link #doAction(String)}.<p/>
 * The super class Compositor tries to build actions given by baseAttribute and targetAttribute. 
 * The second possibility for creating actions is to set an itemProvider through {@link #setItemProvider(Increaser)}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Controller<COLLECTIONTYPE extends Collection<T>, T> extends Compositor<COLLECTIONTYPE, T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public static final String PREFIX_CTRLACTION = "CTRLROW(";
    public static final String POSTFIX_CTRLACTION = ")";

    /** the special bean-implementation holding all desired actions */
    @Transient
    String beanName;

    @Transient
    Increaser itemProvider;
    
    transient List<IAttribute> attributes;

    /**
     * constructor
     */
    protected Controller() {
        super();
    }

    public Controller(String beanName) {
        this(beanName, null, null, null, null);
    }
    
    /**
     * constructor
     * 
     * @param beanType
     * @param workingMode
     */
    public Controller(String beanName, String baseType, String baseAttribute, String targetAttribute, String iconAttribute) {
        this((BeanDefinition<T>) BeanDefinition.getBeanDefinition(beanName), (BeanDefinition<T>) (baseType != null ? BeanDefinition.getBeanDefinition(baseType) : null), baseAttribute, targetAttribute, iconAttribute);
    }

    /**
     * constructor
     * 
     * @param beanType
     * @param workingMode
     */
    public Controller(BeanDefinition<T> beanDef, BeanDefinition<T> baseType, String baseAttribute, String targetAttribute, String iconAttribute) {
        this(beanDef.getDeclaringClass(), baseType != null ? baseType.getDeclaringClass() : null, baseAttribute, targetAttribute, iconAttribute);
    }

    public Controller(Class<T> beanDef, Class<T> baseType, String baseAttribute, String targetAttribute, String iconAttribute) {
        super(beanDef, baseType, baseAttribute, targetAttribute, iconAttribute);
        this.name = "Controller (" + (baseType != null ? baseType.getSimpleName() + "-" : "") + beanDef.getSimpleName() + ")";
        beanName = name;
    }

    /**
     * gets the defined bean (see {@link #beanName} for the given instance.
     * 
     * @param instance
     * @return bean holding instance
     */
    public Bean<T> getBean(T instance) {
        Bean<T> bean = BeanUtil.copy((Bean<T>) Bean.getBean(beanName));
        bean.setAddSaveAction(false);
        bean.setActions(getActions());
        //filter collector actions
        for (Iterator it = bean.getActions().iterator(); it.hasNext();) {
            IAction a = (IAction) it.next();
            //TODO: internal name convention - is there a better way?
            if (!a.getId().startsWith("controller"))
                it.remove();
        }
        bean.setValueExpression(getValueExpression());
        bean.setInstance(instance);
        return bean;
    }

    /**
     * extracts the bean-instance and the action id and starts it.
     * 
     * @param actionIdWithRowNumber the row-number has to be one-based!
     * @param session 
     * @return result of given action
     */
    public Object doAction(String actionIdWithRowNumber, Map context) {
        String strRow = StringUtil.substring(actionIdWithRowNumber, PREFIX_CTRLACTION, POSTFIX_CTRLACTION);
        Number row = NumberUtil.extractNumber(strRow);
        ArrayList<T> list = new ArrayList<T>(getCurrentData());
        String id = StringUtil.substring(actionIdWithRowNumber, POSTFIX_CTRLACTION, null);
        IAction<?> action = getBean(list.get(row.intValue() - 1)).getAction(id);
        action.setParameter(context);
        return action.activate();
    }

    @Override
    public boolean isVirtual() {
        return true;
    }
    
    @Override
    public ValueExpression<T> getValueExpression() {
        if (valueExpression == null) {
            //use the expression of the standard definition
            valueExpression = BeanDefinition.getBeanDefinition(getType()).getValueExpression();
        }
        return valueExpression;
    }

    @Override
    public Compositor<COLLECTIONTYPE, T> refreshed() {
        if (isStale())
            return new Controller(clazz, parentType, baseAttribute, targetAttribute, iconAttribute);
        return this;
    }
    

    @Override
    public <B extends BeanDefinition<T>> B onActivation(Map context) {
        if (itemProvider != null && itemProvider.getCount() > getCurrentData().size()) {
                //TODO: create only missing items
                getCurrentData().addAll(provideTransientData(context));
        }
        return super.onActivation(context);
    }

    private Collection<? extends T> provideTransientData(Map context) {
        T item = createItem(null);
        if (context != null)
            fillContext(item, context.values().toArray());
        return itemProvider.createItems(item, context);
    }

    /**
     * @return Returns the itemProvider.
     */
    public Increaser getItemProvider() {
        return itemProvider;
    }

    /**
     * @param itemProvider The itemProvider to set.
     */
    public Controller setItemProvider(Increaser itemProvider) {
        this.itemProvider = itemProvider;
        setValueExpression(new ValueExpression<T>("{" + itemProvider.getName() + "}"));
        return this;
    }

    public static String createActionName(int tabIndex, String id) {
        return PREFIX_CTRLACTION + tabIndex + POSTFIX_CTRLACTION + id;
    }
}
