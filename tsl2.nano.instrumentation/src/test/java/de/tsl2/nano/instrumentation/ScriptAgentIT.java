package de.tsl2.nano.instrumentation;

import org.junit.BeforeClass;
import org.junit.Test;

public class ScriptAgentIT {

    @BeforeClass
    public static void setUp() {
        System.setProperty("si.packages", ScriptAgentTest.class.getPackage().getName() + ".*");
        System.setProperty("si.filter", ".*matchingMethod.*");
        System.setProperty("si.script", "target/test-classes/console-log.js");
    }

    @Test
    public void testInstrumentation() {
        matchingMethod();
    }

    public void matchingMethod() {
        System.out.println("hello instrumentation");
    }
}
