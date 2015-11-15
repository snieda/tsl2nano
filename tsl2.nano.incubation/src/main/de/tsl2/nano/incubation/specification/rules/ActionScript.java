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

import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.incubation.specification.AbstractRunnable;

/**
 * Uses javascript engine (java6+7: rhino, java8: nashorn) to evaluate an operation.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class ActionScript<T> extends AbstractRunnable<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = -5452505496704132851L;

    transient ScriptEngine engine;

    void init() {
        engine =
            new ScriptEngineManager().getEngineByName("javascript");
    }

    @SuppressWarnings("unchecked")
    @Override
    public T run(Map<String, Object> arguments, Object... extArgs) {
        try {
            return (T) engine.eval(operation, bind(arguments));
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

}
