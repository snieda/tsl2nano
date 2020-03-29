package de.tsl2.nano.aspect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Ignore;
import org.junit.Test;

public class AspectCoverTest {
    @Cover(up = true)
    Account account = new Account();
    @Cover(up = true)
    IAccount iaccount = new Account(); //TODO: Proxy will not be set!!!
    Account1 accountMethodCover = new Account1();

    @Test
    public void testCoverField() {
        // does not work on standard fields (not declared as interface)
        assertFalse(accountMethodCover.allowed());
    }

    @Ignore("setting a proxy on a field does not work with aspectj...") 
    @Test
    public void testCoverInterfaceField() {
        try {
        assertFalse(iaccount.allowed());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testCoverWithMethodAnnotation() {
        // Account account = new Account() {
        // @Cover(up=true) @Override
        // public boolean allowed() {
        // return super.allowed();
        // }
        // };
        Account1 account1 = new Account1();
        assertFalse(account1.allowed());
    }

    @Test
    public void testCoverFunctionBefore() {
        Account1 account1 = new Account1();
        account1.allowed();
        assertEquals("where", account1.before);
    }

    @Test
    public void testCoverFunctionBody() {
        Account1 account1 = new Account1();
        account1.allowed();
        assertEquals("are", account1.body);
    }

    @Test
    public void testCoverFunctionAfter() {
        Account1 account1 = new Account1();
        account1.allowed();
        assertEquals("you", account1.after);
    }

}

class Account1 extends Account {
    String before;
    String body;
    String after;

    @Cover(up = true, before = CoverStatic.class, body = CoverStatic.class, after = CoverStatic.class) 
    public boolean allowed() {
        return super.allowed();
    }

}

interface CoverStatic {

    @CoverBefore
    static Object allowedCoverBefore(CoverArgs t) {
        ((Account1) t.target).before = "where";
        return null;
    }
    @CoverBody
    static boolean allowed(CoverArgs t) {
        ((Account1) t.target).body = "are";
        return false;
    }
    @CoverAfter
    static Object allowedCoverAfter(CoverArgs t) {
        ((Account1) t.target).after = "you";
        return null;
    }
}