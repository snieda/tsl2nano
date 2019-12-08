package de.tsl2.nano.gp;

import java.util.function.Function;

import de.tsl2.nano.core.execution.ScriptEngineProvider;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * generic fitness-function for evolution algorithms to be used for script languages like javascript, python, ruby etc.<p/>
 * the script file will have as input parameter: input (type: Long[]) and should return a result of type Double/Float!
 * <pre>
 * javascript example (file: fit.js):
 * 
 * var s = 0; for (i=0; i<input.length; i++) s+=input[i]; s+=0.001; 
 * 
 * with system-property: -Devolutionalalgorithm.fitnessfuntion.script=fit.js
 * </pre>
 */
public class PolyglottFitnessFunction implements Function<Long[], Double> {
    public static final String EVO_FIT_SCRIPT = "evolutionalalgorithm.fitnessfunction.script";
    public static final String EVO_FIT_LANG = "evolutionalalgorithm.fitnessfunction.language";
    ScriptEngineProvider<Double> engine;
    String script, scriptFile;
    // static final Map<String, String> LANGUAGEEXTENSIONS = MapUtil.asMap("js", "javascript", "py", "python", "tcl", "tcl", "ry", "ruby", "kt", "kotlin");

    public PolyglottFitnessFunction() {
        String scriptLanguage = System.getProperty(EVO_FIT_LANG);
        scriptFile = System.getProperty(EVO_FIT_SCRIPT);
        if (scriptFile == null) {
            System.out.println(ScriptEngineProvider.printEngines());
            throw new IllegalArgumentException("Please define java property '" + EVO_FIT_LANG + "' and '" + EVO_FIT_SCRIPT + "'\n\tThe script may access the variable 'input' of type Long[] ");
        }
        script = FileUtil.getFileString(scriptFile);
        if (scriptLanguage == null && scriptFile.contains(".")) {
            StringUtil.substring(scriptFile, ".", null, true);
            // scriptLanguage = LANGUAGEEXTENSIONS.get(scriptLanguage);
            if (scriptLanguage == null)
                System.out.println(ScriptEngineProvider.printEngines());
        }
        engine = new ScriptEngineProvider<>(scriptLanguage, script);
    }

    @Override
    public Double apply(Long[] t) {
        return ((Number) engine.run(MapUtil.asMap("input", t))).doubleValue();
    }

    @Override
    public String toString() {
        return super.toString() + " (script=" + scriptFile + ", engine=" + engine + ")";
    }
}