package de.tsl2.nano.instrumentation;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Properties;

import com.sun.tools.attach.VirtualMachine;

/**
 * <pre>
 * Java Agent to invoke scripts before inside and/or after the call of matching java methods.
 * this is done by bytecode enhancing using javassist.
 *
 * the scripts and the filters for the matching java methods are defined in the user
 * resource file 'META-INF/scripting-instrumentation.properties'. optionally it is
 * possible to define one filter+script through system arguments.
 *
 * syntax of the property-file:
 *  {script-file}={regular-expression to filter full path method identifier}
 *
 * the script-files must be in the user directory user.dir and has the following format:
 *  {file-name-prefix}.{before|body|after}.{script-file-extension}
 *
 * script-file-extension depends on the supported ScriptEnginge languages.
 *
 * Example content of 'scripting-instrumentation.properties':
 *  myjavascript.js=@MyClassAnnotation de.tsl.nano.instr.* @MyMethodAnnotation .*setMyValue.*long
 *
 *  The instrumentation will find all methods 'setMyValue' with at least one parameter of type long.
 *  The script file will be searched in '${user.dir}/myjavascript.before.js' and '${user.dir}/myjavascript.body.js'
 *  and '${user.dir}/myjavascript.after.js'
 *  to invoke them before and after the matching method. the 'body' script will replace the origin method body - may be used
 *  to mock methods.
 *
 * Example System arguments:
 *  -Dsi.filter="...myfilter..." -Dsi.script="...script-file-name..."
 *
 * </pre>
 */
public class ScriptAgent {

    static final String PACKAGES = "packages";
    static final Properties scriptingInstrumentation = new Properties();
    static String packages = System.getProperty("si.packages", ".*");

    public static final void main(String[] args) throws Exception {
        if (args.length < 2) {
            log("usage: instrumentation <jvmPID> <agent-jar-file>");
            return;
        }
        loadAgent(args[0], args[1]);
    }

    public static void loadAgent(String pid, String agentFile) throws Exception {
        VirtualMachine jvm = VirtualMachine.attach(pid);
        jvm.loadAgent(new File(agentFile).getAbsolutePath());
        log("agent " + agentFile + " attached to JVM " + pid);
        jvm.detach();
    }

    /** dynamic entry point for java instrumentation */
    public static void premain(String agentArgs, Instrumentation inst) {
        log("[Agent] In premain method");
        agentmain(agentArgs, inst);
    }

    /** static entry point for java instrumentation */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        log("[Agent] In agentmain method");
        transform(agentArgs, inst);
    }

    static void initializeAgent() {
        try {
            scriptingInstrumentation.load(new FileReader("scripting-instrumentation.properties"));
        } catch (IOException e) {
            log(e);
        }
        if (scriptingInstrumentation.getProperty("si.packages") == null)
            scriptingInstrumentation.put(PACKAGES, packages);
        String filter = System.getProperty("si.filter");
        String script = System.getProperty("si.script");

        if (filter != null && script != null)
            scriptingInstrumentation.put(script, filter);
    }

    private static void transform(String agentArgs, Instrumentation instrumentation) {
        initializeAgent();
        AssistTransformer t = new AssistTransformer(scriptingInstrumentation);
        instrumentation.addTransformer(t, true);
        Class[] allLoadedClasses = instrumentation.getAllLoadedClasses();
        log("[Agent] searching in " + allLoadedClasses.length + " classes for filter: " + packages);
        for (Class<?> clazz : allLoadedClasses) {
            if (clazz.getName().matches(packages)) {
                transformClass(clazz, instrumentation);
            }
        }
    }

    private static void transformClass(Class<?> clazz, Instrumentation instrumentation) {
        try {
            instrumentation.retransformClasses(clazz);
        } catch (Exception ex) {
            throw new RuntimeException("Transform failed for: [" + clazz.getName() + "]", ex);
        }
    }

    static final void log(Object obj) {
        if (obj instanceof Exception)
            System.err.println("ERROR: " + obj);
        else
            System.out.println(obj);
    }

}
