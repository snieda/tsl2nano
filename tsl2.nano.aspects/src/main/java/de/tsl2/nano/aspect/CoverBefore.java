package de.tsl2.nano.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * if a method that matches the origin method name of the covered class is
 * annoted with CoverBefore, this will be called before calling the origin
 * method body.
 * </p>
 * NOTE: annotation will be read by AspectCover (the aspectj agent must be
 * loaded to work!)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface CoverBefore {
    /** regular expression filter for method arguments */
    public String regEx() default ".*";
}
