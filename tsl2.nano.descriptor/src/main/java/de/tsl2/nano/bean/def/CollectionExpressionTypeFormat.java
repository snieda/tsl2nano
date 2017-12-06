/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 29.07.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Collection;

/**
 * 
 * @author Tom
 * @version $Revision$ 
 */
public class CollectionExpressionTypeFormat<T> extends ValueExpressionTypeFormat<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 5329461370983221661L;
    
    Collection<T> collectionInstance;
    

    /**
     * constructor to be serializable
     */
    protected  CollectionExpressionTypeFormat() {
        super();
    }

    /**
     * constructor
     * 
     * @param collectionMemberType type to format
     */
    public CollectionExpressionTypeFormat(Class<T> collectionMemberType) {
        super(collectionMemberType);
    }

    /**
     * constructor
     * @param collectionMemberType type to format
     * @param collectionInstance see {@link #setCollectionInstance(Collection)}
     */
    public CollectionExpressionTypeFormat(Class<T> collectionMemberType, Collection<T> collectionInstance) {
        this(collectionMemberType);
        this.collectionInstance = collectionInstance;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        return CollectionExpressionFormat.format0((T)obj, toAppendTo, pos, ve());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object parseObject(String source, ParsePosition pos) {
        return CollectionExpressionFormat.parseObject0(source, pos, ve(), collectionInstance);
    }

    /**
     * workaround to not lose jpa persistent collections with orphan-removal=true
     * @param tempInstance The tempInstance to set.
     */
    public void setCollectionInstance(Collection<T> collectionInstance) {
        this.collectionInstance = collectionInstance;
    }
}
