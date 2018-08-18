package de.tsl2.nano.h5.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import de.tsl2.nano.annotation.extension.With;

/**
 * defines a {@link de.tsl2.nano.h5.Compositor} for a bean
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
@Retention(RUNTIME)
@Target(TYPE)
@With(SpecificationAnnotationFactory.class)
public @interface Specification {
    String name();
    SpecificationType specificationType();
    String expression();
    enum SpecificationType {RULE, RULESCRIPT, ACTION, QUERY, WEB};
}
