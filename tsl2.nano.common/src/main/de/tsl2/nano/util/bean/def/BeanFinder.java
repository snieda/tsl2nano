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

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.Environment;
import de.tsl2.nano.util.bean.BeanClass;
import de.tsl2.nano.util.bean.BeanContainer;
import de.tsl2.nano.util.bean.BeanUtil;
import de.tsl2.nano.util.operation.IRange;
import de.tsl2.nano.util.operation.Range;

/**
 * @see IBeanFinder
 * @author Thomas Schneider
 * @version $Revision$
 */
public class BeanFinder<T, F> implements IBeanFinder<T, F>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -1270111762126889953L;
    Class<T> type;
    /** optional bean definition to present a search filter mask */
    Bean<IRange<F>> rangeBean;
    /** optional detail bean definition to present a selected bean in a detailed mask */
    Bean<T> detailBean;
    transient int currentStartIndex = 0;
    int maxResultCount = Environment.get("service.maxresult", 100);
    
    private static final Log LOG = LogFactory.getLog(BeanFinder.class);

    /**
     * constructor to be serializable
     */
    protected BeanFinder() {
        super();
    }

    /**
     * constructor
     * 
     * @param type
     */
    public BeanFinder(Class<T> type) {
        super();
        this.type = type;
    }

    /**
     * constructor
     * 
     * @param rangeBean
     */
    public BeanFinder(Bean<IRange<F>> rangeBean) {
        super();
        this.rangeBean = rangeBean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<T> getData(F fromFilter, F toFilter) {
        return superGetData(fromFilter, toFilter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<T> getData() {
        Bean<IRange<F>> range = getFilterRange();
        F from = range != null ? range.getInstance().getFrom() : null;
        F to = range != null ? range.getInstance().getTo() : null;
        return getData(from, to);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<T> next() {
        currentStartIndex += getMaxResultCount();
        return getData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<T> previous() {
        if (currentStartIndex >= getMaxResultCount())
            currentStartIndex -= getMaxResultCount();
        return getData();
    }

    /**
     * to avoid cycle calling, we have the 'interface' method {@link #getData(Object, Object)} which may be overridden
     * (inline in views or dialogs) and the functional method {@link #superGetData(Object, Object)}. don't call or
     * override this method - it should only be called by the framework.
     */
    public <S> Collection<T> superGetData(S fromFilter, S toFilter) {
        if (fromFilter == null || toFilter == null) {
            return new LinkedList<T>(BeanContainer.instance().getBeans(getType(), getMaxResultCount()));
        } else {
            //TODO: add currentStartIndex if available on getBeansBetween
            return new LinkedList(BeanContainer.instance().getBeansBetween(fromFilter, toFilter, getMaxResultCount()));
        }
    }

    @Override
    public Bean<IRange<F>> getFilterRange() {
        if (rangeBean == null) {
            try {
                F from = (F) BeanClass.createInstance(type);
                F to = BeanUtil.clone(from);
                rangeBean = new Bean<IRange<F>>(new Range<F>(from, to));
            } catch (Exception ex) {
                //ok, we don't provide a range filter
                LOG.info("BeanFinder wont provide a range filter:" + ex.toString());
            }
        }
        return rangeBean;
    }

    /**
     * the filter range bean is used as presentation information for search masks.
     * 
     * @param rangeBean The rangeBean to set.
     */
    public void setFilterRange(Bean<IRange<F>> rangeBean) {
        this.rangeBean = rangeBean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object wrapToDetailBean(T bean) {
        return detailBean != null ? detailBean.setInstance(bean) : bean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T unwrapToSelectableBean(Object obj) {
        return obj instanceof Bean ? ((Bean<T>) obj).getInstance() : (T) obj;
    }

    /**
     * the detail bean will be used on creating a detail presentation of a selected bean. used by
     * {@link #wrapToDetailBean(Object)}.
     * 
     * @param detailBean The detailBean to set.
     */
    public void setDetailBean(Bean<T> detailBean) {
        this.detailBean = detailBean;
    }

    
    /**
     * @return Returns the maxResult.
     */
    @Override
    public int getMaxResultCount() {
        return maxResultCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaxResultCount(int maxresult) {
    }
}
