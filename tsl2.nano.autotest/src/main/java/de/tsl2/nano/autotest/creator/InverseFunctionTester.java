package de.tsl2.nano.autotest.creator;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.Util;

/**
 * collects all annotated methods with {@link InverseFunction} and prepares all
 * start parameters (randomized) to be called on unit testing. provides
 * comparing between origin value and inverse function result.
 * <p/>
 * to do a multiple random test on each function, give a duplication parameter >
 * 0.
 * 
 * @author Thomas Schneider
 */
@FunctionType(InverseFunction.class)
public class InverseFunctionTester extends AFunctionTester<InverseFunction> {

	Object inverseResult;
	private Object[] parameterInverse;
	
	public InverseFunctionTester(Method source) {
		super(source);
		def = source.getAnnotation(InverseFunction.class);
		log("\t" + source.getDeclaringClass().getSimpleName() + ": " + source.getName() + " -> " + def.methodName() + "\n");
	}

	protected Method getFunction(InverseFunction funcAnn) {
		final Class declCls = funcAnn.declaringType().equals(Object.class) ? source.getDeclaringClass()
				: funcAnn.declaringType();
		return Util.trY(() -> declCls.getMethod(funcAnn.methodName(), funcAnn.parameters()));
	}

	protected Object[] getParameter() {
		if (parameter == null)
			parameter = createStartParameter(def.parameters());
		return parameter;
	}
	
	protected Object[] getInverseParameter() {
		if (parameterInverse == null) {
			try {
				parameterInverse = createStartParameter(source.getParameterTypes());
				int[] bind = def.bindParameterIndexesOnInverse();
				for (int i = 0; i < bind.length; i++) {
					if (bind[i] == -1 && i >= parameterInverse.length) //bind = default {-1}, but no parameterInverse existing
						break;
					parameterInverse[i] = bind[i] == -2
						? parameterInverse[i] 
							: bind[i] == -1 
								? ObjectUtil.wrap(getResult(), source.getParameterTypes()[i]) 
									: ObjectUtil.wrap(parameter[bind[i]], source.getParameterTypes()[i]);
				}
			} catch (Exception e) {
				status = new Status(StatusTyp.PARAMETER_ERROR, e.getMessage(), e);
				parameter = null;
				return null;
			}
			status = Status.INITIALIZED;
			
		}
		return parameterInverse;
	}

	@Override
	public void run() {
		Method sourceMethod = getFunction(def);
		log("==> doing test " + cloneIndex + " step 1: ");
		result = run(sourceMethod, getParameter());
		doBetween();
		log("==> doing test " + cloneIndex + " step 2: ");
		inverseResult = run(source, getInverseParameter());
	}


	protected Object getInverseResult() {
		return inverseResult;
	}

	public Object getCompareOrigin() {
		return def.compareParameterIndex() < 0  && !getFunction(def).getReturnType().equals(void.class) ? result : parameter[undefToZeroIndex(def.compareParameterIndex())];
	}

	public Object getCompareResult() {
		Object compRevObject = def.compareInverseParameterIndex() < 0 && !source.getReturnType().equals(void.class) ? inverseResult : parameterInverse[undefToZeroIndex(def.compareInverseParameterIndex())];
		return getCompareOrigin() != null ? ObjectUtil.wrap(compRevObject, getCompareOrigin().getClass()) : compRevObject;
	}

	@Override
	public Throwable getExpectFail() {
		return null;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(def.methodName(), source.toGenericString(), cloneIndex);
	}
	@Override
	public String toString() {
		return cloneIndex + ": " + source.getDeclaringClass().getSimpleName() + "." + def.methodName() + "->" + source.getName() + " " + parametersAsString();
	}
}
