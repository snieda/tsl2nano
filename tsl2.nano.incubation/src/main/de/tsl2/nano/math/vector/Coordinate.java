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

import java.io.Serializable;
import java.util.Arrays;

/**
 * Basic simple handling n-dim coordinate. the coordinates are floats, as they will fulfill most cases.
 * <p/>
 * It is not possible to use java generics, because no primitives are possible. If you use the wrappers of type Number,
 * you are not able to do math calculations.
 * 
 * <pre>
 * Example:
 *  public class Coordinate<T extends Number> {
 *  ...
 *     public void add(T... v) {
 *         checkDimension(v);
 *         for (int i = 0; i < v.length; i++) {
 *         // this is not possible, as the generic type T cannot use autoboxing
 *             x[i] = x[i] + v[i];
 *         }
 *     }
 *     ...
 *     }
 * </pre>
 * 
 * @author ts
 * @version $Revision$
 */
public class Coordinate implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -3712289206658197158L;
    
    public float[] x;

    /**
     * @param v coordinate
     */
    public Coordinate(float... v) {
        super();
        this.x = v;
    }

    public void set(float... v) {
        checkDimension(v);
        System.arraycopy(v, 0, x, 0, v.length);
    }

    /**
     * set new coordinate
     * @param v new coordinate
     */
    public void set(Coordinate v) {
        set(v.x);
    }

    /**
     * checkDimension
     * 
     * @param v coordinate to check
     */
    protected final void checkDimension(float... v) {
        checkDimension(x.length, v);
    }

    /**
     * checkDimension
     * 
     * @param dim dimension
     * @param v coordinate
     * @return given coordinate
     */
    protected static final float[] checkDimension(int dim, float... v) {
        if (dim != v.length) {
            throw new RuntimeException("the given vector " + v + " must have the dimension " + dim);
        }
        return v;
    }

    /**
     * @return vector dimension
     */
    public int dimension() {
        return x.length;
    }

    /**
     * add
     * @param v coordinate to add
     */
    public void add(float... v) {
        checkDimension(v);
        for (int i = 0; i < v.length; i++) {
            x[i] = x[i] + v[i];
        }
    }

    /**
     * scale
     * @param v coordinate to scale
     */
    public void scale(float... v) {
        checkDimension(v);
        for (int i = 0; i < v.length; i++) {
            x[i] *= v[i];
        }
    }

//    public void scale(Vector v) {
//        scale(v.x);
//    }

    /**
     * scale
     * 
     * @param f single scale factor
     */
    public void scale(float f) {
        for (int i = 0; i < x.length; i++) {
            x[i] *= f;
        }
    }

    /**
     * clone with adaption. convenience for clone+scale.
     * @param adapt adaption factor for new coordinate
     * @return
     */
    public Vector clone(float adapt) {
        float[] n = new float[x.length];
        for (int i = 0; i < n.length; i++) {
            n[i] = x[i] * adapt;
        }
        return new Vector(n);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(x);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        return Arrays.equals(x, ((Coordinate)o).x);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < x.length; i++) {
            buf.append(String.format("%10.2f,", x[i]));
        }
        return buf.substring(0, buf.length() - 1);
    }
}
