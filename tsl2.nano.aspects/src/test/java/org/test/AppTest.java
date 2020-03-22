package org.test;

import org.junit.Test;

import static org.junit.Assert.*;

public class AppTest {
    @Cover(up = true) Account account = new Account();

    @Test
    public void testUseAspectCover() {
        assertFalse(account.allowed());
    }

    @Test
    public void testUseAspectCoverWithMethodAnnotation() {
        // Account account = new Account() {
        //     @Cover(up=true) @Override
        //     public boolean allowed() {
        //         return super.allowed();
        //     }
        // };
        Account1 account1 = new Account1();
        assertFalse(account1.allowed());
    }

    @Test
    public void testAbstractTracing() {
        for (int i = 0; i < 2; i++) {
            new Account1().XXXX();
        }
    }
    @Test
    public void testAbstractProfiling() {
        boolean result=false;
        for (int i = 0; i < 2002; i++) {
            result |= new Account1().YYYY();
        }
        assertTrue(result);
    }
    @Test
    public void testAbstractMocking() {
        for (int i = 0; i < 2; i++) {
            new Account1().ZZZZ();
        }
    }
}

class Account1 extends Account {
    public @Cover(up = true) boolean allowed() {
        return super.allowed();
    }
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
