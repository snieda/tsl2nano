/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 10, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util.bean.def;


/**
 * description of a value group inside a bean. usable to create sub panel informations. you can define a full set of
 * child attribute names or only the first and the last one. normally, the order of attributes is constrained, so the
 * attributes between the first and the last are defined by order.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ValueGroup extends Presentable {
    /** serialVersionUID */
    private static final long serialVersionUID = -6371693652730976950L;
    
    String[] attributeNames;

    /**
     * constructor to be serializable
     */
    protected ValueGroup() {
        super();
    }

    public ValueGroup(String label, String... attributeNames) {
        this.label = label;
        this.attributeNames = attributeNames;
        type = TYPE_FORM;
    }

    /**
     * getAttributeNames
     * 
     * @return child attributes
     */
    public String[] getAttributeNames() {
        return attributeNames;
    }

    public String getFirstChildAttribute() {
        return attributeNames[0];
    }

    public String getLastChildAttribute() {
        return attributeNames[attributeNames.length - 1];
    }
}
