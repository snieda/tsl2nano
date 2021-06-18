package de.tsl2.nano.autotest.creator;

import java.util.Collection;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.tsl2.nano.autotest.BaseTest;
import de.tsl2.nano.autotest.ValueRandomizer;

/**
 * creates test fro {@link AutoTestGenerator} calling a set of functions in the
 * classpath, storing the result as expectation ("as is") to preserve the
 * current state.
 * <p/>
 * if no results were created through AutoTestGenerator, it will be done. the
 * file {@link AutoTestGenerator#fileName} contains expectation annotations in a
 * way, so you can copy to your code on the given method.
 * 
 * @author Thomas Schneider
 */
@RunWith(Parameterized.class)
public class CurrentStatePreservationTest {
	AFunctionTester<?> tester;

	public CurrentStatePreservationTest(AFunctionTester tester) {
		this.tester = tester;
	}

	@Parameters(name = "{0}")
	public static Collection<? extends AFunctionTester> parameters() {
		return AutoTestGenerator.createExpectationTesters();
	}

	@AfterClass
	public static void tearDown() {
		ValueRandomizer.reset();
	}
	@Test
	public void test() {
		tester.testMe();
	}
}
