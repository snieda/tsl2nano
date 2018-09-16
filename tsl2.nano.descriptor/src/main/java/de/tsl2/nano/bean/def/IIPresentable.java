/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 24.01.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public interface IIPresentable extends IPresentable, Serializable {

    /**
     * @param label The label to set.
     */
    void setLabel(String label);

    /**
     * @param description The description to set.
     */
    void setDescription(String description);

    /**
     * @param layout The layout to set.
     */
    void setLayout(LinkedHashMap<String, String> layout);

    /**
     * @param layoutConstraints The layoutConstraints to set.
     */
    void setLayoutConstraints(LinkedHashMap<String, String> layoutConstraints);

    /**
     * @param icon The icon to set.
     */
    void setIcon(String icon);

    void setIconFromField(String attributename);
    
    /**
     * @param foreground The foreground to set.
     */
    void setForeground(int[] foreground);

    /**
     * @param background The background to set.
     */
    void setBackground(int[] background);

    /** sets an item list */
    <T extends IPresentable> T setItemList(List<?> itemlist);

    /** sets nesting */
    <T extends IPresentable> T setNesting(boolean nesting);

}
