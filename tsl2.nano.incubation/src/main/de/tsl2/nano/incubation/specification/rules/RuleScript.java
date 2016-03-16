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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.incubation.specification.ParType;
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

    private static final Log LOG = LogFactory.getLog(RuleScript.class);

    transient ScriptEngine engine;

    public static final char PREFIX = '%';

    /**
     * constructor
     */
    protected RuleScript() {
        super();
    }

    /**
     * constructor
     * 
     * @param name
     * @param operation
     * @param parameter
     */
    public RuleScript(String name, String operation, LinkedHashMap<String, ParType> parameter) {
        super(name, operation, parameter);
        init();
    }

    void init() {
        engine = ActionScript.createEngine();
    }

    @Override
    public String prefix() {
        return String.valueOf(PREFIX);
    }

    @Override
    public T run(Map<String, Object> arguments, Object... extArgs) {
        if (!initialized) {
            importSubRules();
        }
        arguments = checkedArguments(arguments, ENV.get("app.mode.strict", false));
        //in generics it is not possible to cast from Map(String,?) to Map(CharSequence, ?)

        try {
            LOG.debug("running rule <" + toString() + "> on arguments: " + arguments);
            Object obj = engine.eval(getOperation(), bind(arguments));
            T result = (T) transform(obj);
            checkConstraint(Operator.KEY_RESULT, result);
            return result;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * some java script objects like 'java.util.HashMap' must be transformed to real java objects. E.g. the javascript
     * 'java.util.HashMap' seems not to implement Serializable!
     * 
     * @param jsValue
     * @return real java class object
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private T transform(Object jsValue) {
        if (jsValue instanceof Map)
            return (T) new LinkedHashMap((Map) jsValue);
        else if (jsValue instanceof Collection)
            return (T) new ArrayList((Collection) jsValue);
        else
            return (T) jsValue;
    }

    private Bindings bind(Map<String, Object> arguments) {
        return ActionScript.bind(engine, arguments);
    }

    @Override
    @Commit
    protected void initDeserializing() {
        init();
        super.initDeserializing();
    }
}
