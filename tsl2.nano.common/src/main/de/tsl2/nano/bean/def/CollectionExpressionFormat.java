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
import java.util.ArrayList;
import java.util.Collection;

import de.tsl2.nano.core.util.Util;

/**
 * Format for collections of entities/beans - not implementing it's own toString(). The format packs the given type into a
 * {@link BeanDefinition}, using the first attribute as output. See {@link ValueExpression} for further informations.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class CollectionExpressionFormat<T> extends ValueExpressionFormat<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -3040338597603039966L;
    static final String DIV = "; ";

    Collection<T> collectionInstance;
    
    /**
     * constructor to be serializable
     */
    protected  CollectionExpressionFormat() {
        super();
    }

    /**
     * constructor
     * 
     * @param collectionMemberType type to format
     */
    public CollectionExpressionFormat(Class<T> collectionMemberType) {
        super(collectionMemberType);
    }

    /**
     * constructor
     * @param collectionMemberType type to format
     * @param collectionInstance see {@link #setCollectionInstance(Collection)}
     */
    public CollectionExpressionFormat(Class<T> collectionMemberType, Collection<T> collectionInstance) {
        this(collectionMemberType);
        this.collectionInstance = collectionInstance;
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
        if (obj == null)
            return toAppendTo;
        Collection<T> c = (Collection<T>) obj;
        for (T item : c) {
            toAppendTo.append(ve.to(item) + DIV);
        }
        if (c.size() > 0)
            toAppendTo.delete(toAppendTo.length() - DIV.length(), toAppendTo.length());
        return toAppendTo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object parseObject(String source, ParsePosition pos) {
        return parseObject0(source, pos, ve(), collectionInstance);
    }

    /**
     * parseObject0
     * @param source
     * @param pos
     * @return
     */
    static <T> Collection<T> parseObject0(String source, ParsePosition pos, ValueExpression<T> ve, Collection<T> collectionInstance) {
        pos.setIndex(pos.getIndex() + 1);
        //don't create an empty collection if there are no values
        if (Util.isEmpty(source)) {
            if (collectionInstance != null)
                collectionInstance.clear();
            return collectionInstance;
        }
        String[] s = source.split(DIV);
        Collection<T> c = collectionInstance != null ? collectionInstance : new ArrayList<T>(s.length);
        c.clear();
        for (int i = 0; i < s.length; i++) {
            c.add(ve.from(s[i]));
        }
        return c;
    }

    /**
     * workaround to not lose jpa persistent collections with orphan-removal=true
     * @param tempInstance The tempInstance to set.
     */
    public void setCollectionInstance(Collection<T> collectionInstance) {
        this.collectionInstance = collectionInstance;
    }
}
