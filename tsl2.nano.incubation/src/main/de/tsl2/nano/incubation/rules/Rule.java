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

import de.tsl2.nano.Environment;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.operation.NumericConditionOperator;
import de.tsl2.nano.util.operation.Operator;

/**
 * rule engine using operators (see {@link Operator}) for numeric and boolean values. rules can reference other rules
 * through using '§' + name.
 * <p/>
 * every rule defines parameters and an operation. the parameters will be checked against the arguments given on
 * {@link #execute(Map)}. additionally, there may be constraints for value ranges. Example:
 * 
 * <pre>
 * rule = new Rule&lt;BigDecimal&gt;(&quot;test&quot;, &quot;A ? (x1 + 1) : (x2 * 2)&quot;, MapUtil.asMap(&quot;A&quot;,
 *     ParType.BOOLEAN,
 *     &quot;x1&quot;,
 *     ParType.NUMBER,
 *     &quot;x2&quot;,
 *     ParType.NUMBER));
 * rule.addConstraint(&quot;x1&quot;, new Constraint(BigDecimal.class, new BigDecimal(0), new BigDecimal(1)));
 * BigDecimal r2 = rule.execute(MapUtil.asMap(&quot;A&quot;, false, &quot;x1&quot;, new BigDecimal(1), &quot;x2&quot;, new BigDecimal(2)));
 * 
 * or
 * 
 *         Rule<BigDecimal> ruleWithImport = new Rule<BigDecimal>("test-import", "A ? 1 + §test : (x2 * 3)", MapUtil.asMap("A",
 *             Boolean.class,
 *             "x1",
 *             BigDecimal.class,
 *             "x2",
 *             BigDecimal.class));
 * 
 * 
 * 
 * </pre>
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
        importSubRules();
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    protected void importSubRules() {
        RulePool pool = Environment.get(RulePool.class);
        String subRule;
        while ((subRule = StringUtil.extract(operation, "§\\w+")).length() > 0) {
            Rule<?> rule = pool.getRule(subRule.substring(1));
            if (rule == null)
                throw new IllegalArgumentException("Referenced rule " + subRule + " in " + this + " not found!");
            operation = operation.replaceAll(subRule, "(" + rule.operation + ")");
            parameter.putAll(rule.parameter);
            //TODO: what to do with sub rule constraints?
//            constraints.putAll(rule.constraints);
        }
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
        importSubRules();
    }

    @Override
    public String toString() {
        return name + "{" + operation + "}";
    }

    /**
     * addConstraint
     * 
     * @param parameterName parameter name to add the constraint for
     * @param constraint new constraint
     */
    public void addConstraint(String parameterName, Constraint<?> constraint) {
        constraints.put(parameterName, constraint);
    }
}
