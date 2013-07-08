/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 20, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

import java.util.Collection;

import de.tsl2.nano.util.bean.ValueHolder;

/**
 * 
 * @author Thomas Schneider
 * @version $Revision$ 
 */
public class SelectionProvider<T> extends ValueHolder<Collection<T>> implements ISelectionProvider<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = -969325041213343728L;

    /**
     * constructor
     * @param selection
     */
    public SelectionProvider(Collection<T> selection) {
        super(selection);
    }

    @Override
    public T getFirstElement() {
        return isEmpty() ? null : getValue().iterator().next();
    }

    @Override
    public boolean isEmpty() {
        return getValue() == null || getValue().isEmpty();
    }
}
