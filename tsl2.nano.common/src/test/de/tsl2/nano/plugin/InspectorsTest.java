package de.tsl2.nano.plugin;

import static org.junit.Assert.*;

import org.junit.Test;

import de.tsl2.nano.plugin.Plugins;
import de.tsl2.nano.plugin.Plugin;

public class InspectorsTest {

    @Test
    public void processTwoInspectors() {
        assertEquals("HALLOHALLO", Plugins.process(IInspectorTest.class).machWas("hallo"));
    }

    //changes to >JDK15: the classes+interface have to be public (and static to be constructable)
    public interface IInspectorTest extends Plugin {
        String machWas(String txt);
    }
    public static class InspectorImpl1Test implements IInspectorTest {
        @Override
        public String machWas(String txt) {
            return txt.toUpperCase();
        }
    }
    public static class InspectorImpl2Test implements IInspectorTest {
        @Override
        public String machWas(String txt) {
            return txt + txt;
        }
    }
}
