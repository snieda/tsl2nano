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

import org.apache.commons.logging.Log;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.incubation.specification.AbstractRunnable;
import de.tsl2.nano.incubation.specification.ParType;

/**
 * Uses javascript engine (java6+7: rhino, java8: nashorn) to evaluate an operation.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class ActionScript<T> extends AbstractRunnable<T> {
    private static final Log LOG = LogFactory.getLog(ActionScript.class);
    /** serialVersionUID */
    private static final long serialVersionUID = -5452505496704132851L;

    transient ScriptEngine engine;

    /**
     * constructor
     */
    public ActionScript() {
    }

    /**
     * constructor
     * 
     * @param name
     * @param operation
     * @param parameter
     */
    public ActionScript(String name, String operation, LinkedHashMap<String, ParType> parameter) {
        super(name, operation, parameter);
        engine = createEngine();
    }

    public static ScriptEngine createEngine() {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        if (engine == null)
            throw new IllegalStateException("couldn't create engine for: 'javascript'\n\tavailable engines: "
                + StringUtil.toFormattedString(new ScriptEngineManager().getEngineFactories(), 1, true));
        LOG.info("script engine loaded: " + engine);
        return engine;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T run(Map<String, Object> arguments, Object... extArgs) {
        try {
            return (T) engine.eval(operation, bind(engine, arguments));
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public static Bindings bind(ScriptEngine engine, Map<String, Object> arguments) {
        Bindings bindings = engine.createBindings();
        bindings.putAll(arguments);
        return bindings;
    }

    @Override
    @Commit
    protected void initDeserializing() {
        engine = createEngine();
        super.initDeserializing();
    }
}
