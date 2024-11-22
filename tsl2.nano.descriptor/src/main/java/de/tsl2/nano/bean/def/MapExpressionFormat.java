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
import java.text.ParsePosition;
import java.util.Map;

import de.tsl2.nano.core.util.MapUtil;

/**
 * Format for collections of entities/beans - not implementing it's own toString(). The format packs the given type into
 * a {@link BeanDefinition}, using the first attribute as output. See {@link ValueExpression} for further informations.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class MapExpressionFormat<T> extends ValueExpressionFormat<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -3040338597603039966L;
    static final String DIV = "; ";

    /**
     * constructor to be serializeable
     */
    public MapExpressionFormat() {
        super();
    }

    /**
     * constructor
     * 
     * @param mapValueType type to format
     */
    public MapExpressionFormat(Class<T> mapValueType) {
        super(mapValueType);
        //TODO: is previously done in super construction -> performance
        ve.setExpression("");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        pos.setEndIndex(pos.getBeginIndex() + 1);
        return toAppendTo.append(MapUtil.toJSon((Map)obj));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object parseObject(String source, ParsePosition pos) {
        pos.setIndex(pos.getIndex() + 1);
        return MapUtil.fromJSon(source);
    }

}
