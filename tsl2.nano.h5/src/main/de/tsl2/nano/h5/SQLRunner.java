/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 25.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5;

import java.io.Serializable;
import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.execution.IPRunnable;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class SQLRunner<RESULT> implements IPRunnable<RESULT, Map<String, Object>> {
    @Attribute
    String name;
    String query;
    @Attribute
    boolean nativeQuery;
    @ElementMap(entry = "parameter", attribute = true, inline = true, keyType = String.class, key = "name", value = "type")
    Map<String, ? extends Serializable> parameter;
    
    /**
     * constructor
     */
    public SQLRunner() {
    }

    /**
     * constructor
     * @param name
     * @param query
     * @param parameter
     */
    public SQLRunner(String name, String query, boolean nativeQuery, Map<String, ? extends Serializable> parameter) {
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
        return parameter;
    }

    @Override
    public void checkArguments(Map<String, Object> args) {
        // TODO implement (see Rule) - using select-statement-columns
    }

}
