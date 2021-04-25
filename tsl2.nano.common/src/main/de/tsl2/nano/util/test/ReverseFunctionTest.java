package de.tsl2.nano.util.test;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static de.tsl2.nano.util.test.ReverseFunctionTester.*;

/**
 * Generic Test creating test cases for each method annotated with
 * {@link ReverseFunction}. For more informations, see
 * {@link ReverseFunctionTester}
 * 
 * @author Thomas Schneider
 */
@RunWith(Parameterized.class)
public class ReverseFunctionTest {
	ReverseFunctionTester tester;

	public ReverseFunctionTest(ReverseFunctionTester tester) {
		this.tester = tester;
	}

	@Parameters(name = "{0}")
	public static Collection<ReverseFunctionTester> parameters() {
		return ReverseFunctionTester.createRunners(2);
	}

	@Test
	public void testReverseFunctions() {
		tester.run();
		assertEquals(best(tester.getCompareOriginObject()), best(tester.getCompareReverseObject()));
	}
}
