package de.tsl2.nano.util.test.inverse;

import static de.tsl2.nano.util.test.inverse.AFunctionTester.best;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.tsl2.nano.core.util.Util;

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
public class AutoFunctionTest {
	AFunctionTester<?> tester;

	private static final String PREF_PROPS = "tsl2.functiontest.";
	private static final float DELTA_FLOAT = Util.get(PREF_PROPS + "delta.float", 0.0000000001f);
	private static final float DELTA_DOUBLE = Util.get(PREF_PROPS + "delta.double", 0.0000000001f);
	
	public AutoFunctionTest(AFunctionTester tester) {
		this.tester = tester;
	}

	@Parameters(name = "{0}")
	public static Collection<? extends AFunctionTester> parameters() {
		FunctionTester types = AutoFunctionTest.class.getAnnotation(FunctionTester.class);
		Collection<AFunctionTester> runners = new LinkedHashSet<>();
		for (int i = 0; i < types.value().length; i++) {
			runners.addAll(AFunctionTester.createRunners(
						types.value()[i],
						Util.get(PREF_PROPS + "duplication", 3),
						Util.get(PREF_PROPS + "filter", "")));
		}
		return runners;
	}

	@Test
	public void testFunctions() {
		tester.run();
		assertTrue(tester.getCompareOrigin() != null || tester.getCompareResult() != null);

		Object o1 = best(tester.getCompareOrigin());
		Object o2 = best(tester.getCompareResult());
		if (o1 != null && o1.getClass().isArray())
			assertAnyArrayEquals(o1, o2);
		else {
			assertEquals(o1, o2);
		}
	}

	private void assertAnyArrayEquals(Object o1, Object o2) {
		if (o1.getClass().getComponentType() == byte.class)
			assertArrayEquals((byte[])o1, (byte[])o2);
		else if (o1.getClass().getComponentType() == char.class)
			assertArrayEquals((char[])o1, (char[])o2);
		else if (o1.getClass().getComponentType() == short.class)
			assertArrayEquals((short[])o1, (short[])o2);
		else if (o1.getClass().getComponentType() == int.class)
			assertArrayEquals((int[])o1, (int[])o2);
		else if (o1.getClass().getComponentType() == float.class)
			assertArrayEquals((float[])o1, (float[])o2, DELTA_FLOAT);
		else if (o1.getClass().getComponentType() == double.class)
			assertArrayEquals((double[])o1, (double[])o2, DELTA_DOUBLE);
		else
			assertArrayEquals((Object[])o1, (Object[])o2);
	}
}
