package de.tsl2.nano.autotest.creator;

import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.tsl2.nano.autotest.ValueRandomizer;

/**
 * creates test from {@link AutoTestGenerator} calling a set of functions in the
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
public class CurrentStatePreservationTest extends ADefaultAutoTester {
	AFunctionTester<?> tester;

	public CurrentStatePreservationTest(AFunctionTester tester) {
		this.tester = tester;
	}

	@Parameters(name = "{0}")
	public static Collection<? extends AFunctionTester> parameters() {
		return new AutoTestGenerator().createExpectationTesters();
	}

	@AfterClass
	public static void tearDownClass() {
		ValueRandomizer.reset();
		AutoTestGenerator.progress = null;
	}
	
	@After
	public void tearDown() {
		tester = null;
	}
	
	@Test
	public void test() {
		tester.testMe();
	}
}
