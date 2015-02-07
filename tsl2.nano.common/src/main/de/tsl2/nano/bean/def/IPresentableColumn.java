/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 4, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.bean.def;

import de.tsl2.nano.action.IAction;

/**
 * Extends the {@link IColumn} to have a member of {@link IPresentable} for layouting in a table - this may differ from
 * attribute presentation.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IPresentableColumn extends IColumn {
    /**
     * getSortingAction
     * 
     * @return action to sort the column
     */
    IAction<?> getSortingAction(final IBeanCollector<?, ?> collector);

    /**
     * if true and {@link #getSummary()} is empty, the table presenting this column should summarize all number values
     * of this column
     * 
     * @return
     */
    boolean isStandardSummary();

    /**
     * getSummarize
     * 
     * @return a summarize expression to be shown to the foot of a table. overwrites {@link #isStandardSummary()}.
     */
    IValueExpression<?> getSummary();

    /**
     * extending layout informations for a table column
     */
    IPresentable getPresentable();

    /**
     * setPresentable
     * 
     * @param presentable
     */
    void setPresentable(IPresentable presentable);
}
