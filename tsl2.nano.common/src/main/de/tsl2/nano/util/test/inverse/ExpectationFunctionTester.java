package de.tsl2.nano.util.test.inverse;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.Util;

@FunctionType(Expectations.class)
public class ExpectationFunctionTester extends AFunctionTester<Expectations> {

	private static final String NOT_DEFINED = "NOT DEFINED";
	private Expect expect;

	public ExpectationFunctionTester(Method source) {
		super(source);
		def = source.getAnnotation(Expectations.class);
		log("\t" + source.getDeclaringClass().getSimpleName() + ": " + source.getName() + " -> " + Arrays.toString(def.value()) + "\n");
	}

	@Override
	protected Object[] getParameter() {
		if (parameter == null) {
			Expect[] expectations = def.value();
			if (cloneIndex >= expectations.length) {
				return null;
			}
			expect = expectations[cloneIndex];
			if (Util.isEmpty(expect.when())) {
				parameter = createStartParameter(source.getParameterTypes());
				int i = expect.parIndex();
				parameter[i] = ObjectUtil.wrap(expect.whenPar(), source.getParameterTypes()[i]);
			} else {
				parameter = new Object[source.getParameterTypes().length];
				for (int i = 0; i < expect.when().length; i++) {
					parameter[i] = ObjectUtil.wrap(expect.when()[i], source.getParameterTypes()[i]);
				}
			}
		}
		return parameter;
	}

	@Override
	public Object getCompareOrigin() {
		return expect != null ? ObjectUtil.wrap(expect.then(), source.getReturnType()) : NOT_DEFINED;
	}

	@Override
	public Object getCompareResult() {
		return result;
	}

	@Override
	public void run() {
		if (getParameter() == null) {
			log ("no expectation found for test number " + cloneIndex + "\n");
			result = NOT_DEFINED;
			return;
		}
		result = run(source, parameter);
	}
	@Override
	public int hashCode() {
		return Objects.hash(source.toGenericString(), cloneIndex);
	}
	@Override
	public String toString() {
		return cloneIndex + ": " + source.getName() + " " + Arrays.toString(getParameter()) + " -> expected: " + expect;
	}
}
