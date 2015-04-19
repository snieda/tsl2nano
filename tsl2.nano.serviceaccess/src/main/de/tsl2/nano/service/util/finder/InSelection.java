/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Oct 10, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.service.util.finder;

import static de.tsl2.nano.service.util.ServiceUtil.addInSelection;

import java.util.Arrays;
import java.util.Collection;

import de.tsl2.nano.core.util.Util;

/**
 * finder to load a collection of beans having attribute values that are contained in the given selection.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class InSelection<T> extends AbstractFinder<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 5440541728057978007L;

    /** attribute to have one value of selection */
    String attribute;

    /**
     * constructor
     * @param resultType
     * @param attribute
     * @param selection
     * @param relationsToLoad
     */
    public InSelection(Class<T> resultType, String attribute, Collection<?> selection, Class<Object>... relationsToLoad) {
        super(resultType, relationsToLoad);
        par = Arrays.asList(Util.untyped(selection));
        this.attribute = attribute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    StringBuffer createQuery(StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations) {
        return addInSelection(currentQuery, getAndClause(), getSubSelectSubst(), attribute, (Collection<?>)par.get(0));
    }

}
