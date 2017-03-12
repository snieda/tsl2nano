/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 10.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;

import org.simpleframework.xml.Attribute;

/**
 * GroupBy Element for BeanCollectors to group its content
 * 
 * @author Tom
 * @version $Revision$
 */
public class GroupBy implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 5402615405121895937L;
    
    @Attribute
    String title;
    @Attribute
    String attribute;
    @Attribute
    String having;
    @Attribute(required=false)
    boolean expanded;

    /**
     * constructor
     */
    protected GroupBy() {
        super();
    }

    /**
     * constructor
     * 
     * @param title
     * @param attribute
     * @param having
     */
    public GroupBy(String title, String attribute, String having) {
        super();
        this.title = title;
        this.attribute = attribute;
        this.having = having;
    }

    /**
     * @return Returns the title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title The title to set.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return Returns the attribute.
     */
    public String getAttribute() {
        return attribute;
    }

    /**
     * @param attribute The attribute to set.
     */
    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    /**
     * @return Returns the having.
     */
    public String getHaving() {
        return having;
    }

    /**
     * @param having The having to set.
     */
    public void setHaving(String having) {
        this.having = having;
    }

    /**
     * @return Returns the expanded.
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * @param expanded The expanded to set.
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
}
