package de.tsl2.nano.util.test.inverse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate your method to define it as a reverse function of another known
 * function. Will be used to do automatic test creation with randomized
 * parameters.
 * 
 * So, the call of {@link #methodName()} should create a result that can be
 * inverted by calling the reverse function (the annotated method itself). The
 * origin parameter value has to be equal to the result of the reverse function.
 * <p/>
 * we duplicate this annotation from common, to use it in core, too.
 * 
 * @author Tom
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface InverseFunction {
	/** class that declares the method (default: same class) */
	Class declaringType() default Object.class;

	/** method that is the origin - or to be called before the own inverse method */
	String methodName();

	/** all parameter types of the given methodName */
	Class[] parameters() default {};

	/**
	 * reuse calling parameter of first function (see methodName) on call of the
	 * inverse function (the method, having this annotation)
	 * <p/>
	 * the content of the int array are the parameter indexes of the first function
	 * - to be filled in the order of the inverse function parameters.
	 * <p/>
	 * -1: fill the returned result instead of a parameter.<br/>
	 * -2: fill any random value on that inverse parameter index
	 */
	int[] bindParameterIndexesOnInverse() default {-1};

	/**
	 * whitch parameter or result (returned result = -1) should be used to compare
	 * with inverse function
	 */
	int compareParameterIndex() default -1;

	/**
	 * which parameter or result of own inverse function should be compare with the
	 * origin function
	 */
	int compareInverseParameterIndex() default -1;
}
