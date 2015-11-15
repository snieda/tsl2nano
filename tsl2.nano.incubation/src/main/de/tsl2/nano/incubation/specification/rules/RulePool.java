/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 01.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.incubation.specification.rules;

import de.tsl2.nano.incubation.specification.Pool;

/**
 * Holds all defined rules. Reading existing rules from directory.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class RulePool extends Pool<AbstractRule<?>> {
    /**
     * constructor
     */
    public RulePool() {
        super();
    }
}
