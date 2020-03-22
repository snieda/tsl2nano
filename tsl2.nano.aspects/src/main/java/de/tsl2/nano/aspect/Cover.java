package de.tsl2.nano.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE_USE, ElementType.PARAMETER})
public @interface Cover {

    public boolean trace() default false;
    public boolean up() default false;
    public Class<Function> before() default Function.class;
    public Class<Function> after() default Function.class;
}
