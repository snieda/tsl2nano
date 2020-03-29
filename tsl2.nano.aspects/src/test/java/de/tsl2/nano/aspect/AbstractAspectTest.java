package de.tsl2.nano.aspect;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AbstractAspectTest {
    @Test
    public void testAbstractTracing() {
        for (int i = 0; i < 2; i++) {
            new Account0().XXXX();
        }
    }

    @Test
    public void testAbstractProfiling() {
        System.setProperty("agent.profile.duration", "2");
        System.setProperty("agent.profile.count", "2");
        boolean result = false;
        for (int i = 0; i < 2; i++) {
            result |= new Account0().YYYY();
        }

        assertTrue(result);
    }

    @Test
    public void testAbstractMocking() {
        for (int i = 0; i < 2; i++) {
            new Account0().ZZZZ();
        }
    }
}

class Account0 extends Account {
    public boolean XXXX() {
        return true;
    }

    public boolean YYYY() {
        return true;
    }

    public boolean ZZZZ() {
        return true;
    }
}
