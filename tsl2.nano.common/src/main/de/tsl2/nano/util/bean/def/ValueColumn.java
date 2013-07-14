/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 16, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

import static de.tsl2.nano.util.bean.def.IPresentable.UNDEFINED;

import java.io.Serializable;
import java.text.Format;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
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
            actionSortColumn = new CommonAction<Object>(name, name, getDescription()) {
                @Override
                public Object action() throws Exception {
                    sortIndex = 0;
                    isSortUpDirection = isSortUpDirection ? false: true;
                    collector.shiftSortIndexes();
                    collector.sort();
                    return collector;
                }
            };
        }
        return actionSortColumn;
    }
}
