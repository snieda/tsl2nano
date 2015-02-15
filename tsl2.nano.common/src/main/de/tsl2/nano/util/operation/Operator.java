package de.tsl2.nano.util.operation;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.util.parser.Parser;

/**
 * Base class to execute any operation with terms having two operands and one operator. it is like a primitive parser
 * using regular expressions. may be used to implement equation-solvers etc.
 * <p/>
 * TODO: support for one-operand-operation (the second has a default value. e.g.: -10 = 0 - 10.
 * <p/>
 * Use:<br/>
 * create an expression of type INPUT. create an Operator instance holding a converter (INPUT <==> OUTPUT) and provide
 * values contained in your expression.
 * <p/>
 * Example:
 * 
 * <pre>
 * if your operator is a numeric operator with INPUT type String and OUTPUT type Number:
 * values.put("x1", 1);
 * value.put("x2, 2);
 * operator.eval("(x1 + x2) + 5") ==> (Number)8.
 * </pre>
 * 
 * @param <INPUT> type of input to extract operations from
 * @param <OUTPUT> type of output as result.
 * @author Thomas Schneider
 * @version $Revision$
 */
@Default(value = DefaultType.FIELD, required = false)
public abstract class Operator<INPUT, OUTPUT> extends Parser<INPUT> {
    private static final Log LOG = LogFactory.getLog(Operator.class);

    /**
     * technical member while it's not possible to create an array out of a generic type. the
     * BeanUtil.getGenericType(Class) can't solve that problem, too. the annotation element has required = false to be
     * set to null in {@link #initSerialization()}.
     */
    @Element(required = false)
    Class<? extends INPUT> inputType;
    /** syntax defining special expression parts. required=false to be set to null in {@link #initSerialization()} */
    @ElementMap(inline = true, required = false)
    Map<String, INPUT> syntax;
    /** a map containing any values. values found by this solver must be of right type */
    private transient Map<INPUT, OUTPUT> values;
    /** converter to convert an operand to a result type and vice versa */
    transient IConverter<INPUT, OUTPUT> converter;
    /** holds all operations to be resolvable */
//    @ElementMap(inline = true, entry = "operation")
    transient Map<INPUT, IAction<OUTPUT>> operationDefs;

    /** if true, this class will serialize all informations, including syntax etc. */
    transient boolean explizitXml = false;

    public static final String KEY_BEGIN = "begin";
    public static final String KEY_END = "end";
    public static final String KEY_TERM = "term";
    public static final String KEY_TERM_ENCLOSED = "term.enclosed";
    public static final String KEY_BETWEEN = "between";
    public static final String KEY_CONCAT = "concat";
    public static final String KEY_OPERATION = "operation";
    public static final String KEY_HIGH_OPERATION = "high_operation";
    public static final String KEY_OPERAND = "operand";
    public static final String KEY_EMPTY = "empty";
//    public static final String KEY_DEFAULT_OPERAND = "default.operand";
//    public static final String KEY_DEFAULT_OPERATOR = "default.operator";
    public static final String KEY_RESULT = "result";

    public Operator() {
        this(null, null, null);
    }

    /**
     * constructor
     * 
     * @param inputClass class of INPUT. technical workaround - see {@link #inputType}.
     * @param converter see {@link #converter}
     * @param values see {@link #values}
     */
    public Operator(Class<? extends INPUT> inputClass, IConverter<INPUT, OUTPUT> converter, Map<INPUT, OUTPUT> values) {
        super();
        //the default type string may result in classcast exceptions
        this.inputType = (Class<? extends INPUT>) (inputClass != null ? inputClass : String.class);
        this.converter = converter;
        this.values = values != null ? values : new HashMap<INPUT, OUTPUT>();
        syntax = createSyntax();
        createOperations();
        createTermSyntax();
    }

    /**
     * default implementation. please override
     * 
     * @return map containing needed {@link #syntax}. see {@link #syntax(String)}.
     */
    protected abstract Map<String, INPUT> createSyntax();

    /**
     * syntax
     * 
     * @param key syntax key to find
     * @return syntax element
     */
    protected final INPUT syntax(String key) {
        return syntax.get(key);
    }

    /**
     * define all possible operations. see {@link #operationDefs}. should set value for {@link #KEY_OPERATION} in
     * {@link #syntax}, too!
     */
    protected abstract void createOperations();

    /**
     * defines the syntax of a term with given set of operators
     */
    protected abstract void createTermSyntax();

    /**
     * helper to define an operation-definition
     * 
     * @param operator operator
     * @param operation operation to be executed
     */
    protected void addOperation(INPUT operator, IAction<OUTPUT> operation) {
        operationDefs.put(operator, operation);
    }

