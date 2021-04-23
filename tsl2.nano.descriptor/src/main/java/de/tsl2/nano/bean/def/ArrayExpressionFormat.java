/*
 * created by: Thomas Schneider
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.lang.reflect.Array;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Arrays;

import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.Util;

/**
 * Format for array fields of entities/beans - not implementing it's own toString(). The format packs the given type into a
 * {@link BeanDefinition}, using the first attribute as output. See {@link ValueExpression} for further informations.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ArrayExpressionFormat<T> extends ValueExpressionFormat<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -3040338597603039966L;
    static final String DIV = ",";

    T[] arrayInstance;
    
    /**
     * constructor to be serializable
     */
    protected  ArrayExpressionFormat() {
        super();
    }

    /**
     * constructor
     * 
     * @param arrayComponentType type to format
     */
    public ArrayExpressionFormat(Class<T> arrayComponentType) {
        super(arrayComponentType);
    }

    /**
     * constructor
     * @param arrayComponentType type to format
     * @param collectionInstance see {@link #setCollectionInstance(Collection)}
     */
    public ArrayExpressionFormat(Class<T> arrayComponentType, T[] instance) {
        this(arrayComponentType);
        this.arrayInstance = instance;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        return format0((T)obj, toAppendTo, pos, ve());
    }

    /**
     * format0
     * @param obj
     * @param toAppendTo
     * @param pos
     * @return
     */
    @SuppressWarnings("unchecked")
    static <T> StringBuffer format0(T obj, StringBuffer toAppendTo, FieldPosition pos, ValueExpression<T> ve) {
        pos.setEndIndex(pos.getBeginIndex() + 1);
        if (obj == null) {
            return toAppendTo;
        }
        toAppendTo.append(Arrays.toString((Object[])obj));
        return toAppendTo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object parseObject(String source, ParsePosition pos) {
        return parseObject0(source, pos, ve(), arrayInstance);
    }

    /**
     * parseObject0
     * @param source
     * @param pos
     * @return
     */
    static <T> T[] parseObject0(String source, ParsePosition pos, ValueExpression<T> ve, T[] arrayInstance) {
        pos.setIndex(pos.getIndex() + 1);
        //don't create an empty collection if there are no values
        if (Util.isEmpty(source)) {
            if (arrayInstance != null) {
                Arrays.fill(arrayInstance, null);
            }
            return arrayInstance;
        }
        if (arrayInstance == null)
        	return (T[]) MapUtil.asArray(ve.type.getComponentType(), source);
        //TODO: check different DIVs and fillings
        String[] s = source.split(DIV);
        Arrays.fill(arrayInstance, null);
        for (int i = 0; i < s.length; i++) {
            Array.set(arrayInstance, i, ve.from(s[i]));
        }
        return arrayInstance;
    }
}
