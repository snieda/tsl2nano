/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 20, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanFindParameters;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.util.operation.IRange;
import de.tsl2.nano.util.operation.Range;

/**
 * @see IBeanFinder
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public class BeanFinder<T, F> implements IBeanFinder<T, F>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -1270111762126889953L;
    Class<T> type;
    /** optional bean definition to present a search filter mask */
    Bean<IRange<F>> rangeBean;
    /** if no {@link #rangeBean} can be created */
    boolean noRangeBean;

    transient String lastExpression = null;

    /** optional detail bean definition to present a selected bean in a detailed mask */
    Bean<T> detailBean;
    transient int currentStartIndex = 0;
    int maxResultCount = ENV.get("collector.service.maxresult", 100);

    private static final Log LOG = LogFactory.getLog(BeanFinder.class);

    /**
     * constructor to be serializable
     */
    protected BeanFinder() {
        super();
        type = (Class<T>) Serializable.class;
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
        type = (Class<T>) Serializable.class;
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
    public Collection<T> getData(F fromFilter, F toFilter, String...orderBy) {
        lastExpression = null;
        return superGetData(fromFilter, toFilter, orderBy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<T> getData() {
        if (lastExpression != null) {
            return getData(lastExpression);
        } else {
            Bean<IRange<F>> range = getFilterRange();
            F from = range != null ? range.getInstance().getFrom() : null;
            F to = range != null ? range.getInstance().getTo() : null;
            return getData(from, to);
        }
    }

    @Override
    public Collection<T> getData(String valueExpression) {
        lastExpression = valueExpression;
        return null;
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
        if (currentStartIndex >= getMaxResultCount()) {
            currentStartIndex -= getMaxResultCount();
        }
        return getData();
    }

    /**
     * to avoid cycle calling, we have the 'interface' method {@link #getData(Object, Object)} which may be overridden
     * (inline in views or dialogs) and the functional method {@link #superGetData(Object, Object)}. don't call or
     * override this method - it should only be called by the framework.
     */
    @SuppressWarnings("rawtypes")
    public <S> Collection<T> superGetData(S fromFilter, S toFilter, String...orderBy) {
    	BeanFindParameters pars = new BeanFindParameters(getType(), currentStartIndex, getMaxResultCount());
    	if (!Util.isEmpty(orderBy))
    		pars.setOrderBy(Arrays.asList(orderBy));
        if (fromFilter == null || toFilter == null) {
            List<T> result = new LinkedList<T>(BeanContainer.instance().getBeans(pars));
//            Collections.sort(result, BeanDefinition.getBeanDefinition(type).getValueExpression().getComparator());
            return result;
        } else {
            List<T> result = new LinkedList(BeanContainer.instance().getBeansBetween(fromFilter, toFilter, pars));
//            Collections.sort(result, BeanDefinition.getBeanDefinition(type).getValueExpression().getComparator());
            return result;
        }
    }

    @Override
    public Bean<IRange<F>> getFilterRange() {
        if (rangeBean == null && !noRangeBean) {
            try {
                if (type.isInterface() || !BeanClass.hasDefaultConstructor(type)) {
                    LOG.warn("BeanFinder wont provide a range filter for type " + type);
                    noRangeBean = true;
                } else {
                    F from = (F) BeanClass.createInstance(type);
                    setNoConstraint(from);
                    Range.setPrimitiveMinValues(from);
                    F to = BeanUtil.clone(from);
                    setNoConstraint(to);
                    Range.setPrimitiveMaxValues(to);
                    rangeBean = new Bean<IRange<F>>(new Range<F>(from, to));
                }
            } catch (Exception ex) {
                noRangeBean = true;
                //ok, we don't provide a range filter
                LOG.warn("BeanFinder wont provide a range filter:" + ex.toString());
            }
        }
        return rangeBean;
    }

    protected static void setNoConstraint(Object instance) {
    	Bean<Object> bean = Bean.getBean(instance);
        List<IAttribute> attributes = bean.getAttributes();
        for (IAttribute a : attributes) {
        	BeanValue attr = (BeanValue) a;
        	Constraint c = Bean.copy(attr.getConstraint(), new Constraint());
//        	c.setType(Serializable.class);
        	c.setFormat(null);
        	attr.setConstraint(c);
        }
    }

    /**
     * the filter range bean is used as presentation information for search masks.
     * 
     * @param rangeBean The rangeBean to set.
     */
    public void setFilterRange(Bean<IRange<F>> rangeBean) {
        this.rangeBean = rangeBean;
        noRangeBean = false;
        lastExpression = null;
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
        this.maxResultCount = maxresult;
    }
    
    @Override
    public void reset() {
        rangeBean = null;
        lastExpression = null;
        currentStartIndex = 0;
        detailBean = null;
    }
}
