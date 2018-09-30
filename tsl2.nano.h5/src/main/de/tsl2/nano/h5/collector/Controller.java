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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
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
 * Through the bean-definition of {@link #name}, a special bean, holding the actions, can start a selected action
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
    private static final String PREFIX_CONTROLLER = "controller";
    /** the special bean-implementation holding all desired actions */

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
        this.name = createName(beanDef, baseType);
    }

    public String createName(Class<?> beanDef, Class<?> baseType) {
        return createBeanDefName(beanDef, baseType);
    }

    public static String createBeanDefName(Class<?> beanDef, Class<?> baseType) {
        return createBeanDefName("Controller", beanDef, baseType);
    }

    /**
     * gets the defined bean (see {@link #name} for the given instance.
     * 
     * @param instance
     * @return bean holding instance
     */
    public Bean<T> getBean(T instance) {
        Bean<T> bean = BeanUtil.copy((Bean<T>) Bean.getBean(name));
        bean.setAddSaveAction(false);
        //if controlling from baseType (annotation on baseType), we want exactly one creation-action
        if (getDeclaringClass().equals(parentType)) {
            Collection<IAction> compActions = super.getActions();
            String compositorActionId = createCompositorActionId(instance);
            for (IAction a : compActions) {
                if (a.getId().equals(compositorActionId)) {
                    bean.setActions(Arrays.asList(a));
                    break;
                }
            }
        } else {//on each row get all compositor actions
            bean.setActions(super.getActions());
            //filter collector actions
            for (Iterator it = bean.getActions().iterator(); it.hasNext();) {
                IAction a = (IAction) it.next();
                //TODO: internal name convention - is there a better way?
                if (!a.getId().startsWith(PREFIX_CONTROLLER))
                    it.remove();
            }
        }
        bean.setValueExpression(getValueExpression());
        bean.setInstance(instance);
        return bean;
    }

    @Override
    public Collection<IAction> getActions() {
        return new LinkedList<>(); //don't show the actions in the action panel again!
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
        assert row.intValue() > 0 : "row must be one-based!";
        ArrayList<T> list = new ArrayList<T>(getCurrentData());
        assert row.intValue() <= list.size() : "row=" + row + " (one-based) is outside of list size=" + list.size();
        String id = StringUtil.substring(actionIdWithRowNumber, POSTFIX_CTRLACTION, null);
        IAction<?> action = getBean(list.get(row.intValue() - 1)).getAction(id);
        assert action != null : "no action with id=" + id + " found on (one-based) row " + row + "! see: " + StringUtil.toString(list, 120);
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

    public static String createActionName(int row, String id) {
        assert row > 0 : "row must be one-based!";
        return PREFIX_CTRLACTION + row + POSTFIX_CTRLACTION + id;
    }
}
