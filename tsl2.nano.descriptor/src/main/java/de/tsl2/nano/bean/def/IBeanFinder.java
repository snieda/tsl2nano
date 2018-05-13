/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 19, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.util.Collection;

import de.tsl2.nano.util.operation.IRange;

/**
 * Evaluate< a constraint list of beans - to be shown f.e. in a search-dialog
 * 
 * @param <T> bean type to get in result list
 * @param <F> bean filter type to constrain the result list
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IBeanFinder<T, F> {
    /** defines the bean type */
    Class<T> getType();

    /**
     * Determine the data for the bean-list-view. Implement this method to evaluate your data to be shown - but don't
     * return null, you should return at least an empty list. Please overwrite the method {@link #getLastResult()}, too.
     * 
     * @param fromFilter from filter object
     * @param toFilter to filter object
     * @return list of beans between from and to
     */
    Collection<T> getData(F fromFilter, F toFilter, String...orderBy);

    /**
     * delegates to {@link #getData(Object, Object)} using {@link #getFilterRange()}
     * 
     * @return result of {@link #getData(Object, Object)}
     */
    Collection<T> getData();

    /**
     * evaluates the data through a given expression using the beans {@link ValueExpression}.
     * @param valueExpression
     * @return collection of data
     */
    Collection<T> getData(String valueExpression);
    
    /**
     * delegates to {@link #getData(Object, Object)} using {@link #getFilterRange()} - for the next {@link #getMaxResultCount()} items.
     * 
     * @return result of {@link #getData(Object, Object)}
     */
    Collection<T> next();

    /**
     * delegates to {@link #getData(Object, Object)} using {@link #getFilterRange()} - for the previous {@link #getMaxResultCount()} items.
     * 
     * @return result of {@link #getData(Object, Object)}
     */
    Collection<T> previous();

    /**
     * provides the max result count of beans to get
     * 
     * @return max result count
     */
    int getMaxResultCount();

    /**
     * provides the beans (packed into the range bean), for minimum and maximum filter - see
     * {@link #getData(Object, Object)}. the range bean doesn't define
     * 
     * @return
     */
    Bean<IRange<F>> getFilterRange();

    /**
     * optional wrap a bean. implement it to wrap your selected beans - you must overwrite the
     * {@link #unwrapBean(Object)} method , too. usable to show a specific presenter on open/new.
     * 
     * @param bean bean to wrap
     * @return wrapped bean
     */
    Object wrapToDetailBean(T bean);

    /**
     * optional unwrap a bean. implement it unwrap selected beans. overwrite this only if you overwrite the
     * {@link #wrapBean(Object)} method. usable to show a specific presenter on open/new.
     * 
     * @param obj obj to unwrap
     * @return origin bean
     */
    T unwrapToSelectableBean(Object obj);
    
    /**
     * setMaxResult
     * @param maxresult
     */
    void setMaxResultCount(int maxresult);
    
    /**
     * resets the from and to ranges
     */
    void reset();
}
