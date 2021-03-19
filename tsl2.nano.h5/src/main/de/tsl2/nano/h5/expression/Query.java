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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.incubation.specification.AbstractRunnable;
import de.tsl2.nano.incubation.specification.IPrefixed;
import de.tsl2.nano.service.util.ServiceUtil;

/**
 * class to execute sql statements. not an inheritance of AbstractRunnable in cause of not having constraints in its
 * parameter map.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Query<RESULT> implements IPRunnable<RESULT, Map<String, Object>>, IPrefixed {
    /** serialVersionUID */
    private static final long serialVersionUID = -9199837113877884921L;

    private static final Log LOG = LogFactory.getLog(Query.class);
    
    /** full column expression for one column */
    private static final String SQL_COL = "(^|\\,)\\s*([^,]+)(\\s+[Aa][Ss]\\s+(\\w+))?";
    /** sql parameter */
    private static final String SQL_PAR = "[:]([\\w._-]+)";
    /** sql function */
    private static final String SQL_FCT = "(\\w+)\\([^\\(\\)]+\\)";
    /** column name */
    private static final String SQL_NAM = ".*(\\w+)";
    /** string literal */
    private static final String SQL_LIT = "['\"][^'\"]*['\"]";
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
    Map<String, Serializable> parameter;
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
    public Query(String name, String query, boolean nativeQuery, Map<String, Serializable> parameter) {
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
        String op = operation.replaceAll("--.*", "");

        Map<String, Object> args = checkedArguments(context, ENV.get("app.mode.strict", false));
        String sqlvar;
        for (String p : pars) {
            sqlvar = p.replace('.', 'X');
            if (parameter.get(p) != null) {
            	args.put(sqlvar, parameter.get(p));
            	if (!context.containsKey(p))
            		context.put(p, parameter.get(p));
            } else if (context.containsKey(p))
            	args.put(sqlvar, context.get(p));
            op = op.replace(":" + p, ":" + sqlvar);
        }
        
        //do the job
        return (RESULT) (ServiceUtil.isExcecutionStatement(op) ? BeanContainer.instance().executeStmt(op, nativeQuery, args.values().toArray()) 
            : BeanContainer.instance().getBeansByQuery(op, nativeQuery, args));
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
		this.name = name;
	}
    
    public String getQuery() {
        return operation;
    }

    public void setQuery(String query) {
    	operation = query;
    	columnNames = null;
    	parameter = null;
	}
    
    @Override
    public Map<String, Serializable> getParameter() {
        if (parameter == null) {
            parameter = new LinkedHashMap<String, Serializable>();
            String p;
            //remove temporary all comments
            String op = operation.replaceAll("--.*", "");
            //we allow both: the named query syntax and the most java used ant-like-variables
            op = operation.replace("${", ":").replace("}", "");
            //redundant: persist the cleaned sql-query-parameter
            operation = operation.replace("${", ":").replace("}", "");
            StringBuilder q = new StringBuilder(op);
            while ((!Util.isEmpty(p = StringUtil.extract(q, SQL_PAR, "")))) {
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
            //get the full selection header and eliminate literals, functions and concatenations
            String select = StringUtil.substring(operation, "select ", "from ");
            select = select.replaceAll(SQL_LIT, "");
            select = select.replaceAll(SQL_PAT, "");
            select = select.replaceAll(SQL_CON, "");
            //TODO: capturing group 1 is not used! why?
            select = select.replaceAll(SQL_FCT, "$1");
            StringBuilder q = new StringBuilder(select);
            while ((!Util.isEmpty(p = StringUtil.extract(q, SQL_COL, "", 0, 0)))) {
                String as = StringUtil.extract(p, "\\s+[Aa][Ss]\\s+");
                columnNames.add(!Util.isEmpty(as) && p.contains(as) 
                	? StringUtil.substring(p, as, null).trim() 
                		: StringUtil.substring(StringUtil.substring(p, ",", null), ".", null, true).trim());
            }
            if (LOG.isDebugEnabled())
                LOG.debug(Arrays.toString(columnNames.toArray()));
        }
        return columnNames;
    }

    @Override
    public Map<String, Object> checkedArguments(Map<String, Object> arguments, boolean strict) {
        boolean asSequence = AbstractRunnable.asSequence(arguments);
        Map<String, Object> args = new LinkedHashMap<String, Object>();
        if (parameter.isEmpty()) //only defined parameters are allowed - otherwise we get an exception by jpa query implementation
        	return args;
        Set<String> keySet = arguments.keySet();
        Set<String> parameterKeys = parameter.keySet();
        Iterator<Object> valueIt = arguments.values().iterator();
        Object arg = null;
        int i = 0;
        for (String par : parameterKeys) {
            if (asSequence) {
                par = "" + (++i);
                arg = valueIt.next();
            } else {
                if (!keySet.contains(par)) {
                    if (strict)
                        throw new IllegalArgumentException(par);
                } else {
                    arg = arguments.get(par);
                }
            }
            args.put(par, arg);
        }
        return args;
    }

    @Override
    public String prefix() {
    	return "?";
    }
    
    @Override
    public String toString() {
        return Util.toString(getClass(), name);
    }
}
