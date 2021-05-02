package de.tsl2.nano.autotest.creator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * types of function tests (like expectations, inversefunction, ...) to be created
 * @author Tom
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface FunctionTester {
	Class<? extends AFunctionTester<?>>[] value();
}
