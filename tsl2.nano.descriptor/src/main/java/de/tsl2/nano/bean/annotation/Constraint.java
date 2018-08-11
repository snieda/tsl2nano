/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 05.01.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.bean.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define attribute constraints for actions and attributes.
 * @author Tom
 * @version $Revision$ 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface Constraint {
    String name() default "";
    Class<?> type() default Object.class;
    String pattern() default "";
    int length() default -1;
    boolean nullable() default true;
    String defaultValue() default "";
    String min() default "";
    String max() default "";
    String[] allowed() default {};
}
