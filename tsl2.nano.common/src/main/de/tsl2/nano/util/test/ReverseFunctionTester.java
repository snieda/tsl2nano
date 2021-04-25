package de.tsl2.nano.util.test;

import java.io.Serializable;
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
 * <p/>
 * to do a multiple random test on each function, give a duplication parameter >
 * 0.
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
	private int cloneIndex = 0;
	
	public ReverseFunctionTester(Method source) {
		this.source = source;
		log("\t" + getClass().getSimpleName() + ": " + source.getName() + " -> " + def().methodName() + "\n");
	}

	private static final void log(Object txt) {
		System.out.print(txt);
	}

	private final ReverseFunction def() {
		return source.getAnnotation(ReverseFunction.class);
	}

	public static Collection<ReverseFunctionTester> createRunners(int duplication) {
		log("collecting reverse function tests:\n");
		Map<Double, Method> revFcts = ClassFinder.self().fuzzyFind("", Method.class, -1, ReverseFunction.class);
		ArrayList<ReverseFunctionTester> runners = new ArrayList<>(revFcts.size());
		revFcts.values().forEach(m -> runners.add(createRunner(m)));
		duplicate(duplication, runners);
		return runners;
	}

	private static void duplicate(int duplication, ArrayList<ReverseFunctionTester> runners) {
		ArrayList<ReverseFunctionTester> clones = new ArrayList<>();
		for (int i = 0; i < duplication; i++) {
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
		final Class declCls = funcAnn.declaringType().equals(Object.class) ? source.getDeclaringClass()
				: funcAnn.declaringType();
		return Util.trY(() -> declCls.getMethod(funcAnn.methodName(), funcAnn.parameters()));
	}

	protected Object[] getParameter() {
		if (parameter == null)
			parameter = createStartParameter(def().parameters());
		return parameter;
	}

	protected Object[] getReverseParameter() {
		if (parameterReverse == null) {
			parameterReverse = createStartParameter(source.getParameterTypes());
			int[] bind = def().bindParameterIndexesOnReverse();
			for (int i = 0; i < bind.length; i++) {
				parameterReverse[i] = bind[i] == -2 
					? parameterReverse[i] 
						: bind[i] == -1 
							? getResult() 
								: parameter[bind[i]];
			}
		}
		return parameterReverse;
	}

	protected static Object[] createStartParameter(Class[] arguments) {
		return ValueRandomizer.provideRandomizedObjects(1, arguments);
	}

	@Override
	public void run() {
		Method sourceMethod = getFunction(def());
		log("==> doing test " + cloneIndex + " step 1: ");
		result = run(sourceMethod, getParameter());
		doBetween();
		log("==> doing test " + cloneIndex + " step 2: ");
		reverseResult = run(source, getReverseParameter());
	}

	/** to be overwritten */
	protected void doBetween() {
	}

	protected Object run(Method method, Object... args) {
		log("invoking " + method.getName() + " with " + Arrays.toString(args) + "\n");
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
		ReverseFunction def = def();
		return def.compareParameterIndex() < 0  && !getFunction(def).getReturnType().equals(Void.class) ? result : parameter[undefToZeroIndex(def.compareParameterIndex())];
	}

	public Object getCompareReverseObject() {
		ReverseFunction def = def();
		return def.compareReverseParameterIndex() < 0 && !source.getReturnType().equals(Void.class) ? reverseResult : parameterReverse[undefToZeroIndex(def.compareReverseParameterIndex())];
	}

	private int undefToZeroIndex(int index) {
		return index < 0 ? 0 : index;
	}

	public static Object best(Object obj) {
		return obj == null || ObjectUtil.isStandardType(obj) ? obj : obj instanceof Serializable ? bytes(obj) : string(obj);
	}

	public static byte[] bytes(Object obj) {
		return ObjectUtil.serialize(obj);
	}

	public static String string(Object obj) {
		return Util.toJson(obj);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		ReverseFunctionTester clone = (ReverseFunctionTester) super.clone();
		clone.cloneIndex++;
		return clone;
	}
	@Override
	public String toString() {
		return cloneIndex + ": " + def().methodName() + "->" + source.getName() + " " + Arrays.toString(getParameter());
	}
}
