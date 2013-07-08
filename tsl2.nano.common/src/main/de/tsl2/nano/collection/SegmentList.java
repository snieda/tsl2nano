package de.tsl2.nano.collection;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * "@deprecated" please use {@link FloatArray} dynamic array using an {@link ArrayList} as segmentation store. the
 * segments are object arrays. were created as a base for fast primitive segmentation arrays, but it is not faster than
 * the origin ArrayList. because of constraint java generics, it is not possible to use with primitives.
 * <p/>
 * 
 * <pre>
 * use:
 * - call the constructor like: new SegmentList<byte[], Byte>(byte.class)
 * - add your items with {@link #add(Object)} giving a new f.e. byte array
 * - get the full array through {@link #toSegmentArray()}
 * </pre>
 * 
 * @param <T>
 * @author Thomas Schneider
 * @version $Revision$
 */
public class SegmentList<TARRAY, T> extends ArrayList<TARRAY> {
    /** serialVersionUID */
    private static final long serialVersionUID = -691507232535252240L;
    Class<T> type;
    /** capacity on growing segmentation list {@link #seg} */
    protected int capacity;
    /** default size of segmentation arrays */
    protected static final int DEFAULT_SEGMENTATION = 50;
    /** default capacity on growing segmentation list {@link #seg} */
    protected static final int DEFAULT_CAPACITY = 50;

    /**
     * constructor
     */
    public SegmentList() {
        this((Class<T>) Object.class, DEFAULT_CAPACITY);
    }

    public SegmentList(Class<T> type) {
        this(type, DEFAULT_CAPACITY);
    }

    /**
     * constructor
     * 
     * @param segmentation segmentation-array size
     * @param capacity segmentation holder size
     */
    public SegmentList(Class<T> type, int capacity) {
        super(capacity);
        this.type = type;
        this.capacity = capacity;
    }

    TARRAY newArraySegment(int size) {
        return (TARRAY) Array.newInstance(type, size);
    }

    public int elementCount() {
        int c = 0;
        for (TARRAY s : this) {
            c += Array.getLength(s);
        }
        return c;
    }

    /**
     * creates one new primitive array holding all stored elements
     * 
     * @return new array
     */
    public TARRAY toSegmentArray() {
        TARRAY arr = (TARRAY) newArraySegment(elementCount());
        int c = 0;
        int off = 0, segment = 0;
        for (TARRAY af : this) {
            segment = Array.getLength(af);
            System.arraycopy(af, 0, arr, off, segment);
            off += segment;
        }
        return arr;
    }

}