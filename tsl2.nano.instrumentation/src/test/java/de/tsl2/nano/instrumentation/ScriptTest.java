package de.tsl2.nano.instrumentation;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;

public class ScriptTest {

    @Test
    public void testScriptLoading() {
        Script script = new Script("target/test-classes/console-log.js");
        script.beforeContent.contains("hello method before");
        script.afterContent.contains("hello method after");
    }

    public void testScriptRun() throws Exception {
        Script script = new Script("target/test-classes/console-log.js");
        Object result = script.run(Script.BEFORE, new HashMap<>());
        assertTrue(result instanceof Long);
    }

    public void matchedMethod() {
        System.out.println("hello instrumentation");
    }
}
