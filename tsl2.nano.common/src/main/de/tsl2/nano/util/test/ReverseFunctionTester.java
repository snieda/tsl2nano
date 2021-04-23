package de.tsl2.nano.util.test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.Util;

/**
 * collects all annotated methods with {@link ReverseFunction} and prepares all
 * start parameters (randomized) to be called on unit testing. provides
 * comparing between origin value and reverse function result.
 * 
 * @author Thomas Schneider
 */
public class ReverseFunctionTester implements Runnable, Cloneable {

	/** reverse function */
	private Method source;
	private Object result;
	private Object reverseResult;
	private Object[] parameter;
	private Object[] parameterReverse;

	public ReverseFunctionTester(Method source) {
		this.source = source;
		log(getClass().getSimpleName() + ": " + source.getName() + " -> " + getDefinition(source).methodName());
	}

	private void log(Object txt) {
		System.out.println(txt);
	}

	private static final ReverseFunction getDefinition(Method source) {
		return source.getAnnotation(ReverseFunction.class);
	}

	public static Collection<ReverseFunctionTester> createRunners(int duplication) {
		Map<Double, Method> revFcts = ClassFinder.self().fuzzyFind("", Method.class, -1, ReverseFunction.class);
		ArrayList<ReverseFunctionTester> runners = new ArrayList<>(revFcts.size());
		revFcts.values().forEach(m -> runners.add(createRunner(m)));
		duplicate(duplication, runners);
		return runners;
	}

	private static void duplicate(int duplication, ArrayList<ReverseFunctionTester> runners) {
		ArrayList<ReverseFunctionTester> clones = new ArrayList<>();
		for (int i=0; i<duplication; i++) {
			clones.clear();
			for (ReverseFunctionTester tester : runners) {
				clones.add((ReverseFunctionTester) Util.trY(() -> tester.clone()));
			}
			runners.addAll(clones);
		}
	}

	protected static ReverseFunctionTester createRunner(Method m) {
		return new ReverseFunctionTester(m);
	}

	protected Method getFunction(ReverseFunction funcAnn) {
		final Class declCls = funcAnn.declaringType().equals(Object.class) ? source.getDeclaringClass() : funcAnn.declaringType();
		return Util.trY(() -> declCls.getMethod(funcAnn.methodName(), funcAnn.parameters()));
	}

	protected Object[] getParameter() {
		if (parameter == null)
			parameter = createStartParameter(getDefinition(source).parameters());
		return parameter;
	}
	
	protected static Object[] createStartParameter(Class[] arguments) {
		return ValueRandomizer.provideRandomizedObjectArray(arguments);
	}

	@Override
	public void run() {
		ReverseFunction funcAnn = getDefinition(source);
		Method sourceMethod = getFunction(funcAnn);
		log("==> doing test step 1");
		result = run(sourceMethod, getParameter());
		doBetween();
		parameterReverse = createStartParameter(source.getParameterTypes());
		log("==> doing test step 2");
		reverseResult = run(source, parameterReverse);
	}

	/** to be overwritten */
	protected void doBetween() {
	}

	protected Object run(Method method, Object... args) {
		log("invoking " + method.getName() + " with " + Arrays.toString(args));
		boolean isStatic = Modifier.isStatic(method.getModifiers());
		final Object instance = isStatic ? null : BeanClass.createInstance(method.getDeclaringClass());
		return Util.trY(() -> method.invoke(instance, args));
	}

	protected Object getResult() {
		return result;
	}

	protected Object getReverseResult() {
		return reverseResult;
	}

	public Object getCompareOriginObject() {
		ReverseFunction def = getDefinition(source);
		return def.compareParameterIndex() < 0 ? result : parameter[def.compareParameterIndex()];
	}

	public Object getCompareReverseObject() {
		ReverseFunction def = getDefinition(source);
		return def.compareParameterIndex() < 0 ? result : parameterReverse[def.compareReverseParameterIndex()];
	}

	public static byte[] bytes(Object obj) {
		return ObjectUtil.serialize(obj);
	}

	public static String string(Object obj) {
		return Util.toJson(obj);
	}
	@Override
	public String toString() {
		return getDefinition(source).methodName() + "->" + source.getName() + " "  + Arrays.toString(getParameter());
	}
}
