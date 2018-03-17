package de.tsl2.nano.inspection;

import static org.junit.Assert.*;

import org.junit.Test;

public class InspectorsTest {

    @Test
    public void processTwoInspectors() {
        assertEquals("HALLOHALLO", Inspectors.process(IInspectorTest.class).machWas("hallo"));
    }

}

interface IInspectorTest extends Inspector {
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