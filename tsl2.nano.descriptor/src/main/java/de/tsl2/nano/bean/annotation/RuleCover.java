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

import de.tsl2.nano.bean.def.AttributeCover;

/**
 * Defines a new {@link AttributeCover} on the given attribute. Means, the attribute will be defined dynamically through evaluating rules on runtime.<p/>
 * You have to provide the attribute presentable 
 * path (the child of the attribute: e.g. presentable.layout) and a rule to overrule the behaviour.
 * @author Tom
 * @version $Revision$ 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface RuleCover {
	Class<? extends AttributeCover<?>> implementationClass();
    String child() default "";
    String rule() default "";
}
