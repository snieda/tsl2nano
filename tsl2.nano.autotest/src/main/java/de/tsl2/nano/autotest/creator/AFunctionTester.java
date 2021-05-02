package de.tsl2.nano.autotest.creator;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.tsl2.nano.autotest.ValueRandomizer;
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
public abstract class AFunctionTester<A extends Annotation>  implements Runnable, Cloneable {
	
	protected Method source;
	protected transient A def;
	protected Object result;
	protected Object[] parameter;
	protected int cloneIndex = 0;
	protected transient Object instance;

	AFunctionTester(Method source) {
		this.source = source;
	}

	protected static final void log(Object txt) {
		System.out.print(txt);
	}

	public static Collection<? extends AFunctionTester> createRunners(Class<? extends AFunctionTester> testerType, int duplication, String fuzzyFilter) {
		log("collecting tests for " + testerType.getSimpleName() + ":\n");
		Class<? extends Annotation> type = testerType.getAnnotation(FunctionType.class).value();
		Map<Double, Method> revFcts = ClassFinder.self().fuzzyFind(fuzzyFilter, Method.class, -1, type);
		Set<AFunctionTester> runners = new LinkedHashSet<>(revFcts.size());
		revFcts.values().forEach(m -> runners.add(createRunner(testerType, m)));
		duplicate(duplication, runners);
		return runners;
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

	protected static AFunctionTester createRunner(Class<? extends AFunctionTester> testerType, Method m) {
		return BeanClass.createInstance(testerType, m);
	}

	public static Object best(Object obj) {
		return obj == null || (ObjectUtil.isStandardType(obj) && ObjectUtil.isSingleValueType(obj.getClass())) && ObjectUtil.hasEquals(obj.getClass()) 
				? obj 
				: obj instanceof Serializable && !(obj instanceof StringBuilder) && !(obj instanceof StringBuffer)
					? bytes(obj) 
					: string(obj);
	}

	protected abstract Object[] getParameter();
	
	protected Object[] createStartParameter(Class[] arguments) {
		return ValueRandomizer.provideRandomizedObjects(cloneIndex == 0 ? 0 : 1, arguments);
	}

	/** to be overwritten */
	protected void doBetween() {
	}

	protected Object run(Method method, Object... args) {
		log("invoking " + method.getName() + " with " + Arrays.toString(args) + "\n");
		final Object instance = getInstance(method);
		return Util.trY(() -> method.invoke(instance, args));
	}

	private Object getInstance(Method method) {
		if (Modifier.isStatic(method.getModifiers()))
			return null;
		if (instance != null && method.getDeclaringClass().isAssignableFrom(instance.getClass()))
			return instance;
		else
			instance = BeanClass.createInstance(method.getDeclaringClass());
		return instance;
	}

	protected Object getResult() {
		return result;
	}

	public abstract Object getCompareOrigin();
	public abstract Object getCompareResult();

	protected int undefToZeroIndex(int index) {
		return index < 0 ? 0 : index;
	}

	public static byte[] bytes(Object obj) {
		return ObjectUtil.serialize(obj);
	}

	public static String string(Object obj) {
		return Util.toJson(obj);
	}

	public AFunctionTester() {
		super();
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		AFunctionTester clone = (AFunctionTester) super.clone();
		clone.cloneIndex++;
		return clone;
	}

}