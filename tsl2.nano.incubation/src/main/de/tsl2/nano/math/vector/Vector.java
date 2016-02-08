/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 05.01.2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.math.vector;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import de.tsl2.nano.collection.FloatArray;

/**
 * Simple-handling numeric n-dim vector with common calculations. See {@link Coordinate} for further informations.
 * 
 * @author ts
 * 
 */
public class Vector extends Coordinate implements Comparable<Vector> {

    /** serialVersionUID */
    private static final long serialVersionUID = -2179340122151337364L;

    /**
     * @param v any vector
     */
    public Vector(float... v) {
        super(v);
    }

    /**
     * create
     * 
     * @param dim dimension
     * @param defaultValue default value for each cell
     * @return new vector
     */
    public static Vector create(int dim, float defaultValue) {
        float v[] = new float[dim];
        for (int i = 0; i < v.length; i++) {
            v[i] = defaultValue;
        }
        return new Vector(v);
    }

    /**
     * scale
     * 
     * @param v vector to be scaled
     * @param f scale factor
     * @return new vector instance, scaled by f.
     */
    public static Vector scale(Vector v, float f) {
        v = v.clone(1);
        v.scale(f);
        return v;
    }

    /**
     * add
     * 
     * @param v vector to add
     */
    public void add(Vector v) {
        add(v.x);
    }

    /**
     * add
     * 
     * @param v1 source vector to add the vectors vv to.
     * @param vv adding vectors
     * @return new vector instance as sum of all vectors
     */
    public static Vector add(Vector v1, Vector... vv) {
        Vector v = v1.clone(1);
        for (int i = 0; i < vv.length; i++) {
            v.add(vv[i]);
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(float... v) {
        checkDimension(v);
        for (int i = 0; i < v.length; i++) {
            this.x[i] += v[i];
        }
    }

    /**
     * subtract
     * 
     * @param v1 source vector to subtract the vectors vv from.
     * @param vv subtractions vectors
     * @return new vector instance as subtraction
     */
    public static Vector subtract(Vector v1, Vector... vv) {
        Vector v = v1.clone(1);
        for (int i = 0; i < vv.length; i++) {
            v.subtract(vv[i]);
        }
        return v;
    }

    /**
     * subtract
     * 
     * @param v vector to subtract
     */
    protected void subtract(float... v) {
        checkDimension(v);
        for (int i = 0; i < v.length; i++) {
            this.x[i] -= v[i];
        }
    }

    /**
     * subtract
     * 
     * @param v vector to subtract
     */
    public void subtract(Vector v) {
        subtract(v.x);
    }

//    public void crossProduct(float... v) {
//        checkDimension(v);
//        for (int i = 0; i < x.length; i++) {
//            x[i] = indexCrossProduct(i, v);
//        }
//    }
//
//    private static final float indexCrossProduct(float x, float... v) {
//        float r = 0;
//        for (int j = 0; j < v.length; j++) {
//            r += x * v[j];
//        }
//        return r;
//    }

    /**
     * see {@link #scalar(float...)}
     */
    public float scalar(Vector v) {
        return scalar(v.x);
    }

    /**
     * scalar / inner / dot product
     * 
     * @param v second operand of scalar
     * @return scalar product of this and v
     */
    protected float scalar(float... v) {
        checkDimension(v);
        float result = 0;
        for (int i = 0; i < v.length; i++) {
            result += x[i] * v[i];
        }
        return result;
    }

    /**
     * delegates to {@link #scalar(float...)}
     */
    public float multiply(Vector v) {
        return scalar(v.x);
    }

    /**
     * through the scalar product it is possible to calculate the angle between two vectors.
     * 
     * @param v another vector to calc the inner angle to
     * @return inner angle between the two vectors
     */
    public float angle(Vector v) {
        float sp = scalar(v);
        float cos = sp / (len() * v.len());
        return (float) Math.acos(cos);
    }

    /**
     * multiply with an equal-dim matrix
     * 
     * @param m matrix
     */
    public void multiply(float[]... m) {
        // do only a simple check on first vector
        checkDimension(m[0]);
        float[] n = new float[x.length];
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[i].length; j++) {
                // error on indexes!
                n[i] += x[i] * m[i][j];
            }
        }
        System.arraycopy(n, 0, x, 0, x.length);
    }

    /**
     * multiply with an equal-dim matrix
     * 
     * @param m matrix
     */
    public void multiply(Vector[] m) {
        int dim = m[0].dimension();
        float[] mf = new float[dim];
        for (int i = 0; i < mf.length; i++) {
            System.arraycopy(m[i].x, 0, mf, 0, dim);
        }
        scalar(mf);
    }

