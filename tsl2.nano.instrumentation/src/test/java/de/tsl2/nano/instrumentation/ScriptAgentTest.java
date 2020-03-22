package de.tsl2.nano.instrumentation;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

public class ScriptAgentTest {
    @BeforeClass
    public static void setUp() {
        System.setProperty("si.packages", ScriptAgentTest.class.getPackage().getName() + ".*");
        System.setProperty("si.filter", ".*callbackForInstrumentation.*");
        System.setProperty("si.script", "target/test-classes/console-log.js");
    }

    @Test
    public void testMainLoading() {
        ScriptAgent.initializeAgent();
        assertEquals(getClass().getPackage().getName() + ".*", ScriptAgent.packages);
        assertEquals(2, ScriptAgent.scriptingInstrumentation.size());
    }
    @Test
    public void testAssistTransformer() {
        ScriptAgent.initializeAgent();
        AssistTransformer at = new AssistTransformer(ScriptAgent.scriptingInstrumentation);
        at.transform(null, this.getClass().getName(), this.getClass(), null, null);
    }

    public void callbackForInstrumentation() {
        System.out.println("hello instrumentation");
    }
}
