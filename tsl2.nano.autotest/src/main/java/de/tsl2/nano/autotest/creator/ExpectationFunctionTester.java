package de.tsl2.nano.autotest.creator;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import de.tsl2.nano.autotest.Construction;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

@FunctionType(Expectations.class)
public class ExpectationFunctionTester extends AFunctionTester<Expectations> {

	private static final String NOT_DEFINED = "NOT DEFINED";
	private Expect expect;

	public ExpectationFunctionTester(Method source, Expectations externalExpecations) {
		this(0, source, externalExpecations);
	}
	public ExpectationFunctionTester(int iteration, Method source, Expectations externalExpecations) {
		super(iteration, source);
		def = externalExpecations;
	}

	public ExpectationFunctionTester(Method source) {
		super(source);
		def = source.getAnnotation(Expectations.class);
		log("\t" + source.getDeclaringClass().getSimpleName() + ": " + source.getName() + " -> " + Arrays.toString(def.value()) + "\n");
	}

	@Override
	protected Object[] getParameter() {
		if (parameter == null) {
			Expect[] expectations = def.value();
			//if generated ExpectationsImpl is used, there is only one @Expect!
			boolean annotated = !def.getClass().getSimpleName().endsWith("Impl");
			if (annotated && cloneIndex >= expectations.length) {
				status = new Status(StatusTyp.PARAMETER_UNDEFINED, "countIndex > expectations.length", null);
				return null;
			}
			expect = expectations[annotated ? cloneIndex : 0];
			try {
				if (Util.isEmpty(expect.when())) {
					int i = expect.parIndex();
					if (i < 0) {
						status = new Status(StatusTyp.PARAMETER_UNDEFINED, "expect.when() == null && expect.parIndex < 0 not allowed!", null);
						return null;
					}
					parameter = createStartParameter(source.getParameterTypes());
					parameter[i] = ObjectUtil.wrap(expect.whenPar(), source.getParameterTypes()[i]);
				} else {
					parameter = createParameterFromStringArr(source.getParameterTypes(), expect.when());
				}
			} catch (Exception e) {
				status = new Status(StatusTyp.PARAMETER_ERROR, e.getMessage(), e);
				parameter = null;
				return null;
			}
			status = INITIALIZED;
		}
		return parameter;
	}
	private Object[] createParameterFromStringArr(Class[] types, String[] strValues) {
		Object[] pars = new Object[types.length];
		for (int i = 0; i < strValues.length; i++) {
			pars[i] = ObjectUtil.wrap(strValues[i], types[i]);
		}
		return pars;
	}

	@Override
	protected Object getInstance(Method method) {
		if (construction == null && !Util.isEmpty(expect.construct())) {
			construction = new Construction(null);
			construction.parameter = createParameterFromStringArr(expect.constructTypes(), expect.construct());
			construction.instance = BeanClass.createInstance(source.getDeclaringClass(), parameter);
			return construction.instance;
		}
		return super.getInstance(method);
	}
	
	@Override
	public Object getCompareOrigin() {
		if (expect == null)
			return NOT_DEFINED;
		Object then = expect == null || expect.then() == null || expect.then().equals("null") ? null : expect.then();
		return then != null && getCompareResult() != null
				? ObjectUtil.wrap(expect.then(), getCompareResult().getClass()) 
				: null;
	}

	private int getResultIndex() {
		return expect.resultIndex() < 0 && !void.class.isAssignableFrom(source.getReturnType()) 
				? -1 
				: expect.resultIndex() < 0 
						? 0
						: expect.resultIndex();
	}

	@Override
	public Object getCompareResult() {
		return expect == null || getResultIndex() < 0 ? result : getParameter()[getResultIndex()];
	}

	@Override
	public Object getExpectFail() {
		return expect != null && expect.then() != null && expect.then().startsWith("fail(") ? createFailException(expect.then()) : null;
	}
	
	private Object createFailException(String then) {
		String cls = StringUtil.substring(then, "fail(", "(");
		String msg = StringUtil.substring(then, cls + "(", null);
		msg = msg.substring(0, msg.length() - 1);
		return BeanClass.createInstance(cls, msg);
	}
	
	@Override
	public void run() {
		if (getParameter() == null) {
			log ("no expectation found for test number " + cloneIndex + "\n");
			status = new Status(StatusTyp.PARAMETER_UNDEFINED, NOT_DEFINED, null);
			result = NOT_DEFINED;
			return;
		}
		result = run(source, parameter);
		status = result != null ? OK : NULL_RESULT;
	}
	@Override
	public int hashCode() {
		return Objects.hash(source.toGenericString(), cloneIndex);
	}
	@Override
	public boolean equals(Object obj) {
		return obj != null && hashCode() == obj.hashCode();
	}
	@Override
	public String toString() {
		return super.toString() + " -> expected: " + expect;
	}
}
