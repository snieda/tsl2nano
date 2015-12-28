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

    @Override
    protected String getFileName(String name) {
        //cut optional rule prefix
        name = name.matches("\\w+.*") ? name : name.substring(1);
        return super.getFileName(name);
    }
    @Override
    public AbstractRule<?> get(String name) {
        Class<? extends AbstractRule> type;
        if (name.startsWith(String.valueOf(RuleScript.PREFIX))) {
            type = RuleScript.class;
            name = name.substring(1);
        } else if (name.startsWith(String.valueOf(RuleDecisionTable.PREFIX))) {
                type = RuleDecisionTable.class;
                name = name.substring(1);
        } else {
            type = Rule.class;
            if (name.startsWith(String.valueOf(AbstractRule.PREFIX)))
                name = name.substring(1);
        }
        return super.get(name, type);
    }
}