    /**
     * length of current vector
     * 
     * @return
     */
    public float len() {
        float l = 0;
        for (int i = 0; i < x.length; i++) {
            l += x[i] * x[i];
        }
        return (float) Math.sqrt(l);
    }

    /**
     * distance between this and v2. see {@link #subtract(Vector, Vector...)}.
     * 
     * @param v2 second operand
     * @return distance between the two vectors
     */
    public Vector distance(Vector v2) {
        return Vector.subtract(this, v2);
    }

    /**
     * distance as scalar.
     * 
     * @param v second operand
     * @return scalar distance
     */
    public float distance(float... v) {
        checkDimension(v);
        float d = 0;
        float id;
        for (int i = 0; i < x.length; i++) {
            id = x[i] - v[i];
            d += id * id;
        }
        return (float) Math.sqrt(d);
    }

    /**
     * normalize the given vector
     * 
     * @param v vector to normalize
     * @return new normalized vector instance
     */
    public static Vector normalize(Vector v) {
        v = v.clone(1);
        v.normalize();
        return v;
    }

    /**
     * normalize vector
     */
    public void normalize() {
        float l = len();
        if (l == 0) {
            return;
        }
        for (int i = 0; i < x.length; i++) {
            x[i] /= l;
        }
    }

    /**
     * orthogonizeInPlaneWith
     * 
     * @param v2 second operand in plane with current vector.
     */
    public void orthogonizeInPlaneWith(Vector v2) {
        int a = isEVector(this);
        if (a != -1) {
            for (int i = 0; i < x.length; i++) {
                if (i == a) {
                    continue;
                }
                Vector v1 = e(x.length, i);
                if (v1.scalar(v2.x) != 0) {
                    x = v1.x;
                    break;
                }
            }
        } else {
            for (int i = 0; i < x.length; i++) {
                if (x[i] == 0) {
                    continue;
                }
                Vector v1 = clone(1);
                v1.x[i] = -v1.x[i];
                if (v1.scalar(v2.x) != 0) {
                    x = v1.x;
                    break;
                }
            }
        }
    }

    /**
     * isEVector
     * 
     * @return axis/row of 1
     */
    protected static int isEVector(Vector v) {
        if (v.len() != 1) {
            return -1;
        }
        int u = 0;
        int a = -1;
        for (int i = 0; i < v.x.length; i++) {
            if (v.x[i] == 1) {
                a = i;
                u++;
            }
        }
        return u == 1 ? a : -1;
    }

    /**
     * creates an n-dim normalized vector with number 1 on given row.
     * 
     * @param dim dimension
     * @param row coordinate position of 1.
     * @return new vector instance
     */
    public static Vector e(int dim, int row) {
        float d[] = new float[dim];
        d[row] = 1;
        return new Vector(d);
    }

    /**
     * invert given vector
     * 
     * @param v vector to invert
     * @return new vector instance
     */
    public static Vector invert(Vector v) {
        Vector n = v.clone(1);
        for (int i = 0; i < v.x.length; i++) {
            if (v.x[i] == 0) {
                n.x[i] = 1;
            } else {
                n.x[i] = 0;
            }
        }
        return n;
    }

    @Override
    public int compareTo(Vector o) {
        return (int) (len() - o.len());
    }

    public static List<Vector> fromStream(InputStream stream) {
        Scanner sc = new Scanner(stream);
        final String CR = "\n";
        FloatArray fa = new FloatArray();
        List<Vector> vlist = new LinkedList<Vector>();
        while (sc.hasNext()) {
            Scanner line = new Scanner(sc.nextLine());
            //read us floats
            line.useLocale(Locale.US);
            while (line.hasNext()) {
                if (line.hasNextFloat()) {
                    fa.add(line.nextFloat());
                } else {
                    line.next();
                }
            }
            if (fa.size() > 0) {
                vlist.add(new Vector(fa.toArray()));
                fa.clear();
            }
            line.close();
        }
        sc.close();
        return vlist;
    }

    public static InputStream toStream(List<Vector> vl) {
        ByteOutputStream stream = new ByteOutputStream();
        final byte[] CR = "\n".getBytes(), TAB = "\t".getBytes();
        for (Vector v : vl) {
            for (int i = 0; i < v.dimension(); i++) {
                stream.write(/*DecimalFormat.getInstance().format*/Float.toString(v.x[i]).getBytes());
                if (i < v.dimension() - 1)
                    stream.write(TAB);
            }
            stream.write(CR);
        }
        stream.close();
        return stream.newInputStream();
    }
}
