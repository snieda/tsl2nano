package de.tsl2.nano.instrumentation;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class AssistTransformer implements ClassFileTransformer {
    /**
     * holding key: reg-exp-filter, value: script-file-path without tag 'before' or
     * 'after'
     */
    private static Map<String, Script> enhancingScripts;

    AssistTransformer(Properties scriptinginstrumentation) {
        // we re-map key/value -> value/key to get the filters as keys. the property
        // file uses script-names as keys to be readable.
        enhancingScripts = new HashMap<>();
        for (Object siKey : scriptinginstrumentation.keySet()) {
            if (siKey.equals(ScriptAgent.PACKAGES))
                continue;
            enhancingScripts.put((String) scriptinginstrumentation.get(siKey), new Script((String) siKey));
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) {

        byte[] byteCode = classfileBuffer;

        log("AGENT Transforming class " + className);
        try {
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = cp.get(className);
            for (Object filter : enhancingScripts.keySet()) {
                Set<CtMethod> methods = matchMethods(cc, (String) filter);
                for (CtMethod m : methods) {
                    enhanceMethod(m, enhancingScripts.get(filter));
                }
            }
            byteCode = cc.toBytecode();
            cc.detach();
        } catch (NotFoundException | CannotCompileException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return byteCode;
    }

    private static final void log(Object obj) {
        System.out.println(obj);
    }

    private Set<CtMethod> matchMethods(CtClass cc, String filter) {
        Set<CtMethod> matched = new HashSet<>();
        CtMethod[] methods = cc.getDeclaredMethods();
        String id;
        for (int i = 0; i < methods.length; i++) {
            id = getMethodID(methods[i]);
            if (id.matches(filter))
                matched.add(methods[i]);
        }
        return matched;
    }

    private String getMethodID(CtMethod m) {
        try {
            return Arrays.toString(m.getDeclaringClass().getAnnotations()) + " " + Arrays.toString(m.getAnnotations())
                    + m.getLongName() + " throws " + Arrays.toString(m.getExceptionTypes());
        } catch (ClassNotFoundException | NotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private void enhanceMethod(CtMethod m, Script script) throws CannotCompileException, NotFoundException {
        log("AGENT enhancing method " + getMethodID(m));
        String collectArgsAsCommandString = collectArgsAsCommandString(m);
        m.addLocalVariable("before_", CtClass.longType);
        m.insertBefore(collectArgsAsCommandString + getClass().getName() + ".runScript(\"" + script.name + "\", -1, args);");
        if (script.bodyContent != null)
            m.setBody(collectArgsAsCommandString + getClass().getName() + ".runScript(\"" + script.name
                    + "\", 0, args);");
        m.insertAfter(collectArgsAsCommandString + getClass().getName() + ".runScript(\"" + script.name + "\", 1, args);");
    }

    private String collectArgsAsCommandString(CtMethod m) throws NotFoundException {
        StringBuffer cmd = new StringBuffer("java.util.HashMap args = new java.util.HashMap();");
        List<String> argNames = getArgNames(m);
        for (String arg : argNames) {
            cmd.append("args.put(\"" + arg + "\", " + arg + ");");
        }
        return cmd.toString();
    }

    static List<String> getArgNames(CtMethod m) throws NotFoundException {
        ArrayList<String> argNames = new ArrayList<>();
        for (int i = 0; i < m.getParameterTypes().length; i++) {
            argNames.add("$" + i);
        }
        return argNames;
    }

    public static Object runScript(String scriptName, int pos, Map<String, Object> args) {
        for (Script s : enhancingScripts.values()) {
            if (s.name.equals(scriptName)) {
                s.run(pos, args);
            }
        }
        throw new IllegalArgumentException("no script defined for: " + scriptName);
    }
}
