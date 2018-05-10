/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 09.05.2018
 * 
 * Copyright: (c) Thomas Schneider 2018, all rights reserved
 */
package de.tsl2.nano.bean;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NEW TO BE USED BY {@link IGenericService} and {@link IBeanContainer} <p/>
 * Provides all parameters for any find-query
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class BeanFindParameters<T> {
    /** only needed, if no beans are given on 'values' */
    Class<T> beanType;
    int startIndex = 0;
    int maxResult = -1;
    /** list of column names with prefix '-' for DESC ordering and no prefix or '+' for ASC ordering */
    List<String> orderBy;
    Map<String, Object> hints;
    Class[] lazyRelations;
    /** needed on sql args or example-bean or two between-beans - then beantype can be examined */
    LinkedHashMap<String, Object> values;
    boolean caseSensitive;
    boolean nativeSql;
    boolean useLike;
    
    public BeanFindParameters() {
    }

    public BeanFindParameters(Class<T> beanType, Class[] lazyRelations) {
        super();
        this.beanType = beanType;
        this.lazyRelations = lazyRelations;
    }

    public BeanFindParameters(LinkedHashMap<String, Object> values,
            int startIndex,
            int maxResult,
            Class[] lazyRelations) {
        super();
        this.values = values;
        this.startIndex = startIndex;
        this.maxResult = maxResult;
        this.lazyRelations = lazyRelations;
    }

    public BeanFindParameters(Class<T> beanType, int startIndex, int maxResult, Class[] lazyRelations) {
        super();
        this.beanType = beanType;
        this.startIndex = startIndex;
        this.maxResult = maxResult;
        this.lazyRelations = lazyRelations;
    }

    public BeanFindParameters(Class<T> beanType,
            int startIndex,
            int maxResult,
            List<String> orderBy,
            Map<String, Object> hints,
            Class[] lazyRelations) {
        super();
        this.beanType = beanType;
        this.startIndex = startIndex;
        this.maxResult = maxResult;
        this.orderBy = orderBy;
        this.hints = hints;
        this.lazyRelations = lazyRelations;
    }
    
    public static LinkedHashMap<String, Object> values(Object... values) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>(values.length);
        int i = 0;
        for (Object o : values) {
            map.put(String.valueOf(i), o);
        }
        return map;
    }
    public Class<T> getBeanType() {
        if (beanType == null) {
            Object firstBean = getFirstValue();
            Object secondBean = getSecondValue();
            beanType = (Class<T>) (firstBean != null ? firstBean.getClass()
                : secondBean != null ? secondBean.getClass() : null);
        }
        return beanType;
    }
    public void setBeanType(Class<T> beanType) {
        this.beanType = beanType;
    }
    public int getStartIndex() {
        return startIndex;
    }
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }
    public int getMaxResult() {
        return maxResult;
    }
    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
    }
    public List<String> getOrderBy() {
        return orderBy;
    }
    public void setOrderBy(List<String> orderBy) {
        this.orderBy = orderBy;
    }
    public Map<String, Object> getHints() {
        return hints;
    }
    public void setHints(Map<String, Object> hints) {
        this.hints = hints;
    }
    public Class[] getLazyRelations() {
        return lazyRelations;
    }
    public void setLazyRelations(Class[] lazyRelations) {
        this.lazyRelations = lazyRelations;
    }
    
    public LinkedHashMap<String, Object> getValues() {
        return values;
    }

    public void setValues(LinkedHashMap<String, Object> values) {
        this.values = values;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isNativeSql() {
        return nativeSql;
    }

    public void setNativeSql(boolean nativeSql) {
        this.nativeSql = nativeSql;
    }

    public boolean isUseLike() {
        return useLike;
    }

    public void setUseLike(boolean useLike) {
        this.useLike = useLike;
    }

    /**
     * may be the example-bean or the first bean for between statement
     * @return
     */
    public Object getFirstValue() {
        Iterator it = values != null && values.size() > 0 ? values.values().iterator() : null;
        return it != null ? it.next() : null;
    }
    /**
     * may be the seond bean for between statement
     * @return
     */
    public Object getSecondValue() {
        Iterator it = values != null && values.size() > 1 ? values.values().iterator() : null;
        if (it != null) {
            it.next();
            return it.next();
        } else {
            return null;
        }
        
    }
    
    /**
     * values as object array to be usable as query arguments
     * @return
     */
    public Object[] getArguments() {
        return values != null ? values.values().toArray() : new Object[0];
    }
}
