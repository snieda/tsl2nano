package de.tsl2.nano.aspect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AspectCoverAnnotationTest {
    @Cover // <-- important to internally call the aspect on field get/set to know the caller class!
    Account account = new Account2();

    String before, body, after;

    @Test
    public void testCoverBefore() {
        assertFalse(account.allowed());
        assertEquals("where", before);
    }

    @Test
    public void testCoverBody() {
        assertFalse(account.allowed());
        assertEquals("are", body);
    }

    @Test
    public void testCoverAfter() {
        assertFalse(account.allowed());
        assertEquals("you", after);
    }

    @CoverBefore
    Object allowedCoverBefore(CoverArgs cargs) {
        before = "where";
        return null;
    }
    @CoverBody
    boolean allowed(CoverArgs cargs) {
        body = "are";
        return false;
    }
    @CoverAfter
    Object allowedCoverAfter(CoverArgs cargs) {
        after = "you";
        return null;
    }
}

class Account2 extends Account {

    @Cover(up = true)
    public boolean allowed() {
        return false;
    }

}

