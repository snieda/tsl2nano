package de.tsl2.nano.h5.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import de.tsl2.nano.annotation.extension.With;

/**
 * defines a {@link de.tsl2.nano.h5.CSheet} for a bean
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
@Retention(RUNTIME)
@Target(TYPE)
@With(CSheetAnnotationFactory.class)
public @interface CSheet {
    String title();
    int rows();
    int cols();
    CCell[] cells();
}
