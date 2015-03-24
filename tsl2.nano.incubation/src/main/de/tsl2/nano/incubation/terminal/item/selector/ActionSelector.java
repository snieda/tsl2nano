/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 24.03.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.incubation.terminal.item.selector;

import java.util.List;
import java.util.Map;

import de.tsl2.nano.incubation.specification.actions.Action;

/**
 * Selector starting an action to evaluate the selectable elements
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class ActionSelector<T> extends Selector<T> {
    private static final long serialVersionUID = -8141122933764248752L;

    Action<T> action;

    /**
     * constructor
     * 
     * @param action
     */
    public ActionSelector(Action<T> action, String description) {
        super(action.getName(), description);
        this.action = action;
    }

    @Override
    protected List<?> createItems(Map props) {
        return (List<?>) action.run(props);
    }
}
