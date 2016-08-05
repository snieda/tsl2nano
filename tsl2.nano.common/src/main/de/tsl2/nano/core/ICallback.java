/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 12.03.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.core;

import java.util.Map;

/**
 * Implementations of this interface can be used on loops - calling the {@link #callback(Map)} method providing all
 * properties of the current loop pass.
 * 
 * @author Tom
 * @version $Revision$
 */
public interface ICallback<RESULT> {
    /** callback to be called inside each loop pass. the entry provides all informations of the current pass */
    RESULT run(Map<Object, Object> passInfo);
}
