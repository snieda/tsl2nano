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
	protected transient Status status = Status.NEW;

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
			try {
				return Arrays.toString(parameter);
			} catch (Exception e1) {
				return "UNRESOLVABLE";
			}
		}
	}

	protected Object[] createStartParameter(Class[] arguments) {
		return ValueRandomizer.provideRandomizedObjects(cloneIndex == 0 ? 0 : 1, arguments);
	}

	@Override
	public void run() {
		run(source, getParameter());
		status = result != null ? Status.OK : Status.NULL_RESULT;
	}
	
	protected Object run(Method method, Object... args) {
		log(StringUtil.fixString(this.getClass().getSimpleName(), 25) + " invoking " + method.getDeclaringClass().getSimpleName() + "." + method.getName() + " with " + Arrays.toString(args));
		final Object instance = getInstance(method);
		try {
			result = method.invoke(instance, args);
			status = Status.OK;
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

enum StatusTyp {
	NEW(0), FUNC_WITHOUT_INTPUT(1), FUNC_WITHOUT_OUTPUT(1), FUNC_COMPLEX_INPUT(1)
	, PARAMETER_UNDEFINED(-1), PARAMETER_ERROR(-1), INITIALIZED(2), INSTANCE_ERROR(-1)
	, NULL_RESULT(1), EXECUTION_ERROR(-1), OK(2), STORE_ERROR(-1), TEST_FAILED(-3), TESTED(4);
	int level; //to categorize a state
	StatusTyp(int level) {this.level = level;};
}
class Status {
	StatusTyp typ;
	String msg;
	Throwable err;
	
	static final Status NEW = new Status(StatusTyp.NEW);
	static final Status INITIALIZED = new Status(StatusTyp.INITIALIZED);
	static final Status OK = new Status(StatusTyp.OK);
	static final Status NULL_RESULT = new Status(StatusTyp.NULL_RESULT);
	static final Status FUNC_WITHOUT_INPUT = new Status(StatusTyp.FUNC_WITHOUT_INTPUT);
	static final Status FUNC_COMPLEX_INPUT = new Status(StatusTyp.FUNC_COMPLEX_INPUT);
	static final Status FUNC_WITHOUT_OUTPUT = new Status(StatusTyp.FUNC_WITHOUT_OUTPUT);

	public Status(StatusTyp typ) {
		this(typ, null, null);
	}
	Status(StatusTyp typ, String msg, Throwable err) {
		this.typ = typ;
		this.msg = msg;
		this.err = err;
	}
	public boolean in(StatusTyp... types) {
		return Util.in(typ, types);
	}

	public boolean isError() {
		return typ.level < 0;
	}
	
	public boolean isRefused() {
		return typ.level == 1;
	}
	
	@Override
	public String toString() {
		return typ + (err != null ? "(" + err.toString() + ")": msg != null ? "(" + msg + ")" : "");
	}
}
