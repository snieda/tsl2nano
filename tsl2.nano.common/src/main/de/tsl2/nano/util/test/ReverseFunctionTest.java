package de.tsl2.nano.util.test;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static de.tsl2.nano.util.test.ReverseFunctionTester.*;

@RunWith(Parameterized.class)
public class ReverseFunctionTest {
	ReverseFunctionTester tester;
	
	public ReverseFunctionTest(ReverseFunctionTester tester) {
		this.tester = tester;
	}

	@Parameters(name="{0}")
	public static Collection<ReverseFunctionTester> parameters() {
		return ReverseFunctionTester.createRunners(9);
	}
	
	@Test
	public void testReverseFunctions() {
		tester.run();
		assertEquals(bytes(tester.getCompareOriginObject()), bytes(tester.getCompareReverseObject()));
	}
}
