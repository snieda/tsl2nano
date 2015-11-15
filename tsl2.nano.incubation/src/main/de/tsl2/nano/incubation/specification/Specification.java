/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 13.11.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.incubation.specification;

import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.core.util.Util;

/**
 * To be used inside of {@link AbstractRunnable} as check/test object
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Specification {
    @Attribute
    String name;
    @Element(required = false)
    String description;
    @Element
    Object exptected;
    @ElementMap(entry = "argument", attribute = true, inline = true, keyType = String.class, key = "name", value = "value", valueType = Object.class, required = false)
    Map<String, Object> arguments;

    /**
     * constructor
     */
    protected Specification() {
        super();
    }

    /**
     * constructor
     * 
     * @param name
     * @param description
     * @param exptected
     * @param arguments
     */
    protected Specification(String name, String description, Object exptected, Map<String, Object> arguments) {
        super();
        this.name = name;
        this.description = description;
        this.exptected = exptected;
        this.arguments = arguments;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return Returns the exptected.
     */
    public Object getExptected() {
        return exptected;
    }

    /**
     * @param exptected The exptected to set.
     */
    public void setExptected(Object exptected) {
        this.exptected = exptected;
    }

    /**
     * @return Returns the arguments.
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * @param arguments The arguments to set.
     */
    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
    
    @Override
    public String toString() {
        return Util.toString(getClass(), "name: ", name);
    }
}
