package de.tsl2.nano.autotest.creator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * child annotation of {@link Expectations}. to be used to create automated unit tests without coding.<p/>
 * you need only to fill either parIndex() + whenPar() + then()  or when() + then().<p/>
 * 
 * annotations can only have primitive, class or string values. so we use String values. they will be converted to
 * values of method argument and result types. If not possible, this annotation is not usable.
 * @author Tom
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Repeatable(Expectations.class)
public @interface Expect {
	/** method parameter index, only if {@link #whenPar()} is used. means, only one parameter will be checked. */
	int parIndex() default -1;
	/** when method parameter of {@link #parIndex()} has this value, then the result has to be value of {@link #then()} */
	String whenPar() default "";
	/** describes all parameter values to have the method result, given by {@link #then()} */
	String[] when() default {};
	/** describes the expected method result when either {@link #when()} or {@link #whenPar()} was set */
	String then();
	/** if the method does not return a result, but changes the content of a given parameter, this is the index of that parameter used by then() */
	int resultIndex() default -1;
}
