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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.core.Messages;

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
    @Attribute(required=false)
    String name;
    @Attribute(required=false)
    int columnIndex;
    @Attribute(required=false)
    int sortIndex;
    @Attribute(required=false)
    boolean isSortUpDirection;
    @Attribute(required=false)
    int width;
    @Element(required=false)
    IPresentable presentable;
    @Element(required=false)
    Comparable<T> minsearch; 
    @Element(required=false)
    Comparable<T> maxsearch;
    @Attribute(required=false)
    boolean standardSummary;
    @Element(required=false)
    IValueExpression<?> summary;
    
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
                    return imagePath != null ? imagePath : sortIndex == 0 ? isSortUpDirection ? "icons/up.png" : "icons/down.png" : "icons/cascade.png";
                }
                @Override
                public Object getKeyStroke() {
                    return String.valueOf(columnIndex);
                }
            };
        }
        return actionSortColumn;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Comparable<?> getMinSearchValue() {
        return maxsearch;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Comparable<?> getMaxSearchValue() {
        return maxsearch;
    }

    
    /**
     * @return Returns the standardSummary.
     */
    @Override
    public boolean isStandardSummary() {
        return standardSummary;
    }

    /**
     * @return Returns the summarize.
     */
    @Override
    public IValueExpression<?> getSummary() {
        return summary;
    }

    /**
     * @param summary The summarize to set.
     */
    public void setSummary(IValueExpression<?> summary) {
        this.summary = summary;
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
