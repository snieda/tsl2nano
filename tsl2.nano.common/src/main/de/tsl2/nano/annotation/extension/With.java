package de.tsl2.nano.annotation.extension;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({TYPE, METHOD, PARAMETER})
@Repeatable(Withs.class)
public @interface With {
	Class<? extends AnnotationFactory<?, ?>> value();
}
