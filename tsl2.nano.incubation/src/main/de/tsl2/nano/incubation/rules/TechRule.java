/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 30.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.incubation.rules;

import java.util.Map;
import java.util.Set;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.util.operation.NumericConditionOperator;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class TechRule<T> {
    @Attribute
    String name;
    @ElementMap(entry="parameter", attribute=true, inline = true, keyType = String.class, key = "name", valueType = Class.class, value = "type")
    Map<String, Class<?>> parameter;
    transient NumericConditionOperator operator;
    @Element
    String operation;

    /**
     * constructor
     */
    public TechRule() {
        this(null, null, null);
    }

    /**
     * constructor
     * 
     * @param operation
     * @param parameter
     */
    public TechRule(String name, String operation, Map<String, Class<?>> parameter) {
        super();
        this.name = name;
        this.operation = operation;
        this.parameter = parameter;
        this.operator = new NumericConditionOperator();
    }

    @SuppressWarnings("unchecked")
    public T execute(Map<CharSequence, T> arguments) {
        checkArguments(arguments);
        operator.reset();
        return (T) operator.eval(operation, (Map<CharSequence, Object>) arguments);
    }

    /**
     * checks arguments against defined parameter
     * @param arguments to be checked
     */
    private void checkArguments(Map<CharSequence, T> arguments) {
        Set<CharSequence> keySet = arguments.keySet();
        Set<String> defs = parameter.keySet();
        for (CharSequence par : keySet) {
            Object arg = arguments.get(par);
            if (!defs.contains(par) || (arg != null && !parameter.get(par).isAssignableFrom(arg.getClass())))
                throw new IllegalArgumentException(par + "=" + arg);
        }
    }

    @Override
    public String toString() {
        return name + "{" + operation + "}";
    }
}
