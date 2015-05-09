/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 29, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.fi;

import java.text.Format;

import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Presentable;


/**
 * 
 * @author Thomas Schneider
 * @version $Revision$ 
 */
public class Bean<T> extends de.tsl2.nano.bean.def.Bean<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    AttributeDefinition<?> attrOnConstruction;
    
    /**
     * constructor
     */
    public Bean() {
        super();
    }

    /**
     * constructor
     * @param instance
     */
    public Bean(T instance) {
        super(instance);
    }

    public <V> Bean<T> add(String name, V value) {
        attrOnConstruction = addAttribute(name, value, null, name, null);
        return this;
    }
    
    public Bean<T> constrain(int length, Format format, boolean nullable) {
        attrOnConstruction.setBasicDef(length, nullable, format, null, attrOnConstruction.getDescription());
        return this;
    }
    public Bean<T> description(Presentable p) {
        attrOnConstruction.setPresentation(p);
        return this;
    }
}
