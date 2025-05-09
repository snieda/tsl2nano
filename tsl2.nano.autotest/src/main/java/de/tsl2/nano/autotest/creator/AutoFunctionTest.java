package de.tsl2.nano.autotest.creator;

import java.util.Collection;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.tsl2.nano.autotest.ValueRandomizer;
import de.tsl2.nano.core.IPreferences;

/**
 * Generic Test creating test cases for each method annotated with
 * {@link InverseFunction}. For more informations, see
 * {@link InverseFunctionTester}<p/>
 * 
 * configure the test with system properties starting with "tsl2.functiontest.":
 * duplication, filter, delta.float, delta.double
 * 
 * @author Thomas Schneider
 */
@RunWith(Parameterized.class)
@FunctionTester({InverseFunctionTester.class, ExpectationFunctionTester.class})
public class AutoFunctionTest extends ADefaultAutoTester {
	AFunctionTester<?> tester;

	public AutoFunctionTest(AFunctionTester tester) {
		this.tester = tester;
	}

	@Parameters(name = "{0}")
	public static Collection<? extends AFunctionTester> parameters() {
		return AFunctionTester.prepareTestParameters(AutoFunctionTest.class.getAnnotation(FunctionTester.class));
	}

	@AfterClass
	public static void tearDownClass() {
		ValueRandomizer.reset();
		IPreferences.reset();
	}
	@Test
	public void test() {
		tester.testMe();
	}
}
