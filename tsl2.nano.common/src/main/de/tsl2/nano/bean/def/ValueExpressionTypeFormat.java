/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 27.07.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import org.simpleframework.xml.core.Persist;

/**
 * extends the {@link ValueExpressionFormat} to use always the value expression of the {@link BeanDefinition} of the given type.
 * @author Tom
 * @version $Revision$
 */
public class ValueExpressionTypeFormat<T> extends ValueExpressionFormat<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 2751158110303013532L;
    Class<T> type;
    /**
     * constructor
     */
    public ValueExpressionTypeFormat() {
        super();
    }

    /**
     * constructor
     * @param type
     */
    public ValueExpressionTypeFormat(Class<T> type) {
        super(type);
        this.type = type;
    }

    @Override
    ValueExpression<T> ve() {
        if (ve == null)
            ve = BeanDefinition.getBeanDefinition(type).getValueExpression();
        return ve;
    }
    
    @Persist
    void initSerialization() {
        //don't save the value expression
        ve = null;
    }
}
