package de.tsl2.nano.incubation.vnet.workflow;

import java.util.Map;

import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.operation.ConditionOperator;

/**
 * implementation of {@link Activity} using {@link ConditionOperator}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class Act<T> extends VActivity<String, T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -1031984359714782652L;

    protected transient ConditionOperator<Object> op;

    /**
     * constructor for xml-deserialization
     */
    public Act() {
    }

    public Act(String name, String condition, String expression, ComparableMap<CharSequence, Object> stateValues) {
        super(name, condition, expression);
        op = new ConditionOperator<Object>(stateValues);
    }

    @Override
    public T action() throws Exception {
        return (T) op.eval(expression);
    }

    @Override
    public boolean canActivate(Map parameter) {
        //sometimes we get a string out of eval, but sometimes a boolean. so we generalize it ;-)
        return (Boolean) Boolean.valueOf(StringUtil.toString(op.eval(condition, parameter)));
    }

    @Commit
    private void initDeserializing() {
        op = new ConditionOperator<Object>(new ComparableMap<CharSequence, Object>());
    }
}