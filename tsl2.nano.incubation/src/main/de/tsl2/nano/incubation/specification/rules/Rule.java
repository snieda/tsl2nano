/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 30.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.incubation.specification.rules;

import java.util.LinkedHashMap;
import java.util.Map;

import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.Environment;
import de.tsl2.nano.incubation.specification.AbstractRunnable;
import de.tsl2.nano.incubation.specification.ParType;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.operation.NumericConditionOperator;
import de.tsl2.nano.util.operation.Operator;

/**
 * rule engine using operators (see {@link Operator}) for numeric and boolean values. rules can reference other rules
 * through using '�' + name.
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
 *         Rule<BigDecimal> ruleWithImport = new Rule<BigDecimal>("test-import", "A ? 1 + �test : (x2 * 3)", MapUtil.asMap("A",
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
public class Rule<T> extends AbstractRunnable<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 8557708958880364123L;
    public static final String KEY_RESULT = Operator.KEY_RESULT;

    transient NumericConditionOperator operator;
    /** the rule is initialized when all sub-rules are imported. see {@link #importSubRules()} */
    boolean initialized;
    
    /**
     * constructor only to be used by deserializing
     */
    public Rule() {
    }

    /**
     * constructor
     * 
     * @param operation
     * @param parameter
     */
    public Rule(String name, String operation, LinkedHashMap<String, ParType> parameter) {
        super(name, operation, parameter);
    }

    /**
     * importSubRules
     */
    void importSubRules() {
        RulePool pool = Environment.get(RulePool.class);
        String subRule;
        while ((subRule = StringUtil.extract(operation, "�\\w+")).length() > 0) {
            Rule<?> rule = pool.get(subRule.substring(1));
            if (rule == null)
                throw new IllegalArgumentException("Referenced rule " + subRule + " in " + this + " not found!");
            operation = operation.replaceAll(subRule, "(" + rule.operation + ")");
            parameter.putAll(rule.parameter);
            //TODO: what to do with sub rule constraints?
//            constraints.putAll(rule.constraints);
        }
        initialized = true;
    }

    /**
     * executes the given rule, checking the given arguments.
     * @param arguments rule input
     * @return evaluation result
     */
    @SuppressWarnings("unchecked")
    @Override
    public T run(Map<String, Object> arguments, Object... extArgs) {
        if (!initialized)
            importSubRules();
        arguments = checkedArguments(arguments, Environment.get("application.mode.strict", false));
        operator.reset();
        //in generics it is not possible to cast from Map(String,?) to Map(CharSequence, ?)
        Object a = arguments;
        T result = (T) operator.eval((CharSequence)operation, (Map<CharSequence, Object>) a);
        checkConstraint(Operator.KEY_RESULT, result);
        return result;
    }

    @Commit
    protected void initDeserializing() {
        super.initDeserializing();
        this.operator = new NumericConditionOperator();
        importSubRules();
    }
}