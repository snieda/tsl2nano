/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 04.11.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.specification.rules;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.script.ScriptEngine;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.execution.ScriptEngineProvider;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.specification.AbstractRunnable;
import de.tsl2.nano.specification.ParType;

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

    @Attribute(required=false)
    String language;

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
    }

    private static void provideLanguage(String language) {
        if (language != null && ClassFinder.self().findClass(language) == null) {
            String pck = ENV.getPackagePrefix(language);
            if (pck != null)
            	ENV.loadClassDependencies(pck);
        }
    }

    protected ScriptEngine engine() {
        if (engine == null)
            engine = ScriptEngineProvider.createEngine(language);
        return engine;
    }
    
	public static ScriptEngine createEngine(String language) {
        language = language == null ? "javascript" : language;
    	provideLanguage(language);
		return ScriptEngineProvider.createEngine(language);
	}

    @SuppressWarnings("unchecked")
    @Override
    public T run(Map<String, Object> arguments, Object... extArgs) {
        try {
            return (T) engine().eval(getOperation(), ScriptEngineProvider.bind(engine(), arguments));
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

}
