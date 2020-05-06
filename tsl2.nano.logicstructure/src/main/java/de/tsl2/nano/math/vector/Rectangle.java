/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 09.04.2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.math.vector;

/**
 * Simple 2-dim rectangle definition, working on floats
 * 
 * @author ts
 * @version $Revision$
 */
public class Rectangle extends Coordinate {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public Rectangle(float x, float y, float w, float h) {
        super(x, y, w, h);
    }

    /**
     * x
     * 
     * @return x-coordinate
     */
    public float x() {
        return x[0];
    }

    /**
     * y
     * 
     * @return y-coordinate
     */
    public float y() {
        return x[1];
    }

    /**
     * w
     * 
     * @return width
     */
    public float w() {
        return x[2];
    }

    /**
     * h
     * 
     * @return height
     */
    public float h() {
        return x[3];
    }

    /**
     * checks, whether given point is contained in current rectangle
     * 
     * @param x x-coordinate
     * @param y y-coordinate
     * @return true, if point x, y is contained
     */
    public boolean contains(float x, float y) {
        return x() <= x && x <= x() + w() && y() <= y && y <= y() + h();
    }
}
