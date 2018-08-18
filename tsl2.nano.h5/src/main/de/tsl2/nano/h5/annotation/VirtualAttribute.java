package de.tsl2.nano.h5.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import de.tsl2.nano.annotation.extension.With;

@Retention(RUNTIME)
@Target(TYPE)
@With(VirtualAttributeAnnotationFactory.class)
public @interface VirtualAttribute {
    Specification.SpecificationType specificationType();
    String name();
    String expression();
}
