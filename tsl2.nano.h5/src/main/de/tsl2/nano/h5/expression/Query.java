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
import java.util.Set;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.IPRunnable;

/**
 * class to execute sql statements. not an inheritance of AbstractRunnable in cause of not having constraints in its
 * parameter map.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Query<RESULT> implements IPRunnable<RESULT, Map<String, Object>> {
    /** serialVersionUID */
    private static final long serialVersionUID = -9199837113877884921L;

    private static final String VAR_SQL = "[:]([\\w._-]+)";

    @Attribute
    String name;
    @Element(data = true)
    String operation;
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
        this.operation = query;
        this.nativeQuery = nativeQuery;
        this.parameter = parameter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public RESULT run(Map<String, Object> context, Object... extArgs) {
        //workaround for hibernate/query not allowing parameter with dots
        Set<String> pars = getParameter().keySet();
        String op = operation;
        Map<String, Object> cont = new HashMap<String, Object>();
        String sqlvar;
        for (String p : pars) {
            sqlvar = p.replace('.', 'X');
            cont.put(sqlvar, context.get(p));
            op = op.replace(":" + p, ":" + sqlvar);
        }

        //do the job
        return (RESULT) BeanContainer.instance().getBeansByQuery(op, nativeQuery, cont);
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * getQuery
     * 
     * @return query
     */
    public String getQuery() {
        return operation;
    }

    @Override
    public Map<String, ? extends Serializable> getParameter() {
        if (parameter == null) {
            parameter = new HashMap<String, Serializable>();
            String p;
            //we allow both: the named query syntax and the most java used ant-like-variables
            operation = operation.replace("${", ":").replace("}", "");
            StringBuilder q = new StringBuilder(operation);
            while ((!Util.isEmpty(p = StringUtil.extract(q, VAR_SQL, "")))) {
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
            String select = StringUtil.substring(operation.toLowerCase(), "select", "from");
            StringBuilder q = new StringBuilder(select);
            while ((!Util.isEmpty(p = StringUtil.extract(q, "(^|\\,)\\s*([^\\s,]+)(\\s+as\\s+([\\w]+))?", "")))) {
                columnNames.add(p.contains("as ") ? StringUtil.substring(p, "as ", null) : StringUtil.substring(p, ".",
                    null));
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
