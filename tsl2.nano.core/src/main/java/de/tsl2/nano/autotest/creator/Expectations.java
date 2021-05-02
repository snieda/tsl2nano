package de.tsl2.nano.autotest.creator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * annotate your method to do automated creation of unit tests - without coding
 * - checking all expectations.<p/>
 * 
 * as a constraint of defining annotations, only primitive types, strings and classes are allowed, so {@link Expect} uses 
 * strings as values for {@link Expect#when()} and {@link Expect#then()}. you are only able to use this {@link Expectations},
 * if your method parameter values and return type are convertible to strings and vice versa.
 * 
 * @author Tom
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Expectations {
	Expect[] value();
}
