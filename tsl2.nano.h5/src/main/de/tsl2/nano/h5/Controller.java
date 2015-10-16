/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 26.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.simpleframework.xml.Transient;

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
 * through {@link #doAction(String)}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Controller<COLLECTIONTYPE extends Collection<T>, T> extends BeanCollector<COLLECTIONTYPE, T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public static final String PREFIX_CTRLACTION = "CTRLROW(";
    public static final String POSTFIX_CTRLACTION = ")";

    /** the special bean-implementation holding all desired actions */
    @Transient
    String beanName;

    transient List<IAttribute> attributes;

    /**
     * constructor
     */
    protected Controller() {
        super();
    }

    /**
     * constructor
     * 
     * @param beanType
     * @param workingMode
     */
    public Controller(String beanName, int workingMode) {
        this((BeanDefinition<T>) BeanDefinition.getBeanDefinition(beanName), workingMode);
    }

    /**
     * constructor
     * 
     * @param beanType
     * @param workingMode
     */
    public Controller(BeanDefinition<T> beanDef, int workingMode) {
        super(beanDef.getDeclaringClass(), workingMode);
        beanName = beanDef.getName();
    }

//    @Override
//    public List<IAttribute> getAttributes(boolean readAndWriteAccess) {
//        if (attributeDefinitions == null) {
//            attributeDefinitions = new LinkedHashMap<String, IAttributeDefinition<?>>();
//            attributeDefinitions.put("nix", new AttributeDefinition<T>(new VAttribute("nix")));
//        }
//        return new ArrayList<IAttribute>((Collection<? extends IAttribute>) attributeDefinitions.values().iterator().next());
//    }
//    

    /**
     * gets the defined bean (see {@link #beanName} for the given instance.
     * 
     * @param instance
     * @return bean holding instance
     */
    public Bean<T> getBean(T instance) {
        Bean<T> bean = (Bean<T>) Bean.getBean(beanName);
        bean.setInstance(instance);
        return bean;
    }

    /**
     * extracts the bean-instance and the action id and starts it.
     * 
     * @param actionIdWithRowNumber the row-number has to be one-based!
     * @return result of given action
     */
    public Object doAction(String actionIdWithRowNumber) {
        String strRow = StringUtil.substring(actionIdWithRowNumber, PREFIX_CTRLACTION, POSTFIX_CTRLACTION);
        Number row = NumberUtil.extractNumber(strRow);
        ArrayList<T> list = new ArrayList<T>(getCurrentData());
        String id = StringUtil.substring(actionIdWithRowNumber, POSTFIX_CTRLACTION, null);
        return getBean(list.get(row.intValue() - 1)).getAction(id).activate();
    }

    @Override
    public ValueExpression<T> getValueExpression() {
        if (valueExpression == null) {
            //use the expression of the standard definition
            valueExpression = BeanDefinition.getBeanDefinition(getType()).getValueExpression();
        }
        return valueExpression;
    }
}
