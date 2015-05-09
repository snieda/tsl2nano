/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 24.10.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.util.operation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.ElementArray;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.format.FormatUtil;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.parser.SParser;

/**
 * Function is a singelton (configured through a serialized xml) to parse functional expressions, calling this functions (defined java methods) and returning the result.
 * @author Tom
 * @version $Revision$
 */
/**
 * @param <OUTPUT>
 * @author Tom
 * @version $Revision$
 */
@Default(value = DefaultType.FIELD, required = false)
public class Function<OUTPUT> extends SParser {
    private static final Log LOG = LogFactory.getLog(Function.class);
    @ElementArray(name = "function-container")
    List<FunctionContainer> funcContainer;
    static final String FUNC_BEGIN = "\\(";
    static final String FUNC_NAME = "(\\w+)";
    static final String FUNC_END = "\\)";
    static final String FUNC_ARG = "(\\s*\\w+\\s*[,]?)";

    Function() {
    }

    @SuppressWarnings("unchecked")
    public OUTPUT eval(CharSequence expression, Map<CharSequence, Object> values) {
        CharSequence func;
        OUTPUT res = null;
        expression = wrap(expression);
        while (!isEmpty(expression)) {
            while (isEmpty(func = extract(expression, FUNC_NAME + FUNC_BEGIN + FUNC_ARG + "*" + FUNC_END))) {
                if (isEmpty(extract(expression, FUNC_NAME + FUNC_BEGIN, ""))) {
                    break;
                }
            }
            if (isEmpty(func)) {
                break;
            }
            res = calc(func, values);
            values.put(func, res);
            replace(expression, func, res != null ? FormatUtil.getDefaultFormat(res, false).format(res) : "");
        }
        OUTPUT result = (OUTPUT) values.get(expression);
        return (OUTPUT) (result != null ? result : res != null ? res : expression);
    }

    private OUTPUT calc(CharSequence func, Map<CharSequence, Object> values) {
        CharSequence f = wrap(func);
        CharSequence name = this.extract(f, FUNC_NAME, "");
        List<Object> argList = new ArrayList<Object>();
        CharSequence p = f;
        while (!isEmpty(p = extract(f, "\\w+", ""))) {
            if (NumberUtil.isNumber(p.toString())) {
                argList.add(NumberUtil.getBigDecimal(p.toString()).doubleValue());
            } else {
                argList.add(values.get(p));
            }
        }
        OUTPUT result = null;
        if (funcContainer == null) {
            funcContainer = Arrays.asList(new FunctionContainer(Math.class, double.class, true));
        }

        boolean funcFound = false;
        for (FunctionContainer fc : funcContainer) {
            if (fc.hasMethod(name)) {
                try {
                    Object[] args =
                        fc.argType != null ? Util.convertAll(fc.argType, argList.toArray()) : argList.toArray();
                    result = (OUTPUT) BeanClass.call(fc.funcClass, name.toString(), fc.usePrimitives, args);
//                    if (fc.argType != null)//TODO: create an if clause to constrain this formatting
//                        result = (OUTPUT) FormatUtil.getDefaultFormat(result, false).format(result);
                    LOG.info(func + " = " + result + " [with " + values + "]");
                    funcFound = true;
                    break;
                } catch (Exception e) {
                    // try the next one...
                }
            }
        }
        if (!funcFound) {
            throw new IllegalArgumentException("function " + func + " is not defined!");
        }
        return result;
    }

    /**
     * filters the values with names found in pars and returns the as new object array.
     * 
     * @param pars parameter filter
     * @param values all available values
     * @return argument array
     */
    private Object[] getArgs(List<String> pars, Map<CharSequence, Object> values) {
        Object[] args = new Object[pars.size()];
        int i = 0;
        for (String p : pars) {
            args[i++] = values.get(p);
        }
        return args;
    }

}

class FunctionContainer {
    Class funcClass;

    /** if defined, all function args will be converted to this type! */
    Class<?> argType = double.class;
    /**
     * if true, the method to be called should have primitive parameter types, where possible! Example: java.util.Math
     * defines only methods on primitive types. Calling this methods, 'usePrimitives' should be true.
     */
    boolean usePrimitives = true;

    String[] methodNames;

    /**
     * constructor
     * 
     * @param funcClass
     * @param argType
     * @param usePrimitives
     * @param methodNames
     */
    public FunctionContainer(Class funcClass, Class<?> argType, boolean usePrimitives, String... methodNames) {
        super();
        this.funcClass = funcClass;
        this.argType = argType;
        this.usePrimitives = usePrimitives;
        if (methodNames == null || methodNames.length == 0) {
            Method[] methods = funcClass.getMethods();
            methodNames = new String[methods.length];
            for (int i = 0; i < methods.length; i++) {
                methodNames[i] = methods[i].getName();
            }
        }
        this.methodNames = methodNames;
    }

    public boolean hasMethod(CharSequence name) {
        return Arrays.asList(methodNames).contains(name);
    }
}