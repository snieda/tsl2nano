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
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.core.AppLoader;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.incubation.specification.AbstractRunnable;
import de.tsl2.nano.incubation.specification.ParType;
import de.tsl2.nano.util.ClassFinder;

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

    public static ScriptEngine createEngine(String language) {
        provideLanguage(language);
//        if (!AppLoader.hasCompiler())
//           throw new IllegalStateException("can't start javascript engine on jre environment. a full jdk must be present!");
        //java7 provides rhino as javascript, java8 provides nashorn
        ScriptEngine engine = language != null ? new ScriptEngineManager().getEngineByName(language)
            : new ScriptEngineManager().getEngineFactories().iterator().next().getScriptEngine();
        if (engine == null)
            throw new IllegalStateException("couldn't create engine for: '" + language + "'\n\tavailable engines:\n"
                + printEngines());
        LOG.info("script engine loaded: " + engine);
        return engine;
    }

    protected ScriptEngine engine() {
        if (engine == null)
            engine = ActionScript.createEngine(language);
        return engine;
    }
    
    private static void provideLanguage(String language) {
        if (language != null && new ClassFinder().findClass(language) == null) {
            String pck = ENV.getPackagePrefix(language);
            ENV.loadClassDependencies(pck);
        }
        List<ScriptEngineFactory> engines = new ScriptEngineManager().getEngineFactories();
        if (engines.isEmpty())
            throw new IllegalStateException("no script engine available!");
        else if (language != null && new ScriptEngineManager().getEngineByName(language) == null)
            throw new IllegalStateException("couldn't create engine for: '" + language + "'\n\tavailable engines:\n"
                    + printEngines());
    }

    public static String printEngines() {
        StringBuilder str = new StringBuilder("\n--------------------------------------------------------------------------------");
        List<ScriptEngineFactory> engs = new ScriptEngineManager().getEngineFactories();
        for (ScriptEngineFactory e : engs) {
            str.append(e.getEngineName() + ": " + e.getLanguageName() + " " + e.getLanguageVersion() + "\n");
        }
        return str.append("--------------------------------------------------------------------------------\n").toString();
    }
    
    private static boolean isJava8() {
        return AppLoader.getJavaVersion().equals("1.8");
    }

    @SuppressWarnings("unchecked")
    @Override
    public T run(Map<String, Object> arguments, Object... extArgs) {
        try {
            return (T) engine().eval(getOperation(), bind(engine(), arguments));
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

}
