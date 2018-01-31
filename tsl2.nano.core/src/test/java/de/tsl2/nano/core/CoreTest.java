package de.tsl2.nano.core;

import static org.junit.Assert.assertTrue;

import java.security.Policy;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.ENVTestPreparation;

public class CoreTest implements ENVTestPreparation {
	static int aufrufe = 0;
	
	@BeforeClass
	public static void setUp() {
		ENVTestPreparation.setUp("core", false);
	}

	@AfterClass
	public static void tearDown() {
		ENVTestPreparation.tearDown();
	}

    @Test
    public void testSecurity() {
        System.out.println(System.getSecurityManager());
        System.out.println(Policy.getPolicy());
        //setze SecurityManager/Policy zurück
        BeanClass.call(AppLoader.class, "noSecurity", false);
        assertTrue(System.getSecurityManager() == null);
        assertTrue(Policy.getPolicy().toString().contains("all-permissions"));
    }
    
    @Test
    public void testAppLoaderMain() {
    	//on empty, don't throw an exception
    	String[] args = new String[] {};
		AppLoader.main(args );

		//only one parameter
		args = new String[] {"test"};
		AppLoader.main(args );
		
		// standard call
		args = new String[] {this.getClass().getName(), "sagEs", "Hello World"};
		AppLoader.main(args );
		assertTrue("" + aufrufe, aufrufe > 0);
    }
    
    public static void sagEs(String[] wasdenn) {
    	System.out.println(wasdenn[0]);
    	aufrufe++;
    }
}
