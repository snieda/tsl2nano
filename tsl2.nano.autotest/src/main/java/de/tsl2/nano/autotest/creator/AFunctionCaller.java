package de.tsl2.nano.autotest.creator;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import de.tsl2.nano.autotest.ValueRandomizer;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.Util;

public class AFunctionCaller implements Runnable {

	protected Object result;
	protected Object[] parameter;
	protected int cloneIndex = 0;
	protected transient Object instance;
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
	protected static final void log(Object txt) {
		System.out.print(txt);
	}

	protected Object[] getParameter() {
		if (parameter == null)
			parameter = createStartParameter(source.getParameterTypes());
		return parameter;
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
		log("invoking " + method.getDeclaringClass().getSimpleName() + "." + method.getName() + " with " + Arrays.toString(args) + "\n");
		final Object instance = getInstance(method);
		try {
			return result = method.invoke(instance, args);
		} catch (Exception e) {
			status = new Status(StatusTyp.EXECUTION_ERROR, e.toString(), e);
			return ManagedException.forward(e);
		}
	}

	protected Object getInstance(Method method) {
		if (Modifier.isStatic(method.getModifiers()))
			return null;
		try {
			if (instance != null && method.getDeclaringClass().isAssignableFrom(instance.getClass()))
				return instance;
			else
				instance = BeanClass.createInstance(method.getDeclaringClass());
		} catch(Exception ex) {
			status = new Status(StatusTyp.INSTANCE_ERROR, ex.toString(), ex);
			ManagedException.forward(ex);
		}
		return instance;
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
	public boolean is(StatusTyp typ) {
		return this.typ.equals(typ);
	}
	
	@Override
	public String toString() {
		return Util.toJson(this);
	}
}
