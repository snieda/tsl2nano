package de.tsl2.nano.autotest.creator;

import static de.tsl2.nano.autotest.creator.AFunctionTester.PREF_PROPS;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import de.tsl2.nano.autotest.Construction;
import de.tsl2.nano.autotest.ValueRandomizer;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

public class AFunctionCaller implements Runnable {

	protected Object result;
	protected Object[] parameter;
	protected int cloneIndex = 0;
	protected Construction construction;
	protected Method source;
	protected transient Status status = NEW;

	static final Status NEW = new Status(StatusTyp.NEW, null, null);
	static final Status INITIALIZED = new Status(StatusTyp.INITIALIZED, null, null);
	static final Status OK = new Status(StatusTyp.OK, null, null);
	static final Status NULL_RESULT = new Status(StatusTyp.NULL_RESULT, null, null);

	AFunctionCaller(Method source) {
		this(0, source);
	}

	AFunctionCaller(int iteration, Method source) {
		this.cloneIndex = iteration;
		this.source = source;
	}
	static final <T> T def(String name, T value) {
		return Util.get(PREF_PROPS + name, value);
	}
	protected static final void log(Object txt) {
		System.out.print(txt);
	}

	protected Object[] getParameter() {
		if (parameter == null)
			try {
				parameter = createStartParameter(source.getParameterTypes());
			} catch (Exception e) {
				status = new Status(StatusTyp.PARAMETER_ERROR, e.toString(), e);
				ManagedException.forward(e);
			}
		return parameter;
	}
	
	protected String parametersAsString() {
		try {
			return Arrays.toString(getParameter());
		} catch (Exception e) {
			status = new Status(StatusTyp.PARAMETER_ERROR, e.toString(), e);
			return Arrays.toString(parameter);
		}
	}

	protected Object[] createStartParameter(Class[] arguments) {
		return ValueRandomizer.provideRandomizedObjects(cloneIndex == 0 ? 0 : 1, arguments);
	}

	@Override
	public void run() {
		run(source, getParameter());
		status = result != null ? OK : NULL_RESULT;
	}
	
	protected Object run(Method method, Object... args) {
		log(StringUtil.fixString(this.getClass().getSimpleName(), 25) + " invoking " + method.getDeclaringClass().getSimpleName() + "." + method.getName() + " with " + Arrays.toString(args));
		final Object instance = getInstance(method);
		try {
			result = method.invoke(instance, args);
			status = OK;
			return result;
		} catch (Throwable e) {
			status = new Status(StatusTyp.EXECUTION_ERROR, e.toString(), e);
			return ManagedException.forward(e);
		} finally {
			log(" -> " + status + "\n");
		}
	}

	protected Object getInstance(Method method) {
		if (Modifier.isStatic(method.getModifiers()))
			return null;
		try {
			Class<?> cls = method.getDeclaringClass();
			if (construction != null && construction.instance != null && cls.isAssignableFrom(construction.instance.getClass()))
				return construction.instance;
			else if (BeanClass.hasDefaultConstructor(cls))
				construction = new Construction(BeanClass.createInstance(cls));
			else {
				construction = ValueRandomizer.constructWithRandomParameters(cls);
			}
		} catch(Throwable ex) {
			status = new Status(StatusTyp.INSTANCE_ERROR, ex.toString(), ex);
			log(" -> " + status + "\n");
			ManagedException.forward(ex);
		}
		return construction.instance;
	}

	protected Object getResult() {
		return result;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		AFunctionCaller clone = (AFunctionCaller) super.clone();
		clone.cloneIndex++;
		return clone;
	}

	@Override
	public String toString() {
		return cloneIndex + ": " + source.getDeclaringClass().getSimpleName() + "." + source.getName() + " " + parametersAsString() + " -> " + status;
	}

	public Construction getConstruction() {
		return construction;
	}
}

enum StatusTyp {NEW, PARAMETER_UNDEFINED, PARAMETER_ERROR, INITIALIZED, INSTANCE_ERROR
	, NULL_RESULT, EXECUTION_ERROR, OK, TEST_FAILED, TESTED}
class Status {
	StatusTyp typ;
	String msg;
	Throwable err;
	
	Status(StatusTyp typ, String msg, Throwable err) {
		this.typ = typ;
		this.msg = msg;
		this.err = err;
	}
	public boolean in(StatusTyp... types) {
		return Util.in(typ, types);
	}
	
	@Override
	public String toString() {
		return typ + (err != null ? "(" + err.toString() + ")": msg != null ? "(" + msg + ")" : "");
	}
}
