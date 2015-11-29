/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 21.11.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.lang.reflect.InvocationHandler;

/**
 * Extends the {@link InvocationHandler} to have a delegating instance
 * @author Tom
 * @version $Revision$ 
 */
public interface IDelegationHandler<T> extends InvocationHandler {
    T getDelegate();
    Class<?>[] getInterfaces();
}
