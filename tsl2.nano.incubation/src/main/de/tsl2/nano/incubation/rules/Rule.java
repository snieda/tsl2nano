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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.util.operation.NumericConditionOperator;
import de.tsl2.nano.util.operation.Operator;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Rule<T> {
    @Attribute
    String name;
    @ElementMap(entry = "parameter", attribute = true, inline = true, keyType = String.class, key = "name", valueType = ParType.class, value = "type")
    Map<String, ParType> parameter;
    @ElementMap(entry = "constraint", attribute = true, inline = true, keyType = String.class, key = "name", valueType = Constraint.class, value = "constraint")
    Map<String, Constraint<?>> constraints;
    transient NumericConditionOperator operator;
    @Element
    String operation;

    /**
     * constructor
     */
    public Rule() {
        this(null, null, null);
    }

    /**
     * constructor
     * 
     * @param operation
     * @param parameter
     */
    public Rule(String name, String operation, Map<String, ParType> parameter) {
        super();
        this.name = name;
        this.operation = operation;
        this.parameter = parameter;
        this.operator = new NumericConditionOperator();
        createConstraints();
    }

    @SuppressWarnings("unchecked")
    public T execute(Map<CharSequence, T> arguments) {
        checkArguments(arguments);
        operator.reset();
        T result = (T) operator.eval(operation, (Map<CharSequence, Object>) arguments);
        checkConstraint(Operator.KEY_RESULT, result);
        return result;
    }

    /**
     * checks arguments against defined parameter
     * 
     * @param arguments to be checked
     */
    private void checkArguments(Map<CharSequence, T> arguments) {
        Set<CharSequence> keySet = arguments.keySet();
        Set<String> defs = parameter.keySet();
        for (CharSequence par : keySet) {
            Object arg = arguments.get(par);
            if (!defs.contains(par))
                throw new IllegalArgumentException(par + "=" + arg);
            else
                checkConstraint(par, arg);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void createConstraints() {
        if (constraints == null)
            constraints = new HashMap<String, Constraint<?>>();
        Set<String> pars = parameter.keySet();
        for (String p : pars) {
            Object o = parameter.get(p);
            Class<?> cls = o instanceof Class ? (Class<?>) o : transform((ParType) o);
            Constraint constraint = constraints.get(p);
            if (constraint == null) {
                constraint = new Constraint(cls);
            } else {
                constraint.type = cls;
            }
        }
    }

    protected Class<?> transform(ParType t) {
        switch (t) {
        case TEXT:
            return String.class;
        case BOOLEAN:
            return Boolean.class;
        case NUMBER:
            return BigDecimal.class;
        }
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void checkConstraint(CharSequence par, Object arg) {
        Constraint constraint = constraints.get(par);
        if (constraint != null)
            constraint.check((Comparable) arg);
    }

    @Commit
    private void initDeserializing() {
        createConstraints();
    }

    @Override
    public String toString() {
        return name + "{" + operation + "}";
    }

    /**
     * addConstraint
     * @param parameterName parameter name to add the constraint for
     * @param constraint new constraint
     */
    public void addConstraint(String parameterName, Constraint<?> constraint) {
        constraints.put(parameterName, constraint);
    }
}
