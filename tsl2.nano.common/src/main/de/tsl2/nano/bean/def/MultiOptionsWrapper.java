/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 28, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.util.Collection;

import de.tsl2.nano.bean.IValueAccess;

/**
 * see {@link MultiOptionsWrapper} but works on real enums.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class MultiOptionsWrapper<E> extends OptionsWrapper<E> {
    /** valueset, should be a set */
    Collection<E> valueset;

    /**
     * constructor
     * 
     * @param ovalue must hold a collection as value
     * @param enumType enum class
     */
    @SuppressWarnings("unchecked")
    public MultiOptionsWrapper(IValueAccess<E> ovalue, Class<E> enumType) {
        super(ovalue, enumType);
        valueset = (Collection<E>) ovalue.getValue();
    }

    /**
     * constructor
     * 
     * @param ovalue must hold a collection as value
     * @param enumType enum class
     */
    @SuppressWarnings("unchecked")
    public MultiOptionsWrapper(IValueAccess<E> ovalue, E[] enumConstants) {
        super(ovalue, enumConstants);
        valueset = (Collection<E>) ovalue.getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasValue(int index) {
        return valueset.contains(getEnumConstants()[index]);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void setValue(int index, boolean value) {
        if (value) {
            valueset.add(getEnumConstants()[index]);
        } else {
            valueset.remove(getEnumConstants()[index]);
        }
    }

}
