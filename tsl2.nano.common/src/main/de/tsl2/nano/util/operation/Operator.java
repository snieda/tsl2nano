package de.tsl2.nano.util.operation;

import java.lang.reflect.Array;
import java.util.Map;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.util.StringUtil;

/**
 * Base class to execute any operations with terms having two operand and one operator. it is like a primitive parser
 * using regular expressions. may be used to implement equation-solvers etc.
 * 
 * @param <INPUT> type of input to extract operations from
 * @param <OUTPUT> type of output as result.
 * @author Thomas Schneider
 * @version $Revision$
 */
public abstract class Operator<INPUT, OUTPUT> {
    /**
     * technical member while it's not possible to create an array out of a generic type. the
     * BeanUtil.getGenericType(Class) can't solve that problem, too.
     */
    Class<? extends INPUT> inputType;
    /** syntax defining special expression parts */
    Map<String, INPUT> syntax;
    /** a map containing any values. values found by this solver must be of right type */
    Map<INPUT, OUTPUT> values;
    /** converter to convert an operand to a result type and vice versa */
    IConverter<INPUT, OUTPUT> converter;
    /** holds all operations to be resolvable */
    Map<INPUT, IAction<OUTPUT>> operationDefs;

    public static final String KEY_BEGIN = "begin";
    public static final String KEY_END = "end";
    public static final String KEY_TERM = "term";
    public static final String KEY_TERM_ENCLOSED = "term.enclosed";
    public static final String KEY_BETWEEN = "between";
    public static final String KEY_CONCAT = "concat";
    public static final String KEY_OPERATION = "operation";
    public static final String KEY_OPERAND = "operand";
    public static final String KEY_EMPTY = "empty";

    public Operator() {
        super();
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
        this.inputType = inputClass;
        this.converter = converter;
        this.values = values;
        syntax = createSyntax();
        createOperations();
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
     * define all possible operations. see {@link #operationDefs}
     */
    protected abstract void createOperations();
    
    /**
     * helper to define an operation-definition
     * 
     * @param operator operator
     * @param operation operation to be executed
     */
    protected void addOperation(INPUT operator, IAction<OUTPUT> operation) {
        operationDefs.put(operator, operation);
    }

//    public OUTPUT eval(INPUT expression) {
//        return eval(new StringBuilder(expression.toString()));
//    }
    /**
     * eval
     * 
     * @param expression
     * @return
     */
    public OUTPUT eval(INPUT expression) {
        //create an operable expression like a sequence
        expression = wrap(expression);
        //extract all terms
        INPUT term;
        INPUT t;
        while (true) {
            term = extract(expression, syntax(KEY_TERM_ENCLOSED));
            if (isEmpty(term))
                break;
            t = extract(term, syntax(KEY_TERM));
            replace(expression, term, converter.from(operate(wrap(t), values)));
        }
        return operate(expression, values);
    }

    /**
     * isEmpty
     * 
     * @param term term to check
     * @return true, if term is empty
     */
    protected boolean isEmpty(INPUT term) {
        return StringUtil.isEmpty(term, true);
    }

    /**
     * replace
     * 
     * @param src
     * @param expression
     * @param replace
     */
    protected abstract void replace(INPUT src, INPUT expression, INPUT replace);

    /**
     * should be overridden if you need a transformation. F.e., if your INPUT is CharSequence and you need a conversion
     * from string to stringbuilder.
     * 
     * @param src source to be transformed/wrapped
     * @return transformed/wrapped value
     */
    protected INPUT wrap(INPUT src) {
        return src;
    }

    /**
     * see {@link #wrap(Object)}.
     * 
     * @param src source to be re-transformed
     * @return re-transformed/unwrapped value
     */
    protected INPUT unwrap(INPUT src) {
        return src;
    }

    /**
     * main funtion to extract operation elements and to execute the operation.
     * 
     * @param term term to analyse and execute
     * @param values pre-defined values
     * @return result of operation
     */
    protected OUTPUT operate(INPUT term, Map<INPUT, OUTPUT> values) {
        Operation op = new Operation(term);

        /*
         * the value map may contain any values - but the found value must have the right type!
         */
        OUTPUT n1 = values.get(op.o1());
        n1 = n1 != null ? n1 : newOperand(op.o1());
        OUTPUT n2 = values.get(op.o2());
        n2 = n2 != null ? n2 : newOperand(op.o2());

        OUTPUT result;
        IAction<OUTPUT> operation = operationDefs.get(op.op());
        if (operation != null) {
            operation.setParameter(new Object[] { n1, n2 });
            result = operation.activate();
        } else {
            throw new IllegalArgumentException(term.toString());
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

    private INPUT extract(INPUT source, INPUT regexp) {
        return extract(source, regexp, null);
    }

    protected abstract INPUT extract(INPUT source, INPUT match, INPUT replacement);

    /**
     * concatenates given elements of type INPUT
     * 
     * @param input input array to concatenate. the array is of type Object as a technical workaround on auto-creating
     *            an INPUT[].
     * @return concatenation of input
     */
    protected abstract INPUT concat(Object... input);

    protected OUTPUT newOperand(INPUT expr) {
        return (OUTPUT) converter.to(expr);
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