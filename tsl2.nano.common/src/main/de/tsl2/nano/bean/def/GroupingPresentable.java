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

import java.util.Collection;
import java.util.LinkedList;

import org.simpleframework.xml.ElementList;

/**
 * Presentable extension to define group-by statements similar to sql group-by/having
 *  
 * @author Tom
 * @version $Revision$ 
 */
public class GroupingPresentable extends Presentable {
    /** serialVersionUID */
    private static final long serialVersionUID = -3479452140340072920L;
    
    /** optional grouping informations */
    @ElementList(inline = true, entry = "groupby", type = GroupBy.class, required = false)
    protected Collection<GroupBy> groups;

    /**
     * constructor
     */
    public GroupingPresentable() {
        super();
    }
        
    public GroupingPresentable(AttributeDefinition<?> attr) {
        super(attr);
    }

    /**
     * @return Returns the groups.
     */
    public Collection<GroupBy> getGroups() {
        return groups;
    }

    /**
     * adds a GroupBy group for the given attribute with a having expression
     * @param title
     * @param attribute
     * @param having
     * @return true, if successfull added to the GroupBy collection
     */
    public boolean addGroup(String title, String attribute, String having) {
        if (groups == null)
            groups = new LinkedList<GroupBy>();
        return groups.add(new GroupBy(title, attribute, having));
    }
    
    /**
     * @param groups The groups to set.
     */
    public void setGroups(Collection<GroupBy> groups) {
        this.groups = groups;
    }

    /**
     * getGroupByFor
     * @param instance instance to check for GroupBy expressions
     * @return GroupBy or null
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public GroupBy getGroupByFor(BeanCollector collector, Object instance) {
        String colText;
        if (groups != null) {
            for (GroupBy g : groups) {
                colText = collector.getColumnText(instance, collector.getAttribute(g.getAttribute()));
                if (colText.matches(g.getHaving()))
                    return g;
            }
        }
        return null;
    }

}
