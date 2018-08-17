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

import de.tsl2.nano.bean.def.AbstractDependencyListener;

/**
 * TODO: UNIMPLEMENTED! implement process to use annotations from outside of tsl2.nano.descriptor.<p/>
 * Defines a new {@link AbstractDependencyListener} on the given attribute.
 * @author Tom
 * @version $Revision$ 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface DependencyListener {
	@SuppressWarnings("rawtypes")
	Class<? extends AbstractDependencyListener> implementation();
	String rule();
	int type() default 2;
	String[] observableAttributes();
}
