/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 22.11.2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.math.vector;

/**
 * 2-dim point with x and y coordinate
 * 
 * @author ts
 * @version $Revision$
 */
public class Point extends Coordinate {

    /**
     * constructor
     * 
     * @param x x coordinate
     * @param y y coordinate
     */
    public Point(float x, float y) {
        super(x, y);
    }

    /**
     * x
     */
    public float x() {
        return x[0];
    }

    /**
     * y
     */
    public float y() {
        return x[1];
    }

    /**
     * adds the given point to the current.
     * @param x x coordinate of adding point
     * @param y y coordinate of adding point
     */
    public void move(float x, float y) {
        add(x, y);
    }
}
