package de.tsl2.nano.autotest.creator;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.tsl2.nano.core.cls.BeanClass;

/**
 * This parameterized test class is a workaround on test suites having parameterized tests that have to be initialized from outside.<p/>
 * The external application, using this framworks auto test classes, should be implemented like this:
 * <pre>
 *	@RunWith(Suite.class)
 *	@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
 *	public class AllAutoTests {
 *		public static void init() {
 *			System.setProperty("tsl2.functiontest.filter", MyClassToTest.class.getPackage().getName());
 *		}
 *	}
 * </pre>
 * 
 * 
 * @author Thomas Schneider
 */
@RunWith(Parameterized.class)
public class InitAllAutoTests {

	@Parameters
	public static Collection<?> parameters() {
		BeanClass.callStatic("de.tsl2.nano.util.autotest.creator.AllAutoTests", "init");
		return Arrays.asList();
	}

	@Test public void nothing() {}
	
	public static String matchPackage(Class...classes) {
		StringBuilder buf = new StringBuilder(".*(");
		for (int i = 0; i < classes.length; i++) {
			buf.append(classes[i].getPackage().getName() + (i < classes.length - 1 ? "|" : ""));
		}
		return buf.append(").*").toString();
	}
}
