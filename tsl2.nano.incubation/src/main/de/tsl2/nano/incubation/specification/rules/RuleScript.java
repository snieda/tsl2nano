/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 04.11.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.incubation.specification.rules;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.util.operation.Operator;

/**
 * Uses javascript engine (java6+7: rhino, java8: nashorn) to evaluate an operation.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class RuleScript<T> extends AbstractRule<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -6479357867492017791L;

    transient ScriptEngine engine;

    /**
     * constructor
     */
    public RuleScript() {
        super();
    }

    /**
     * constructor
     * @param name
     * @param operation
     * @param parameter
     */
    public RuleScript(String name, String operation, LinkedHashMap parameter) {
        super(name, operation, parameter);
        init();
    }

    void init() {
        engine =
            new ScriptEngineManager().getEngineByName("javascript");
    }

    @SuppressWarnings("unchecked")
    @Override
    public T run(Map<String, Object> arguments, Object... extArgs) {
        if (!initialized) {
            importSubRules();
        }
        arguments = checkedArguments(arguments, ENV.get("application.mode.strict", false));
        //in generics it is not possible to cast from Map(String,?) to Map(CharSequence, ?)
        Object a = arguments;
        
        try {
            T result = (T) engine.eval(operation, bind(arguments));
            checkConstraint(Operator.KEY_RESULT, result);
            return result;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    private Bindings bind(Map<String, Object> arguments) {
        Bindings bindings = engine.createBindings();
        bindings.putAll(arguments);
        return bindings;
    }

    @Override
    @Commit
    protected void initDeserializing() {
        super.initDeserializing();
        init();
    }
}