    /**
     * used to do a break if result already available
     * 
     * @return true, if result already available
     */
    protected boolean resultEstablished() {
        return values.containsKey(KEY_RESULT);
    }

    /**
     * getValues
     * 
     * @return values
     */
    public Map<INPUT, OUTPUT> getValues() {
        return values;
    }

    /**
     * resets stored values - to do the next operation
     */
    public void reset() {
        getValues().clear();
    }

    //    public OUTPUT eval(INPUT expression) {
//        return eval(new StringBuilder(expression.toString()));
//    }

    /**
     * delegates to {@link #eval(Object)} filling the given values to {@link #values}.
     */
    public OUTPUT eval(INPUT expression, Map<INPUT, OUTPUT> v) {
        if (values == null)
            values = new HashMap<INPUT, OUTPUT>();
        values.putAll(v);
        return eval(expression);
    }

    /**
     * eval
     * 
     * @param expression
     * @return
     */
    public OUTPUT eval(INPUT expression) {
        try {
            //create an operable expression like a sequence
            expression = wrap(expression);
            //enclose operations in brackets
            expression = encloseInBrackets(expression);

            if (LOG.isDebugEnabled()) {
                String log = "\n-------------------------------------------------------------------------------\n"
                    + "  OPERATION: " + expression + "\n"
                    + "  PARAMETER: " + values + "\n";
                LOG.debug(log);
            }
            //extract all terms
            INPUT term;
            INPUT t;
            while (!values.containsKey(KEY_RESULT)) {
                term = extract(expression, syntax(KEY_TERM_ENCLOSED));
                if (isEmpty(term)) {
                    term = extract(expression, syntax(KEY_TERM));
                    if (isEmpty(term)) {
                        break;
                    }
                }
                t = extract(term, syntax(KEY_TERM));
                boolean finish = unwrap(expression).equals(term);
                replace(expression, term, converter.from(operate(wrap(t), values)));
                if (finish)
                    break;
            }

            OUTPUT result;
            if (resultEstablished()) {
                result = getValue(KEY_RESULT);
            } else {
                INPUT operand = extract(expression, syntax.get(KEY_OPERAND));
                result = getValue(operand);
                if (isEmpty(expression))
                    expression = operand;
                result = result != null ? result : converter.to(trim(expression));
            }
            if (LOG.isDebugEnabled()) {
                String log =
                    "  RESULT: " + result + "\n"
                        + "-------------------------------------------------------------------------------\n";
                LOG.debug(log);
            }
            return result;
        } catch (Exception ex) {
            String msg = Util.toString(this.getClass(), "expression=" + expression, "value=", values);
            throw new IllegalStateException("Error on evaluation of operation '" + msg + "'", ex);
        }
    }

    /**
     * gets a value - usable to fill values from value map. overwrite this method to do more...
     * 
     * @param key values key
     * @return value
     */
    protected OUTPUT getValue(Object key) {
        return values.get(key);
    }

    /**
     * addValue
     * 
     * @param key
     * @param value
     */
    protected void addValue(INPUT key, OUTPUT value) {
        getValues().put(key, value);
    }

//    protected abstract INPUT encloseInBrackets(INPUT expression);

    /**
     * searches for not enclosed terms with a {@link #KEY_HIGH_OPERATION} to enclose it. Needed for math-operations like
     * multiply, divide and pow
     * 
     * @param expression to inspect
     * @return expression with enclosed high-operations
     */
    protected INPUT encloseInBrackets(INPUT expression) {
        INPUT term = wrap(syntax.get(KEY_TERM));
        replace(term, syntax.get(KEY_OPERATION), syntax.get(KEY_HIGH_OPERATION));
        INPUT notEnclosed = concat(term, syntax.get(KEY_OPERATION), syntax.get(KEY_OPERAND));
        INPUT highOp = syntax.get(KEY_HIGH_OPERATION);
        INPUT toEnclose, t;
        while (!isEmpty(toEnclose = extract(expression, notEnclosed))) {
            t = extract(toEnclose, term);
            if (extract(t, highOp) != null)
                replace(expression, t, concat(syntax.get(KEY_BEGIN), t, syntax.get(KEY_END)));
        }
        return expression;
    }

