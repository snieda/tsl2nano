package de.tsl2.nano.autotest.creator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.Util;

/**
 * base class to create automated parameterized test through the information of an annotation.
 * 
 * @param <A> Function Test Annotation
 * 
 * @author ts
 */
@SuppressWarnings("rawtypes")
public abstract class AFunctionTester<A extends Annotation> extends AFunctionCaller  implements Runnable, Cloneable {
	protected transient A def;
	public static final String PREF_PROPS = "tsl2.functiontest.";
	private static final float DELTA_FLOAT = Util.get(PREF_PROPS + "delta.float", 0.0000000001f);
	private static final float DELTA_DOUBLE = Util.get(PREF_PROPS + "delta.double", 0.0000000001f);

	public AFunctionTester(Method source) {
		super(source);
	}

	public AFunctionTester(int iteration, Method source) {
		super(iteration, source);
	}

	public static Collection<? extends AFunctionTester> createRunners(Class<? extends AFunctionTester> testerType, int duplication, String filter) {
		log("collecting tests for " + testerType.getSimpleName() + ":\n");
		Class<? extends Annotation> type = testerType.getAnnotation(FunctionType.class).value();
		List<Method> revFcts = ClassFinder.self().find(filter, Method.class, -1, type);
		Set<AFunctionTester> runners = new LinkedHashSet<>(revFcts.size());
		revFcts.forEach(m -> runners.add(createRunner(testerType, m)));
		duplicate(duplication, runners);
		return runners;
	}

	protected static AFunctionTester<?> createRunner(Class<? extends AFunctionTester> testerType, Method m) {
		return BeanClass.createInstance(testerType, m);
	}

	private static void duplicate(int duplication, Set<AFunctionTester> runners) {
		log("duplicating " + runners.size() + " runners " + duplication + " times\n");
		ArrayList<AFunctionTester> clones = new ArrayList<>();
		for (int i = 0; i < duplication; i++) {
			clones.clear();
			for (AFunctionTester tester : runners) {
				clones.add((AFunctionTester) Util.trY(() -> tester.clone()));
			}
			runners.addAll(clones);
		}
	}

	public static Object best(Object obj) {
		return obj == null || (ObjectUtil.isStandardType(obj) || ObjectUtil.isSingleValueType(obj.getClass())) && ObjectUtil.hasEquals(obj.getClass()) 
				? obj 
				: obj instanceof Serializable && !obj.getClass().isAnonymousClass() && !(obj instanceof StringBuilder) && !(obj instanceof StringBuffer)
					? bytes(obj) 
					: string(obj);
	}

	/** to be overwritten */
	protected void doBetween() {
	}

	public abstract Object getCompareOrigin();
	public abstract Object getCompareResult();
	public abstract Object getExpectFail();

	protected int undefToZeroIndex(int index) {
		return index < 0 ? 0 : index;
	}

	public static byte[] bytes(Object obj) {
		return ObjectUtil.serialize(obj);
	}

	public static String string(Object obj) {
		return Util.toJson(obj);
	}

	public static Collection<? extends AFunctionTester> prepareTestParameters(FunctionTester types) {
		Collection<AFunctionTester> runners = new LinkedHashSet<>();
		for (int i = 0; i < types.value().length; i++) {
			runners.addAll(AFunctionTester.createRunners(types.value()[i], def("duplication", 3), def("filter", "")));
		}
		return runners;
	}

	public void testMe() {
		try {
			long start = System.currentTimeMillis();
			run();
			checkFail();
			if (def("filter.nullresults", false))
				assertTrue(getCompareOrigin() != null || getCompareResult() != null);

			Object o1 = best(getCompareOrigin());
			Object o2 = best(getCompareResult());
			if (o1 != null && o1.getClass().isArray())
				assertAnyArrayEquals(o1, o2);
			else {
				assertEquals(toString(), o1, o2);
			}
			status = new Status(StatusTyp.TESTED, (System.currentTimeMillis() - start) / 1000 + " sec", null);
			log(this + "\n");
		} catch (Exception | AssertionError e) {
			boolean shouldFailError = false;
			try {
				if (shouldFail(e))
					return;
			} catch (Exception | AssertionError e1) {
				status = new Status(StatusTyp.TEST_FAILED, e1.toString(), e1);
				shouldFailError = true;
			}
			if (!shouldFailError)
				status = new Status(StatusTyp.TEST_FAILED, e.toString(), e);
			log(" -> " + status + "\n");
			if (!Util.get(PREF_PROPS + "testneverfail", false))
				ManagedException.forward(e);
			else
				log("ERROR (testneverfail=true): " + status);
		}
	}

	private boolean checkFail() {
		return shouldFail(status.err);
	}
	private boolean shouldFail(Throwable error) {
		if (getExpectFail() != null) {
			if (error == null)
				fail("test should fail with " + getExpectFail() + " but has result: " + getResult());
			else if (!getExpectFail().toString().contains(error.toString().substring(0, Math.min(200, error.toString().length()))))
				fail("test should fail with " + getExpectFail() + " but failed with: " + error);
			return true;
		}
		return false;
	}

	protected void assertAnyArrayEquals(Object o1, Object o2) {
		if (o1.getClass().getComponentType() == byte.class)
			assertArrayEquals(toString(), (byte[])o1, (byte[])o2);
		else if (o1.getClass().getComponentType() == char.class)
			assertArrayEquals(toString(), (char[])o1, (char[])o2);
		else if (o1.getClass().getComponentType() == short.class)
			assertArrayEquals(toString(), (short[])o1, (short[])o2);
		else if (o1.getClass().getComponentType() == int.class)
			assertArrayEquals(toString(), (int[])o1, (int[])o2);
		else if (o1.getClass().getComponentType() == float.class)
			assertArrayEquals(toString(), (float[])o1, (float[])o2, DELTA_FLOAT);
		else if (o1.getClass().getComponentType() == double.class)
			assertArrayEquals(toString(), (double[])o1, (double[])o2, DELTA_DOUBLE);
		else
			assertArrayEquals(toString(), (Object[])o1, (Object[])o2);
	}
}