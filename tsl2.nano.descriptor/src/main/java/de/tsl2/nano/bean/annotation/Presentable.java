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

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import de.tsl2.nano.action.IActivable;

/**
 * Define attribute presentation. For further informmations, see IPresentable.
 * @author Tom
 * @version $Revision$ 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface Presentable {
    int type() default -1;
    int style() default -1;
    boolean enabled() default true;
    boolean visible() default true;
    boolean searchable() default true;
    boolean nesting() default false;
    String label() default "";
    String description() default "";
    String[] items() default {};
    String[] layout() default {};
    String[] layoutConstraints() default {};
    int width() default -1;
    int height() default -1;
    String icon() default "";
    int[] foreground() default {};
    int[] background() default {};

}