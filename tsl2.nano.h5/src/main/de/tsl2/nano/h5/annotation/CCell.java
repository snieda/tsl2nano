package de.tsl2.nano.h5.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * defines a cell in a {@link de.tsl2.nano.h5.collector.CSheet} for a {@link CSheet}
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(CCells.class)
public @interface CCell {
    int row();
    int col();
    String value();
}
