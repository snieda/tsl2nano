package de.tsl2.nano.annotation.extension;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** only to be repeatable */
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE, METHOD, PARAMETER})
public @interface Withs {
    With[] value();
}
