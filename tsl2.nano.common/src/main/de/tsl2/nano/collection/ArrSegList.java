package de.tsl2.nano.collection;

import static de.tsl2.nano.collection.SegmentList.DEFAULT_CAPACITY;
import static de.tsl2.nano.collection.SegmentList.DEFAULT_SEGMENTATION;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * The segments are object arrays. were created as a base for fast primitive segmentation arrays, but it is not faster
 * than the origin ArrayList. If you need a float[] list, use FloatArray instead, while it is faster.
 * <p/>
 * If you need only access to the segments itself (primitive arrays), use the {@link SegmentList} instead!
 * <p/>
 * Because of constraint java generics, it is not possible to use with primitives as generics directly. that's why we
 * use the TARRAY (normally a primitive array like float[]). If you don't use primitive arrays, the TARRAY type will be
 * nothing more than T[].
 * <p/>
 * Most {@link List} interface methods are implemented - but not sublists and iterations! It is possible to add segments
 * itself. if they have the size of {@link #segmentation}, they may be add directly.
 * 
 * <pre>
 * Use:
 * - standard constructing (e.g. for float): new ArrSegList<float[], Float>(float.class)
 * - {@link #add(Object)} to add a single object
 * - {@link #addSegment(Object)} to add a primitive array
 * - {@link #get(int)}
 * - {@link #toArray()} to get all segments wrapped into a new array.
 * </pre>
 * 
 * @param <TARRAY> array type (useful for primitive arrays, otherwise it should be T[])
 * @param <T> single type - as it isn't possible to use primitive types as generic, you will give an immutable like
 *            Float or Byte.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ArrSegList<TARRAY, T> implements List<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 8456729694923697194L;
    /** segmentation list - holding all segments */
    protected SegmentList<TARRAY, T> seg;
    /** all current stored elements */
    int elements = 0;
    /** current capacity */
    int arraysize = 0;
    /** size of segmentation arrays */
    protected int segmentation;

    /**
     * constructor
     */
    public ArrSegList() {
        this((Class<T>) Object.class, DEFAULT_SEGMENTATION, DEFAULT_CAPACITY);
    }

    public ArrSegList(Class<T> type) {
        this(type, DEFAULT_SEGMENTATION, DEFAULT_CAPACITY);
    }

    /**
     * constructor
     * 
     * @param segmentation segmentation-array size
     * @param capacity segmentation holder size
     */
    public ArrSegList(Class<T> type, int segmentation, int capacity) {
        super();
        this.segmentation = segmentation;
        seg = new SegmentList<TARRAY, T>(type, capacity);
    }

    /**
     * gets the value at the given array index
     * 
     * @param index array index
     * @return value
     */
    @Override
    public T get(int index) {
        int iseg = index / segmentation;
        int iarr = index % segmentation;
        return (T) Array.get(seg.get(iseg), iarr);
    }

    /**
     * getLastSegment
     * 
     * @return last created segment
     */
    public final TARRAY getLastSegment() {
        return seg.get(seg.size() - 1);
    }

    /**
     * adds a new element to the segmentation array
     * 
     * @param newItem new element
     */
    @Override
    public boolean add(T newItem) {
        TARRAY s;
        if (elements == arraysize) {
            s = seg.newArraySegment(segmentation);
            arraysize += segmentation;
            seg.add(s);
        } else {
            s = seg.get(seg.size() - 1);
        }
        Array.set(s, elements % segmentation, newItem);
        elements++;
        return true;
    }

    /**
     * add
     * 
     * @param newSegment new segment
     * @return true, if the given segment was directly added by reference. then, its content may be shifted!
     */
    public boolean addSegment(TARRAY newSegment) {
        int length = Array.getLength(newSegment);
        //to lazy to implement that case
        assert length <= segmentation;
        TARRAY s;
        //use the new array as segment
        if (length == segmentation) {
            if (elements == arraysize) {
                arraysize += segmentation;
                seg.add(newSegment);
            } else if (elements + length >= arraysize) {
                s = (TARRAY) Array.get(seg, seg.size() - 1);
                int delta = elements + length - arraysize;
                System.arraycopy(newSegment, 0, s, elements % segmentation, delta);
                System.arraycopy(newSegment, delta + 1, newSegment, 0, delta);
                seg.add(newSegment);
            }
        } else {
            if (elements + length < arraysize) {
                s = getLastSegment();
                System.arraycopy(newSegment, 0, s, elements % segmentation, length);
            } else {
                s = seg.newArraySegment(segmentation);
                arraysize += segmentation;
                System.arraycopy(newSegment, 0, s, elements % segmentation, length);
                seg.add(newSegment);
            }

        }
        elements += length;
        return length == segmentation;
    }

    /**
     * size
     * 
     * @return size
     */
    @Override
    public int size() {
        return elements;
    }

    /**
     * creates one new primitive array holding all stored elements
     * 
     * @return all together
     */
    public TARRAY toSegmentArray() {
       TARRAY arr = seg.newArraySegment(elements);
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

    /**
     * creates one new primitive array holding all stored elements. please use {@link #toSegmentArray()} instead, if you
     * not have to use the list-interface.
     * 
     * @return new array
     */
    @Override
    public Object[] toArray() {
        return (Object[]) toSegmentArray();
    }

    @Override
    public boolean isEmpty() {
        return elements == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object item : c) {
            if (!contains(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean result = true;
        for (T item : c) {
            result &= add(item);
        }
        return result;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub

    }

    @Override
    public T set(int index, T element) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void add(int index, T element) {
        // TODO Auto-generated method stub

    }

    @Override
    public T remove(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int indexOf(Object o) {
        int i;
        for (TARRAY arr : seg) {
            i = Arrays.binarySearch((Object[]) arr, o);
            if (i >= 0) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

}