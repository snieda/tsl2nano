package de.tsl2.nano.specification.rules;

import java.util.LinkedHashMap;
import java.util.Map;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.specification.AbstractRunnable;
import de.tsl2.nano.specification.ParType;
import de.tsl2.nano.specification.Pool;

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

    public static final char PREFIX = '§';

    public AbstractRule() {
        super();
    }

    public AbstractRule(String name, String operation, LinkedHashMap<String, ParType> parameter) {
        super(name, operation, parameter);
    }

    @Override
    public String prefix() {
        return String.valueOf(PREFIX);
    }
    @Override
    public T run(Map<String, Object> context, Object... extArgs) {
    	throw new UnsupportedOperationException();
    }
    
    /**
     * importSubRules
     */
    protected void importSubRules() {
        Pool pool = ENV.get(Pool.class);
        String subRule;
        while ((subRule = StringUtil.extract(getOperation(), prefix() + "\\w+")).length() > 0) {
            AbstractRule<?> rule = (AbstractRule<?>) pool.get(subRule);
            if (rule == null) {
                throw new IllegalArgumentException("Referenced rule " + subRule + " in " + this + " not found!");
            }
            setOperation(getOperation().replaceAll(subRule, "(" + rule.getOperation() + ")"));
            parameter.putAll(rule.parameter);
            //TODO: what to do with sub rule constraints?
            //            constraints.putAll(rule.constraints);
        }
        initialized = true;
    }

}
