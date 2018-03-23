package de.tsl2.nano.inspection;

import static org.junit.Assert.*;

import org.junit.Test;

import de.tsl2.nano.plugin.Plugins;
import de.tsl2.nano.plugin.Plugin;

public class InspectorsTest {

    @Test
    public void processTwoInspectors() {
        assertEquals("HALLOHALLO", Plugins.process(IInspectorTest.class).machWas("hallo"));
    }

}

interface IInspectorTest extends Plugin {
    String machWas(String txt);
}
class InspectorImpl1Test implements IInspectorTest {
    @Override
    public String machWas(String txt) {
        return txt.toUpperCase();
    }
}
class InspectorImpl2Test implements IInspectorTest {
    @Override
    public String machWas(String txt) {
        return txt + txt;
    }
}