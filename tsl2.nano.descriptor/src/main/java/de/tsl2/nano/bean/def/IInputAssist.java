/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 01.07.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.util.Collection;

/**
 * known from code-assist, this interfaces provides an input assist, see {@link #availableValues(Object)}. this input
 * assist may refresh connected change listeners, hold by {@link #changeHandler()}.
 * 
 * @author Tom
 * @version $Revision$
 */
public interface IInputAssist<T> {
    /**
     * evaluates matching objects for given prefix. technical base routine for {@link #availableValues(Object)}.
     * @param prefix part of a value
     * @return all possible values for prefix
     */
    Collection<T> matchingObjects(Object prefix);
    /**
     * getting a part of a value this method should return all available/possible values for this part or prefix.
     * 
     * @param prefix part of a value
     * @return all possible values for prefix
     */
    Collection<String> availableValues(Object prefix);
}
