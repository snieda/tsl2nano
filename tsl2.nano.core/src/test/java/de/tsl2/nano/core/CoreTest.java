package de.tsl2.nano.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.Policy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
import de.tsl2.nano.core.util.ValueSet;

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
		assertEquals("1:2:3.0", StringUtil.toFormattedString(asList, -1, false, ":"));

		asList = new ArrayList<Object>(asList); //java.util.ArrayList
		assertEquals("[1, 2, 3.0]", StringUtil.toString(asList, -1));
		assertEquals("1:2:3.0", StringUtil.toFormattedString(asList, -1, false, ":"));
	}
	
	@Test
	public void testValueSetSimple() {
		Date date = new Date();
		Object[] mm = new Object[] {"Madl", "Marion", date};
		ValueSet<Person, Object> p = new ValueSet<>(Person.class, mm);
		assertArrayEquals(mm, p.valuesOf(p.names()));
	}		
	@Test
	public void testValueSetExtended() {
		ValueSet<Person, Object> p = new ValueSet(Person.class, new HashMap<>()) {
			public String toString() {
				return def(Person.Name, "Madl"); 
			}
		};
		assertEquals("Madl", p.toString());
	}		
	@Test
	public void testValueSet() {
		Date date = new Date();
		ValueSet<Person, Object> p = new ValueSet<>(Person.class);
//		p.set(e(Person.Name, "muster"), 
//				e(Person.Vorname, "max"),
//				e(Person.Geburtsdatum, date));
		p.set(Person.Name, "muster"); 
		p.set(Person.Vorname, "max");
		p.set(Person.Geburtsdatum, date);
		p.on(Person.Name, (e, n) -> p.set(e, StringUtil.toFirstUpper((String) n)));
		p.transform(Person.Vorname, n -> StringUtil.toFirstUpper(n.toString()));
		assertEquals(date, (Date)p.get(Person.Geburtsdatum));
		
		Person[] members = p.names();
		Object[] expected = new Object[] {"Muster", "Max", date};
		assertArrayEquals(expected, p.stream().map(m -> p.get(m)).toArray());
		assertArrayEquals(expected, p.valuesOf(p.names()));
	}
}
enum Person {Name, Vorname, Geburtsdatum}; 
