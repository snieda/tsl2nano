/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 04.11.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.core.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import de.tsl2.nano.core.ENV;

/**
 * Uses javascript engine (java6+7: rhino, java8: nashorn) to evaluate an operation.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class ScriptEngineProvider<T> {

    String language;

    transient ScriptEngine engine;
    private String operation;

    public ScriptEngineProvider(String language, String operation) {
        this.language = language;
        this.operation = operation;
    }

    protected ScriptEngine engine() {
        if (engine == null)
            engine = ScriptEngineProvider.createEngine(language);
        return engine;
    }
    
	public static ScriptEngine createEngine(String language) {
    	if (language == null) {
    		System.out.println("WARN: script language undefined -> using 'javascript' as default!");
    		language = "javascript";
    	}

        ScriptEngineManager managerThread = new ScriptEngineManager();
        ScriptEngine engine = managerThread.getEngineByName(language);
        if (engine != null)
        	return engine;
        ScriptEngineManager managerSystem = new ScriptEngineManager(System.class.getClassLoader());
        engine = managerSystem.getEngineByName(language);
        if (engine != null)
        	return engine;
		List<ScriptEngineFactory> engines = managerThread.getEngineFactories();
        if (engines.isEmpty()) {
        	engines = new ScriptEngineManager(System.class.getClassLoader()).getEngineFactories();
            if (!ENV.isModeOffline()) {
                ENV.loadJarDependencies(language);
                engines = managerThread.getEngineFactories();
            }
        	if (engines.isEmpty())
        		throw new IllegalStateException("no script engine available!");
        }
        String info = "script engine " + language + " not found. Available engines are: " +
        		printEngines();
        throw new IllegalStateException(info);
	}

    public static String printEngines() {
        List<ScriptEngineFactory> engs = new ArrayList<>();
        engs.addAll(new ScriptEngineManager().getEngineFactories());
        engs.addAll(new ScriptEngineManager(System.class.getClassLoader()).getEngineFactories());
        
        StringBuilder str = new StringBuilder("\n--------------------------------------------------------------------------------");
        for (ScriptEngineFactory e : engs) {
            str.append(e.getEngineName() + ": " + e.getLanguageName() + " " + e.getLanguageVersion() + "\n");
        }
        return str.append("--------------------------------------------------------------------------------\n").toString();
    }
    
    @SuppressWarnings("unchecked")
    public T run(Map<String, Object> arguments) {
        try {
            return (T) engine().eval(operation, bind(engine(), arguments));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Bindings bind(ScriptEngine engine, Map<String, Object> arguments) {
        Bindings bindings = engine.createBindings();
        bindings.put("polyglot.js.allowAllAccess", true); //needed for graalvm to access java objects
        bindings.putAll(arguments);
        return bindings;
    }

    @Override
    public String toString() {
        return super.toString() + "(engine: " + engine() + ")";
    }
}
