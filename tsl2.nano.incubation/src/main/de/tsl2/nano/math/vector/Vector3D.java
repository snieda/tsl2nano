/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 09.12.2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.math.vector;

/**
 * 3-dim constraint numeric vector providing x,y,z coordinates and rotations
 * 
 * @author ts
 * @version $Revision$
 */
public class Vector3D extends Vector {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    /**
     * e1, e2, e3 are the standard normalized vectors for dim 3.
     */
    public static final Vector e1 = e(3, 0);
    public static final Vector e2 = e(3, 1);
    public static final Vector e3 = e(3, 2);

    /**
     * X,Y,Z are the indexes of the coordinates x, y, z
     */
    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;

    /** temporary vector to be used on calculation - for performance aspects only */
    private static final Vector3D tmp = new Vector3D(0, 0, 0);

    /**
     * constructor
     * 
     * @param v vector coordinates
     */
    public Vector3D(float x, float y, float z) {
        super(x, y, z);
    }

    /**
     * x
     * @return x-coordinate
     */
    public final float x() {
        return x[X];
    }

    /**
     * y
     * @return y-coordinate
     */
    public final float y() {
        return x[Y];
    }

    /**
     * z
     * @return z-coordinate
     */
    public final float z() {
        return x[Z];
    }

    /**
     * tmpVector
     * @param x x-coordinate
     * @param y y-coordinate
     * @param z z-coordinate
     * @return static fast vector, only for temp. calculations 
     */
    public static final Vector3D tmpVector(float x, float y, float z) {
        tmp.set(x, z, z);
        return tmp;
    }

    /**
     * rotX
     * @param angle x-rotation-angle
     */
    public void rotX(float angle) {
        float cosRY = (float) Math.cos(angle);
        float sinRY = (float) Math.sin(angle);

        tmp.set(x(), y(), z());

        x[Y] = (tmp.y() * cosRY) - (tmp.z() * sinRY);
        x[Z] = (tmp.y() * sinRY) + (tmp.z() * cosRY);
    }

    /**
     * rotY
     * @param angle y-rotation-angle
     */
    public void rotY(float angle) {
        float cosRY = (float) Math.cos(angle);
        float sinRY = (float) Math.sin(angle);

        tmp.set(x(), y(), z());

        this.x[X] = (tmp.x() * cosRY) + (tmp.z() * sinRY);
        this.x[Z] = (tmp.x() * -sinRY) + (tmp.z() * cosRY);
    }

    /**
     * rotZ
     * @param angle z-rotation-angle
     */
    public void rotZ(float angle) {
        float cosRY = (float) Math.cos(angle);
        float sinRY = (float) Math.sin(angle);

        tmp.set(x(), y(), z());

        x[X] = (tmp.x() * cosRY) - (tmp.y() * sinRY);
        x[Y] = (tmp.x() * sinRY) + (tmp.y() * cosRY);
    }

}
