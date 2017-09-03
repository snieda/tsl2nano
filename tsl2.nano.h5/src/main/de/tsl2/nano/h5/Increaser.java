/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 24.07.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.h5;

import java.io.Serializable;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;

/**
 * holds properties to create new instances with an increasing attribute
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Increaser implements Serializable {

    private static final long serialVersionUID = 4984470230289140956L;

    private int count;
    private int step;
    private String name;

    protected Increaser() {
    }

    /**
     * constructor
     */
    public Increaser(String name, int count, int step) {
        this.name = name;
        this.count = count;
        this.step = step;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Returns the count.
     */
    public int getCount() {
        return count;
    }

    /**
     * @return Returns the step.
     */
    public int getStep() {
        return step;
    }

}
