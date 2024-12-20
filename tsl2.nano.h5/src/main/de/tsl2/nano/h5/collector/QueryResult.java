/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 26.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.collector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.Context;
import de.tsl2.nano.bean.def.ArrayValue;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanFinder;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.expression.Query;
import de.tsl2.nano.specification.Pool;

/**
 * Special {@link BeanCollector} to show the result of an SQL or JPAQL Query gotten by a defined query-name in a
 * {@link QueryPool}. The result columns are evaluated through {@link Query#getColumnNames()}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes", "serial" })
public class QueryResult<COLLECTIONTYPE extends Collection<T>, T> extends BeanCollector<COLLECTIONTYPE, T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Transient
    String queryName;
    transient Query<Collection<Object[]>> query;

    /**
     * constructor
     */
    protected QueryResult() {
        super();
    }

    /**
     * constructor
     * 
     * @param query
     */
    public QueryResult(String queryName) {
        super();
        this.queryName = queryName;
        this.name = queryName;
        initDeserialization();
    }

    public String getQueryName() {
        return queryName;
    }
    
    @Override
    public List<IAttribute> getAttributes(boolean readAndWriteAccess) {
        if (!allDefinitionsCached) {
            if (attributeDefinitions == null) {
                attributeDefinitions = new LinkedHashMap<String, IAttributeDefinition<?>>();
            }
            List<String> names = query.getColumnNames();
            IAttribute<?> attribute;
            int i = 0;
            for (String name : names) {
                attribute = new ArrayValue(name, i++);
                attributeDefinitions.put(name, new AttributeDefinition(attribute));
            }
            allDefinitionsCached = true;
        }
        return new ArrayList<IAttribute>(attributeDefinitions.values());
    }

    @Override
    public <B extends BeanDefinition<T>> B  onActivation(Map context) {
        super.onActivation(context);
        // TODO this may be before evaluating the new data :-(
        if (!Util.isEmpty(query.getColumnNames()) && query.getColumnNames().size() > 1 && !searchStatus.contains("<span"))
            searchStatus += "<span>" + Statistic.createGraph(getName(), query.getColumnNames(), (Collection<Object[]>) collection) + "</span>";
        return (B) this;
    }
    
    @Override
    public QueryResult<COLLECTIONTYPE, T> refreshed() {
        if (isStale())
            return new QueryResult(queryName);
        return this;
    }
    
    @Override
    @Commit
    protected void initDeserialization() {
        query = (Query<Collection<Object[]>>) ENV.get(Pool.class).get(queryName, Query.class);
        if (query == null)
            throw new IllegalStateException(this + " can't load query '" + queryName + "'");
        beanFinder = new BeanFinder() {
            @Override
            public Collection superGetData(Object fromFilter, Object toFilter, String...orderBy) {
                Map<String, Object> context = ConcurrentUtil.getCurrent(Context.class);
                if (context == null)
                	context = new HashMap<String, Object>();
                if (fromFilter != null && toFilter != null) {
                    String[] names = query.getParameter().keySet().toArray(new String[0]);
                    context.putAll(BeanUtil.toValueMap(fromFilter, "from", false, names));
                    context.putAll(BeanUtil.toValueMap(toFilter, "to", false, names));
                }
                return query.run(context);
            }
        };
        init(null, beanFinder, MODE_SEARCHABLE, null);
        isStaticCollection = false;
    }

    @Override
    public Map<String, Object> preAdjustContext(Map<String, Object> context) {
    	if (Util.isEmpty(query.getParameter()))
    		return null;
		Set<String> keySet = query.getParameter().keySet();
		for (String k : keySet) {
			if (context.containsKey(k)) {
				query.getParameter().put(k, (Serializable)context.get(k));
			}
		}
		return (Map)query.getParameter();
	}
    
    public static QueryResult createQueryResult(String title, String stmt) {
        Query<Object> query = new Query<>(title, stmt, true, null);
        Pool queryPool = ENV.get(Pool.class);
        queryPool.add(query);
        QueryResult<Collection<Object>, Object> qr = new QueryResult<>(query.getName());
        qr.getPresentable().setIcon("icons/barchart.png");
        qr.saveDefinition();
        return qr;
    }

}
