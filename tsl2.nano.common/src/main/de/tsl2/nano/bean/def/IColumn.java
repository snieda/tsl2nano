/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jan 24, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.text.Format;

/**
 * describes a table columns properties. should be implemented by a beanvalue class. intersects some properties of
 * IAttributeDefinition.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IColumn {

    /**
     * @return Returns the columnText.
     */
    public abstract String getDescription();

    /**
     * the position of that column in a table.
     * 
     * @return Returns the index.
     */
    public abstract int getIndex();

    /**
     * the index of sorting - on multiple column sort.
     * 
     * @return Returns the sortIndex.
     */
    public abstract int getSortIndex();

    /**
     * @return Returns the sortDirection.
     */
    public abstract boolean isSortUpDirection();

    /**
     * @return Returns the pixel-width of the column.
     */
    public abstract int getWidth();

    /**
     * @return Returns the attribute name.
     */
    public abstract String getName();

    /**
     * @return Returns the columns format, to format a columns value.
     */
    public abstract Format getFormat();

}