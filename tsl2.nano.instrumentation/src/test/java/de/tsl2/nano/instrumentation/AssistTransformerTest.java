package de.tsl2.nano.instrumentation;

import org.junit.BeforeClass;
import org.junit.Test;

public class AssistTransformerTest {
    @BeforeClass
    public static void setUp() {
        System.setProperty("si.packages", AssistTransformerTest.class.getPackage().getName() + ".*");
    }

    @Test
    public void testAssistTransformer() {

    }
}
