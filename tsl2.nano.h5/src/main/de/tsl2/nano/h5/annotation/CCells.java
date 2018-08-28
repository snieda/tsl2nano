package de.tsl2.nano.h5.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** only to be repeatable */
@Retention(RUNTIME)
@Target(TYPE)
public @interface CCells {
    CCell[] value();
}
