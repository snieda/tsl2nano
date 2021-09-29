package de.tsl2.nano.autotest.creator;

import java.security.Permission;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.tsl2.nano.autotest.BaseTest;
import de.tsl2.nano.core.IPreferences;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;

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
		IPreferences.reset();
		// the string.set uses german city names
		Locale.setDefault(Locale.GERMANY);
		
		System.setProperty("tsl2.nano.logfactory.off", "true");
		System.setProperty("tsl2.nano.test", "true");
		System.setProperty("tsl2.json.recursive", "false");
//		System.setProperty("tsl2.functiontest.testneverfail", "true");
		System.setProperty("tsl2.functiontest.donttest", "true");
		
		BaseTest.useTargetDir();
		if (Boolean.getBoolean("tsl2.functiontest.forbidSystemExit"))
			forbidSystemExit();
		BeanClass.callStatic("de.tsl2.nano.util.autotest.creator.AllAutoTests", "init");
		return Arrays.asList();
	}

	@Test public void nothing() {}

	public static void set(boolean on, Enum...properties) {
		Arrays.stream(properties).forEach( e -> set(e, on));
	}

	public static void set(boolean on, String...properties) {
		Arrays.stream(properties).forEach( p -> System.setProperty(AFunctionTester.PREF_PROPS + p, String.valueOf(on)));
	}

	public static void set(Enum p, Object value) {
		IPreferences.set(p, value);
	}
	public static void set(String property, Object value) {
		System.setProperty(AFunctionTester.PREF_PROPS + property, value.toString());
	}
	
	public static String matchPackage(Class...classes) {
		return matchPackage(true, classes);
	}
	public static String matchPackage(boolean loadAllClassesInPackage, Class...classes) {
		StringBuilder buf = new StringBuilder(".*(");
		for (int i = 0; i < classes.length; i++) {
			if (loadAllClassesInPackage)
				ClassFinder.getClassesInPackage(classes[i].getPackage().getName(), null);
			buf.append(classes[i].getPackage().getName() + (i < classes.length - 1 ? "|" : ""));
		}
		return buf.append(").*").toString();
	}

	public static void forbidSystemExit() {
		try {
			System.setSecurityManager(new SecurityManager() {
				@Override
				public void checkPermission(Permission perm) {
					//ALL PERMISSIONS! (on test)
				}
				@Override
				public void checkPermission(Permission perm, Object context) {
					//ALL PERMISSIONS! (on test)
				}
				@Override
				public void checkExit(int status) {
					StackTraceElement caller = Thread.currentThread().getStackTrace()[4];
					if (!caller.toString().contains("surefire"))
						throw new IllegalStateException("systemexit forbidden:" + status + "(" + caller + ")");
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
