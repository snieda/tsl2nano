package de.tsl2.nano.instrumentation;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

public class ScriptTest {

    @Test
    public void testScriptLoading() {
        Script script = new Script("target/test-classes/console-log.js");
        script.beforeContent.contains("hello method before");
        script.afterContent.contains("hello method after");
    }

    public void testScriptRun() throws NotFoundException {
        Script script = new Script("target/test-classes/console-log.js");
        CtClass ctClass = ClassPool.getDefault().get(getClass().getName());
        Object result = script.run(ctClass.getDeclaredMethod("callbackForInstrumentation"), true);
        assertTrue(result instanceof Long);
    }

    public void callbackForInstrumentation() {
        System.out.println("hello instrumentation");
    }
}
