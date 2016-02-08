/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 25.04.2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.collection;

import java.util.ArrayList;
import java.util.List;

/**
 * fast primitive dynamic array using floats. stores array-segments and is about 40% faster as an ArrayList - on a test
 * adding and getting one million floats.
 * 
 * @author ts
 * @version $Revision$
 */
public class FloatArray {
    /** segmentation list - holding all segments */
    List<float[]> seg;
    /** all current stored elements */
    int elements = 0;
    /** current capacity */
    int arraysize = 0;
    /** capacity on growing segmentation list {@link #seg} */
    int capacity;
    /** size of segmentation arrays */
    int segmentation;
    /** default size of segmentation arrays */
    static final int DEFAULT_SEGMENTATION = 50;
    /** default capacity on growing segmentation list {@link #seg} */
    static final int DEFAULT_CAPACITY = 50;

    /**
     * constructor using {@link #DEFAULT_SEGMENTATION} and {@link #DEFAULT_CAPACITY}.
     */
    public FloatArray() {
        this(DEFAULT_SEGMENTATION, DEFAULT_CAPACITY);
    }

    /**
     * constructor
     * 
     * @param segmentation segmentation-array size
     * @param capacity segmentation holder size
     */
    public FloatArray(int segmentation, int capacity) {
        super();
        this.segmentation = segmentation;
        this.capacity = capacity;
        seg = new ArrayList<float[]>(capacity);
    }

    /**
     * gets the value at the given array index
     * 
     * @param index array index
     * @return value
     */
    public float get(int index) {
        int iseg = index / segmentation;
        int iarr = index % segmentation;
        return seg.get(iseg)[iarr];
    }

    /**
     * adds a new element to the segmentation array
     * 
     * @param newNumber new element
     */
    public void add(float newNumber) {
        float[] s;
        if (elements == arraysize) {
            s = new float[segmentation];
            arraysize += segmentation;
            seg.add(s);
        } else {
            s = seg.get(seg.size() - 1);
        }
        s[elements % segmentation] = newNumber;
        elements++;
    }

    /**
     * size
     * @return count of stored elements
     */
    public int size() {
        return elements;
    }

    public void clear() {
        elements = 0;
    }
    
    /**
     * creates one new primitive array holding all stored elements
     * 
     * @return new array
     */
    public float[] toArray() {
        float[] arr = new float[elements];
        int c;
        int size = seg.size();
        for (c = 0; c < size - 1; c++) {
            System.arraycopy(seg.get(c), 0, arr, c * segmentation, segmentation);
        }
        //last segmentation may not be fully filled
        if (size > 0) {
            int rest = elements - c * segmentation;
            System.arraycopy(seg.get(size - 1), 0, arr, c * segmentation, rest);
        }
        return arr;
    }
}
