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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Format for collections of entities/beans - not implementing it's own toString(). The format packs the given type into a
 * {@link BeanDefinition}, using the first attribute as output. See {@link ValueExpression} for further informations.
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        pos.setEndIndex(pos.getBeginIndex() + 1);
        Map<?, T> m = (Map<?, T>) obj;
        Set<?> keySet = m.keySet();
        for (Object key : keySet) {
            toAppendTo.append(key + "=" + ve.to(m.get(key)) + DIV);
        }
        if (m.size() > 0)
            toAppendTo.delete(toAppendTo.length() - DIV.length(), toAppendTo.length());
        return toAppendTo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object parseObject(String source, ParsePosition pos) {
        pos.setIndex(pos.getIndex() + 1);
        String[] s = source.split(DIV);
        Map<String, T> m = new HashMap<String, T>(s.length);
        for (int i = 0; i < s.length; i++) {
            String kv[] = s[i].split("=");
            m.put(kv[0], ve.from(kv[1]));
        }
        return m;
    }

}