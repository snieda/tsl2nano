/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 16, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import static de.tsl2.nano.bean.def.IPresentable.UNDEFINED;

import java.io.Serializable;
import java.text.Format;

import de.tsl2.nano.Messages;
import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;

/**
 * default implementation of {@link IColumn}. {@link IAttributeDefinition} needed as source information, see
 * {@link #ValueColumn(IAttributeDefinition, int, int, boolean, int)}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ValueColumn<T> implements IPresentableColumn, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 3998475409703066783L;
    
    transient IAttributeDefinition<T> attributeDefinition;
    transient IAction<?> actionSortColumn;
    
    /** attribute name to be identifiable */
    String name;
    int columnIndex;
    int sortIndex;
    boolean isSortUpDirection;
    int width;
    IPresentable presentable;
    
    /**
     * constructor to be serializable
     */
    protected ValueColumn() {
        super();
    }

    /**
     * constructor
     * 
     * @param attributeDefinition
     */
    public ValueColumn(IAttributeDefinition<T> attributeDefinition) {
        this(attributeDefinition, UNDEFINED, UNDEFINED, false, UNDEFINED);
    }

    /**
     * constructor
     * 
     * @param attributeDefinition
     * @param columnIndex
     * @param sortIndex
     * @param isSortUpDirection
     */
    public ValueColumn(IAttributeDefinition<T> attributeDefinition,
            int columnIndex,
            int sortIndex,
            boolean isSortUpDirection,
            int width) {
        super();
        this.name = attributeDefinition.getName();
        this.attributeDefinition = attributeDefinition;
        this.columnIndex = columnIndex;
        this.sortIndex = sortIndex;
        this.isSortUpDirection = isSortUpDirection;
        this.width = width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return attributeDefinition.getDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndex() {
        return columnIndex;
    }

    public void setIndex(int index) {
        columnIndex = index;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getSortIndex() {
        return sortIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSortUpDirection() {
        return isSortUpDirection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth() {
        if (width != UNDEFINED)
            return width;
        else
            return attributeDefinition.getPresentation().getWidth();
    }

    public void setWidth(int width) {
        this.width = width;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Format getFormat() {
        return attributeDefinition.getFormat();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IPresentable getPresentable() {
        return presentable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPresentable(IPresentable presentable) {
        this.presentable = presentable;
    }
    
    public IAction<?> getSortingAction(final IBeanCollector<?, ?> collector) {
        if (actionSortColumn == null) {
            actionSortColumn = new CommonAction<Object>(name, Messages.getStringOpt(name, true), getDescription()) {
                @Override
                public Object action() throws Exception {
                    collector.shiftSortIndexes();
                    sortIndex = 0;
                    isSortUpDirection = isSortUpDirection ? false: true;
                    collector.sort();
                    return collector;
                }
                @Override
                public String getImagePath() {
                    return imagePath != null ? imagePath : sortIndex == 0 ? isSortUpDirection ? "icons/up.png" : "icons/down.png" : "icons/updown.png";
                }
                @Override
                public Object getKeyStroke() {
                    return String.valueOf(columnIndex);
                }
            };
        }
        return actionSortColumn;
    }
    
    @Override
    public String toString() {
        return "column '" + name + "' at index " + columnIndex;
    }

    @Override
    public int compareTo(IColumn o) {
        return Integer.valueOf(getIndex()).compareTo(Integer.valueOf(o.getIndex()));
    }
}
