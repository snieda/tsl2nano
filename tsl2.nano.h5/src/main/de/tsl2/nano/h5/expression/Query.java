/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 25.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.expression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.Util;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Query<RESULT> implements IPRunnable<RESULT, Map<String, Object>> {
    /** serialVersionUID */
    private static final long serialVersionUID = -9199837113877884921L;
    @Attribute
    String name;
    @Element(data = true)
    String query;
    @Attribute
    boolean nativeQuery;
    @ElementMap(entry = "parameter", attribute = true, inline = true, keyType = String.class, key = "name", value = "type", required = false)
    Map<String, ? extends Serializable> parameter;
    private transient ArrayList<String> columnNames;

    /**
     * constructor
     */
    public Query() {
    }

    /**
     * constructor
     * 
     * @param name
     * @param query
     * @param parameter
     */
    public Query(String name, String query, boolean nativeQuery, Map<String, ? extends Serializable> parameter) {
        super();
        this.name = name;
        this.query = query;
        this.nativeQuery = nativeQuery;
        this.parameter = parameter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public RESULT run(Map<String, Object> context, Object... extArgs) {
        return (RESULT) BeanContainer.instance().getBeansByQuery(query, nativeQuery, context);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, ? extends Serializable> getParameter() {
        if (parameter == null) {
            parameter = new HashMap<String, Serializable>();
            String p;
            StringBuilder q = new StringBuilder(query);
            while ((!Util.isEmpty(p = StringUtil.extract(q, "[:]\\w+", "")))) {
                parameter.put(p.substring(1), null);
            }
        }
        return parameter;
    }

    /**
     * evaluates the query's column names through the select-columns having an 'as' expression.
     * <p/>
     * e.g. 'select name, type as Type from...<br/>
     * will have the column 'Type'.
     * 
     * @return
     */
    public List<String> getColumnNames() {
        if (columnNames == null) {
            columnNames = new ArrayList<String>();
            String p;
            StringBuilder q = new StringBuilder(query.toLowerCase());
            while ((!Util.isEmpty(p = StringUtil.extract(q, "as \\w+", "")))) {
                columnNames.add(p.substring(3));
            }
        }
        return columnNames;
    }

    @Override
    public Map<String, Object> checkedArguments(Map<String, Object> args, boolean strict) {
        // TODO implement (see Rule) - using select-statement-columns
        return args;
    }

    @Override
    public String toString() {
        return Util.toString(getClass(), name);
    }
}
