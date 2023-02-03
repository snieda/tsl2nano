package de.tsl2.nano.autotest.creator;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Objects;

import de.tsl2.nano.autotest.Construction;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

@FunctionType(Expectations.class)
public class ExpectationFunctionTester extends AFunctionTester<Expectations> {

	private static final String UNDEFINED = "UNDEFINED";
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
		logd("\t" + source.getDeclaringClass().getSimpleName() + ": " + source.getName() + " -> " + Arrays.toString(def.value()) + "\n");
	}

	@Override
	protected Object[] getParameter() {
		if (parameter == null && !status.in(StatusTyp.PARAMETER_UNDEFINED, StatusTyp.PARAMETER_ERROR)) {
			Expect[] expectations = def.value();
			//if generated ExpectationsImpl is used, there is only one @Expect!
			boolean annotated = !def.getClass().getSimpleName().endsWith("Impl")
				&& !Proxy.isProxyClass(def.getClass());
			if (annotated && cloneIndex >= expectations.length) {
				status = new Status(StatusTyp.PARAMETER_UNDEFINED, "countIndex > expectations.length", null);
				return null;
			}
			try {
				expect = expectations[annotated ? cloneIndex : 0];
				if (!Util.isEmpty(expect.whenPar())) {
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
			status = Status.INITIALIZED;
		}
		return parameter;
	}
	private Object[] createParameterFromStringArr(Class[] types, String[] strValues) {
		Object[] pars = new Object[types.length];
		for (int i = 0; i < types.length; i++) {
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
			return UNDEFINED;
		Object then = expect.then() == null || expect.then().equals("null") ? null : expect.then();
		return convertOnMultilineString(
				then != null
					? ObjectUtil.wrap(then, (Class)(getResultIndex() < 0 ? source.getReturnType() : source.getParameterTypes()[getResultIndex()])) 
					: null);
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
		Object res =  convertOnMultilineString(expect == null || getResultIndex() < 0 || getParameter() == null || getParameter().length == 0 ? result : getParameter()[getResultIndex()]);
		// dirty workaround on using part of parameter (not having quotations around
		return addQuotationsOnResultParameter(res);
	}
	private Object addQuotationsOnResultParameter(Object res) {
		if (getResultIndex() > -1 &&  res instanceof String) {
			res = addQuotationsOnStringAroundBrackets((String) res);
		} else if (getResultIndex() > -1 &&  res instanceof String[]) {
			String[] r = (String[]) res;
			if (r.length > 0)
				r[0] = addQuotationsOnStringAroundBrackets(r[0]);
		}
		return res;
	}
	private String addQuotationsOnStringAroundBrackets(String r) {
		if (!r.startsWith("{\"") && r.startsWith("{") && r.endsWith("}")) {
			r = r.replace("{", "{\"").replace("}", "\"}");
		}
		return r;
	}

	@Override
	public Throwable getExpectFail() {
		return expect != null && expect.then() != null && expect.then().startsWith("fail(") ? createFailException(expect.then()) : null;
	}
	
	private Throwable createFailException(String then) {
		String cls = StringUtil.substring(then, "fail(", ":");
		String msg = StringUtil.substring(then, cls + ": ", null);
		msg = msg.substring(0, msg.length() - 1);
		return BeanClass.createInstance(cls, msg);
	}
	
	@Override
	public void run() {
		if (getParameter() == null) {
			log (this + ": Parameter Error on cloneIndex: " + cloneIndex + "\n");
			if (!status.isFatal())
				status = new Status(StatusTyp.PARAMETER_UNDEFINED, UNDEFINED, null);
			result = UNDEFINED;
			return;
		}
		result = run(source, parameter);
		status = result != null ? Status.OK : Status.NULL_RESULT;
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
		return super.toString() + " -> EXPECTED: " + expect;
	}
}
