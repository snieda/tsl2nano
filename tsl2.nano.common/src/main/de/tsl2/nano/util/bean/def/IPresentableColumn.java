/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 4, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

/**
 * Extends the {@link IColumn} to have a member of {@link IPresentable} for layouting in a table - this may differ from
 * attribute presentation.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IPresentableColumn extends IColumn {
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
