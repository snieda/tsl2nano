package de.tsl2.nano.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.Policy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.CallingPath;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;

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
    public void testReflection() {
    	assertTrue(((Long)BeanClass.getStatic(BeanClass.class, "serialVersionUID")).longValue() > 0);
    }
    
	@Test
	public void testAppLoaderMainNoParameter() {
		// on empty, don't throw an exception
		String[] args = new String[] {};
		AppLoader.main(args);
	}

	@Test
	public void testAppLoaderMainOneParameter() {
		// only one parameter
		String[] args = new String[] { "test" };
		AppLoader.main(args);
	}

	@Test
	public void testAppLoaderMainStandard() {
		// standard call
		Thread.currentThread().setContextClassLoader(CoreTest.class.getClassLoader());
		String[] args = new String[] { this.getClass().getName(), "sagEs", "Hello World" };
		AppLoader.main(args);
		assertTrue("" + aufrufe, aufrufe > 0);
	}
    
    public static void sagEs(String[] wasdenn) {
    	System.out.println(wasdenn[0]);
    	aufrufe++;
	}
	
	@Test
	public void testCallingPath() {
		Map args = MapUtil.asMap("0", 0, "2", 2, "3", 3);
		Object result = CallingPath.eval("test", "substring(0, 3).substring(2)", args);
		assertEquals("s", result);
	}
	
	@Test
	public void testStringUtilToString() {
		Object[] arr = new Object[] {"1", 2, 3.};
		List<Object> asList = Arrays.asList(arr); //Arrays.ArrayList
		assertEquals("[1, 2, 3.0]", StringUtil.toString(asList, -1));
		assertEquals("1:2:3.0:", StringUtil.toFormattedString(asList, -1, false, ":"));

		asList = new ArrayList<Object>(asList); //java.util.ArrayList
		assertEquals("[1, 2, 3.0]", StringUtil.toString(asList, -1));
		assertEquals("1:2:3.0:", StringUtil.toFormattedString(asList, -1, false, ":"));
	}
}
