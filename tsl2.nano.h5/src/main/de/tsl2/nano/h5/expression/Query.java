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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.core.log.LogFactory;
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

    private static final Log LOG = LogFactory.getLog(Query.class);
    
    /** full column expression for one column */
    private static final String SQL_COL = "(^|\\,)\\s*([^,]+)(\\s+as\\s+([\\w]+))?";
    /** sql parameter */
    private static final String SQL_PAR = "[:]([\\w._-]+)";
    /** sql function */
    private static final String SQL_FCT = "(\\w+)\\([^\\(\\)]+\\)";
    /** column name */
    private static final String SQL_NAM = ".*(\\w+)";
    /** string literal */
    private static final String SQL_LIT = "['][^']*[']";
    /** one or more concatenations */
    private static final String SQL_CON = "[^,]+[|]{2}";
    /** column path (if schema, etc. where given) */
    private static final String SQL_PAT = "(\\w+[.])+";

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
            parameter = new LinkedHashMap<String, Serializable>();
            String p;
            //we allow both: the named query syntax and the most java used ant-like-variables
            operation = operation.replace("${", ":").replace("}", "");
            StringBuilder q = new StringBuilder(operation);
            while ((!Util.isEmpty(p = StringUtil.extract(q, SQL_PAR, "")))) {
                parameter.put(p, null);
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
            //get the full selection header and eliminate literals, functions and concatenations
            String select = StringUtil.substring(operation, "select", "from");
            select = select.replaceAll(SQL_LIT, "");
            select = select.replaceAll(SQL_PAT, "");
            select = select.replaceAll(SQL_CON, "");
            //TODO: capturing group 1 is not used! why?
            select = select.replaceAll(SQL_FCT, "$1");
            StringBuilder q = new StringBuilder(select);
            while ((!Util.isEmpty(p = StringUtil.extract(q, SQL_COL, "", 0, 0)))) {
                columnNames.add(p.contains(" as ") ? StringUtil.substring(p, " as ", null).trim() : StringUtil.substring(p, ".",
                    null).trim());
            }
            if (LOG.isDebugEnabled())
                LOG.debug(Arrays.toString(columnNames.toArray()));
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