    /**
     * main function to extract operation elements and to execute the operation.
     * 
     * @param term term to analyse and execute
     * @param values pre-defined values
     * @return result of operation
     */
    protected OUTPUT operate(INPUT term, Map<INPUT, OUTPUT> values) {
        if (resultEstablished()) {
            replace(term, term, syntax(KEY_EMPTY));
            return getValue(KEY_RESULT);
        }

        Operation op = new Operation(term);

        /*
         * the value map may contain any values - but the found value must have the right type!
         */
        OUTPUT n1 = getValue(op.o1());
        n1 = n1 != null || isEmpty(op.o2()) ? n1 : newOperand(op.o1());
        OUTPUT n2 = getValue(op.o2());
        n2 = n2 != null || isEmpty(op.o2()) ? n2 : newOperand(op.o2());

        OUTPUT result;
        IAction<OUTPUT> operation = operationDefs.get(op.op());
        if (operation != null) {
            operation.setParameter(new Object[] { n1, n2 });
            result = operation.activate();
        } else {
            throw new IllegalArgumentException(term.toString() + " (operation should match:"
                + syntax(KEY_OPERATION)
                + ")");
        }

        //TODO: this should be deprecated because we should extract only pure terms!
        if (!isEmpty(term)) {
            /*
             * technical workaround, see #inputType
             * it's not possible to provide a generic array as dynamic parameter
             * like: term = concat(converter.from(result), term);
             */
            term = concat(converter.from(result), term);

            result = operate(wrap(term), values);
        }
        return result;
    }

    protected OUTPUT newOperand(INPUT expr) {
        return (OUTPUT) converter.to(expr);
    }

    @Persist
    void initSerialization() {
        if (!explizitXml) {
            //TODO: how to tell simple-xml on runtime not to include this vars
//            this.getClass().getField("syntax").addAnnotation(...)
            syntax = null;
            inputType = null;
        }
    }

    @Override
    public String toString() {
        // TODO backus-naur form?
        return "Operator(possible operations: " + StringUtil.toFormattedString(operationDefs, 1000);
    }

    /**
     * class holding a single operation
     * 
     * @author Tom, Thomas Schneider
     * @version $Revision$
     */
    class Operation {
        INPUT[] op;

        public static final int MODE_INFIX = 0;
        public static final int MODE_PREFIX = 1;
        public static final int MODE_POSTFIX = 2;

        public Operation(INPUT term) {
            op = extractOperation(term, MODE_INFIX);
        }

        public Operation(INPUT term, int mode) {
            op = extractOperation(term, mode);
        }

        /**
         * extractOperation
         * 
         * @param term term to extract an operation from
         * @param mode one of {@link #MODE_INFIX}, {@link #MODE_PREFIX}, {@link #MODE_POSTFIX}.
         * @return array holding operation parameter
         */
        @SuppressWarnings("unchecked")
        protected INPUT[] extractOperation(INPUT term, int mode) {
            //technical workaround, see #inputType
//            INPUT[] OP = (INPUT[]) Array.newInstance(BeanUtil.getGenericType(this.getClass()), 3);
            INPUT[] OP = (INPUT[]) Array.newInstance(inputType, 3);

            INPUT empty = syntax(KEY_EMPTY);

            INPUT sOpd = syntax(KEY_OPERAND);
            INPUT sOpt = syntax(KEY_OPERATION);

            //extract operands dependent on mode, but always in form: op[0] = operand1, op[1] = operator, op[2] = operand2
            OP[mode == MODE_PREFIX ? 1 : 0] = extract(term, mode == MODE_PREFIX ? sOpt : sOpd, empty);
            OP[mode == MODE_INFIX ? 1 : mode == MODE_POSTFIX ? 2 : 0] = extract(term,
                mode == MODE_INFIX ? sOpt : sOpd,
                empty);
            OP[mode == MODE_POSTFIX ? 1 : 2] = extract(term, mode == MODE_POSTFIX ? sOpt : sOpd, empty);

            /*
             * on INFIX without first operand, an operation like '!var' should 
             * result in OP[0]="", OP[1]="!", OP[2]="var".
             * so we have to swap the operands
             */
            if (mode == MODE_INFIX && isEmpty(OP[2])) {
                CollectionUtil.swap(OP, 0, 2);
            }
//            //default first operand or/and default operator
//            if (isEmpty(OP[0]))
//                OP[0] = syntax(KEY_DEFAULT_OPERAND);
//            if (isEmpty(OP[1]))
//                OP[1] = syntax(KEY_DEFAULT_OPERATOR);

            return OP;
        }

        /**
         * o1
         * 
         * @return operand1
         */
        public INPUT o1() {
            return op[0];
        }

        /**
         * op
         * 
         * @return operator
         */
        public INPUT op() {
            return op[1];
        }

        /**
         * o2
         * 
         * @return operand2
         */
        public INPUT o2() {
            return op[2];
        }

        @Override
        public String toString() {
            return "" + op[0] + op[1] + op[2];
        }
    }
}