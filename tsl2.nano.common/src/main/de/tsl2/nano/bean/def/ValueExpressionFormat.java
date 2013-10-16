/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 5, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

/**
 * Format for entities/beans - not implementing it's own toString(). The format packs the given type into a
 * {@link BeanDefinition}, using the first attribute as output. See {@link ValueExpression} for further informations.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ValueExpressionFormat<T> extends Format {
    /** serialVersionUID */
    private static final long serialVersionUID = -3040338597603039966L;
    ValueExpression<T> ve;

    /**
     * constructor to be serializable
     */
    protected ValueExpressionFormat() {
        super();
    }

    /**
     * constructor
     * 
     * @param type type to format
     */
    public ValueExpressionFormat(Class<T> type) {
        super();
        ve = BeanDefinition.getBeanDefinition(type).getValueExpression();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        pos.setEndIndex(pos.getBeginIndex() + 1);
        return toAppendTo.append(ve.to((T) obj));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object parseObject(String source, ParsePosition pos) {
        pos.setIndex(pos.getIndex() + 1);
        return ve.from(source);
    }

}
