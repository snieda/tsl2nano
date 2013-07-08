package de.tsl2.nano.logictable;

import java.math.BigDecimal;
import java.util.Map;

import de.tsl2.nano.util.StringUtil;

/**
 * TODO: implement, see EquationSolver
 * @param <INPUT>
 * @param <OUTPUT>
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$ 
 */
public abstract class Operator<INPUT, OUTPUT> {
    Map<INPUT, OUTPUT> values;

    public Operator() {
        super();
    }

    public OUTPUT eval(INPUT expression) {
//        Object result;
//        //extract all terms
//        INPUT term = expression, t;
//        while (true) {
//            term = extract(expression, termExpression);
//            if (isEmpty(term))
//                break;
//            t = term.substring(1, term.length() - 1);
//            expression = extract(term, operate(t, values));
//        }
//        return operate(expression, values);
        return null;
    }

    abstract protected  OUTPUT operate(INPUT term, Map<String, OUTPUT> values);
    
    abstract boolean isEmpty(INPUT expression);
    
    abstract INPUT extract(INPUT expression, INPUT term);
}