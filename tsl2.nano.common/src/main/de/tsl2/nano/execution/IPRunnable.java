/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 25.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.execution;

import java.io.Serializable;
import java.util.Map;

/**
 * 
 * @author Tom
 * @version $Revision$ 
 */
public interface IPRunnable<RESULT, CONTEXT extends Map<String, Object>> extends IRunnable<RESULT, CONTEXT>, Serializable {
    /** @return name of runnable */
    String getName();
    /** defined parameter, to be checked (see {@link #checkArguments(Map)}) against given CONTEXT */
    Map<String, ? extends Serializable> getParameter();
    /** check arguments against defined parameter and return only defined arguments */
    CONTEXT checkedArguments(CONTEXT args, boolean strict);
}
