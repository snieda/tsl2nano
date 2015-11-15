package de.tsl2.nano.incubation.specification.rules;

import java.util.LinkedHashMap;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.incubation.specification.AbstractRunnable;

/**
 * base rule with sub-rule importing
 * 
 * @param <T> result type
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public abstract class AbstractRule<T> extends AbstractRunnable<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = 5905121180488153205L;
    /** the rule is initialized when all sub-rules are imported. see {@link #importSubRules()} */
    protected boolean initialized;

    public AbstractRule() {
        super();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public AbstractRule(String name, String operation, LinkedHashMap parameter) {
        super(name, operation, parameter);
    }

    /**
     * importSubRules
     */
    protected void importSubRules() {
        RulePool pool = ENV.get(RulePool.class);
        String subRule;
        while ((subRule = StringUtil.extract(operation, "§\\w+")).length() > 0) {
            AbstractRule<?> rule = pool.get(subRule.substring(1));
            if (rule == null) {
                throw new IllegalArgumentException("Referenced rule " + subRule + " in " + this + " not found!");
            }
            operation = operation.replaceAll(subRule, "(" + rule.operation + ")");
            parameter.putAll(rule.parameter);
            //TODO: what to do with sub rule constraints?
            //            constraints.putAll(rule.constraints);
        }
        initialized = true;
    }

}