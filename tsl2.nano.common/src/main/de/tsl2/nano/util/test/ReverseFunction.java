package de.tsl2.nano.util.test;

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
 * 
 * @author Tom
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ReverseFunction {
	/** class that declares the method (default: same class) */
	Class declaringType() default Object.class;

	/** method that is the origin - or to be called before the own reverse method */
	String methodName();

	/** all parameter types of the given methodName */
	Class[] parameters() default {};

	/**
	 * use the result of given method as parameter (with given index) for own
	 * reverse function (default: none)
	 */
	int returnAsParameterIndex() default -1;

	/**
	 * whitch parameter or result (returned result = -1) should be used to compare
	 * with reverse function
	 */
	int compareParameterIndex() default -1;

	/**
	 * which parameter or result of own reverse function should be compare with the
	 * origin function
	 */
	int compareReverseParameterIndex() default -1;
}
