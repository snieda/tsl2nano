/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 12, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.util.Collection;

/**
 * describes table columns
 * @author Thomas Schneider
 * @version $Revision$ 
 */
public interface ITableDescriptor<T> {

    /**
     * getColumnDefinitions
     * @return all visible column definitions of that bean - while the columns itself may be defined inside the beanvalue.
     */
    Collection<IPresentableColumn> getColumnDefinitions();
    
    /**
     * Returns the label text for the given column of the given element.
     *
     * @param element the object representing the entire row, or
     *   <code>null</code> indicating that no input object is set
     *   in the viewer
     * @param columnIndex the zero-based index of the column in which the label appears
     * @return String or or <code>null</code> if there is no text for the 
     *  given object at columnIndex
     */
    String getColumnText(T element, int columnIndex);

    /**
     * Returns the label text for the given column-summary of the given element.
     *
     * @param columnIndex the zero-based index of the column in which the label appears
     * @return String or or <code>null</code> if there is no text for the 
     *  given object at columnIndex
     */
    String getSummaryText(Object context, int columnIndex);
    
    /**
     * shiftSortIndexes
     */

    void shiftSortIndexes();
    /**
     * sort
     */
    void sort();
}
