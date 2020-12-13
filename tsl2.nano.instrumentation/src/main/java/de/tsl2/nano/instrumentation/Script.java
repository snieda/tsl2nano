package de.tsl2.nano.instrumentation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptException;

public class Script {
    String name;
    String ext;
    String beforeContent;
    String bodyContent;
    String afterContent;

    static final int BEFORE = -1;
    static final int BODY = 0;
    static final int AFTER = 1;

    public Script(String scriptName) {
        int div = scriptName.lastIndexOf(".");
        name = scriptName.substring(0, div);
        ext = scriptName.substring(div + 1);

        beforeContent = readFile(getFileName(BEFORE));
        bodyContent = readFile(getFileName(BODY));
        afterContent = readFile(getFileName(AFTER));

        if (beforeContent == null && bodyContent == null && afterContent == null)
            throw new IllegalArgumentException("ERROR: at least one agent script must be readable and not empty: "
                    + getFileName(BEFORE) + " or " + getFileName(BODY) + " or " + getFileName(AFTER));
    }

    private String readFile(String scriptBefore) {
        try {
            return beforeContent = new String(Files.readAllBytes(Paths.get(scriptBefore)));
        } catch (IOException e) {
            ScriptAgent.log(e);
            return null;
        }
    }
    /** callback on enhanced class on runtime */
    public Object run(int pos, Map<String, Object> args) {
        ScriptAgent.log("[Agent] running script " + getFileName(pos) + " on args: " + args.keySet());
        if (pos == BEFORE  && beforeContent == null)
            return null;
        else if (pos == BODY && bodyContent == null)
            return null;
        else if (pos == AFTER && afterContent == null)
            return null;
        javax.script.ScriptEngine engine = new javax.script.ScriptEngineManager().getEngineByExtension(ext);
        Bindings argBindings = engine.createBindings();
        for (String arg : argBindings.keySet()) {
            argBindings.put(arg, argBindings.get(arg));
        }
        try {
            return engine.eval(pos == BEFORE ? beforeContent : pos == BODY ? bodyContent : afterContent, argBindings);
        } catch (ScriptException e) {
            ScriptAgent.log(e);
            return null;
        }
    }

    private String getFileName(int pos) {
        return name + (pos == BEFORE ? ".before." : pos == BODY ? ".body." : ".after.") + ext;
    }
}
