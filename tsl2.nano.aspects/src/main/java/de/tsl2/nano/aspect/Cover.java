package de.tsl2.nano.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * annotation will be read by AspectCover (the aspectj agent must be loaded to work!)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE_USE, ElementType.PARAMETER})
public @interface Cover {
    public static final Class<?>  EMPTYCLS = Class.class;

    /** trace only. set system property 'agent.log.timeformat' to set time format */
    public boolean trace() default false;
    /** coverup (mock) the annotated method ( or field, if declare as interface) */
    public boolean up() default false;
    /** if mocking on a field (declared as interface) this is the filter for the members to mock */
    public String upRegEx() default ".*";
    /** you must extend the function interface - but your interface must provide the static method run(CoverArgs) */
    public Class<?> before() default Class.class;
    /** see {@link #before()} */
    public Class<?> body() default Class.class;
    /** see {@link #before()} */
    public Class<?> after() default Class.class;
}