package de.tsl2.nano.autotest.creator;

import static de.tsl2.nano.autotest.creator.AutoTest.PARALLEL;
import static de.tsl2.nano.autotest.creator.AutoTest.TIMEOUT;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import de.tsl2.nano.autotest.Construction;
import de.tsl2.nano.autotest.ValueRandomizer;
import de.tsl2.nano.core.IPreferences;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

public class AFunctionCaller implements Runnable, Comparable<AFunctionCaller> {

	protected Object result;
	protected Object[] parameter;
	protected int cloneIndex = 0;
	protected Construction construction;
	protected Method source;
	protected transient Status status = Status.NEW;
	long duration = -1;
	long memusage = -1;
	public static final String PREF_PROPS = "tsl2.functiontest.";

	AFunctionCaller(Method source) {
		this(0, source);
	}

	AFunctionCaller(int iteration, Method source) {
		this.cloneIndex = iteration;
		this.source = source;
		this.source.setAccessible(true);
	}
	public static final <T> T def(AutoTest pref, T value) {
		return IPreferences.get(pref, value);
	}

	public static final String defs(AutoTest pref) {
		return def(pref, String.class);
	}

	public static final boolean defb(AutoTest pref) {
		return def(pref, boolean.class);
	}

	public static final int defn(AutoTest pref) {
		return def(pref, int.class);
	}

	public static final <T> T def(AutoTest pref, Class<T> type) {
		return IPreferences.get(pref, type);
	}
	public static final <T> T def(String name, T value) {
		return Util.get(PREF_PROPS + name, value);
	}
	protected static final void logd(Object txt) {
		if (LogFactory.isDebugLevel())
			log(txt);
	}
	/** this logging is only for console output - logging can be seen in autotest output files */
	protected static final void log(Object txt) {
		String s = txt.toString();
		System.out.print(s.length() > 640 ? s.substring(0, 640) : s);
	}

	protected Object[] getParameter() {
		if (parameter == null && !status.isFatal())
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
			return getParameter() != null ? Util.toJson(getParameter()) : "UNDEFINED";//Arrays.toString(getParameter());
		} catch (Exception e) {
			status = new Status(StatusTyp.PARAMETER_ERROR, e.toString(), e);
			try {
				return Util.toJson(getParameter());//Arrays.toString(parameter);
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
		ADefaultAutoTester defaultAutoTester = new ADefaultAutoTester();
		try {
			defaultAutoTester.setUp();
			run(source, getParameter());
			status = result != null ? Status.OK : Status.NULL_RESULT;
		} finally {
			defaultAutoTester.tearDown();
		}
	}
	
	public void runWithTimeout() {
		int timeout = def(TIMEOUT, int.class);
		if (timeout == -1 || !def(PARALLEL, false))
			run(); 
		else 
			ConcurrentUtil.runWithTimeout(getID(), this, timeout * 1000);
	}
	protected Object run(Method method, Object... args) {
		boolean last = LogFactory.setPrintToConsole(false);
		logd(StringUtil.fixString(this.getClass().getSimpleName(), 25) + " invoking " + method.getDeclaringClass().getSimpleName() + "." + method.getName() + " with " + StringUtil.toString(Arrays.toString(args), 80));
		final Object instance = getInstance(method);
		try {
			long start = System.currentTimeMillis();
			long mem = Profiler.getUsedMem();
			
			result = method.invoke(instance, args);
			
			duration = System.currentTimeMillis() - start;
			memusage = Profiler.getUsedMem() - mem;
			status = Status.OK;
			return result;
		} catch (Throwable e) {
			status = new Status(StatusTyp.EXECUTION_ERROR, e.toString(), e);
			if (e instanceof Error)
				Util.trY( () -> FileUtil.writeBytes((this.toString() + "\nSTACKTRACE:\n" + ManagedException.toStringCause(e)).getBytes(), AutoTestGenerator.fileName + "hard-errors.txt", true), false);
			return ManagedException.forward(e);
		} finally {
//			Thread.currentThread().notifyAll(); // you should't call notifyAll() on a Thread! see Thread.join() description
			logd(" -> " + status + "\n");
			LogFactory.setPrintToConsole(last);
		}
	}

	protected Object getInstance(Method method) {
		if (Modifier.isStatic(method.getModifiers()))
			return null;
		try {
			Class<?> cls = method.getDeclaringClass();
			if (construction != null && construction.instance != null && cls.isAssignableFrom(construction.instance.getClass()))
				return construction.instance;
			else {
				if (!status.isFatal()) {
					construction = ValueRandomizer.constructWithRandomParameters(cls);
				} else {
					throw status.err;
				}
			}
		} catch(Throwable ex) {
			status = new Status(StatusTyp.INSTANCE_ERROR, ex.toString(), ex);
			logd(" -> " + status + "\n");
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
	public boolean equals(Object obj) {
		return obj instanceof AFunctionCaller && getID().equals(((AFunctionCaller)obj).getID());
	}
	
	@Override
	public int hashCode() {
		return getID().hashCode();
	}
	
	@Override
	public int compareTo(AFunctionCaller o) {
		return toString().compareTo(o.toString());
	}
	@Override
	public String toString() {
		return getID() + " -> " + status;
	}

	public String getID() {
		return cloneIndex + ": " + getFunctionDescription() + " " + parametersAsString();
	}

	public String getFunctionDescription() {
		return source.getDeclaringClass().getSimpleName() + "." + source.getName();
	}

	public Construction getConstruction() {
		return construction;
	}
}

enum StatusTyp {
	NEW(0), FUNC_SYNTHETIC(1), FUNC_WITHOUT_INTPUT(1), FUNC_WITHOUT_OUTPUT(1), FUNC_COMPLEX_INPUT(1)
	, PARAMETER_UNDEFINED(-9), PARAMETER_ERROR(-9), INITIALIZED(2), INSTANCE_ERROR(-9)
	, NULL_RESULT(1), TYPECONVERSION_CHECK_FAIL(1), EXECUTION_ERROR(-1), OK(2), STORE_ERROR(-1), TEST_FAILED(-3),
	TESTED(4);
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
	static final Status FUNC_SYNTHETIC = new Status(StatusTyp.FUNC_SYNTHETIC);
	static final Status FUNC_WITHOUT_INPUT = new Status(StatusTyp.FUNC_WITHOUT_INTPUT);
	static final Status FUNC_COMPLEX_INPUT = new Status(StatusTyp.FUNC_COMPLEX_INPUT);
	static final Status FUNC_WITHOUT_OUTPUT = new Status(StatusTyp.FUNC_WITHOUT_OUTPUT);
	static final Status TYPECONVERSION_CHECK_FAIL = new Status(StatusTyp.TYPECONVERSION_CHECK_FAIL);

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
	
	public boolean isFatal() {
		return typ.level == -9;
	}
	
	public boolean isRefused() {
		return typ.level == 1;
	}

	@Override
	public String toString() {
		return typ + (err != null ? "(" + err.toString() + ")": msg != null ? "(" + msg + ")" : "");
	}
}
