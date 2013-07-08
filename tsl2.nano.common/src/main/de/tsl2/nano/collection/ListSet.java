/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Dec 15, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

/**
 * The Set interface is a subgroup of the List interface - but they are distinguished in all implementations. sometimes,
 * on f.e. eclipse databdinding, you have to select your base implementation to work on sets or lists. This class
 * provides the combination of both interfaces.
 * <p>
 * The implementation is an extension of {@link ArrayList} but additionally implementing the {@link Set} interface - no
 * code has to be created.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ListSet<E> extends ArrayList<E> implements Set<E> {
    /** serialVersionUID */
    private static final long serialVersionUID = -8997546499166739440L;
    transient Comparator<E> comp = new Comparator() {

        @Override
        public int compare(Object o1, Object o2) {
            if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            } else if (o1.equals(o2)) {
                return 0;
            } else {
                return (o1.toString().compareTo(o2.toString()));
            }
        }

    };

    /**
     * constructor
     */
    public ListSet() {
        super();
    }

    /**
     * constructor - does not respect, that items may be equal
     */
    public ListSet(E...items) {
        super(Arrays.asList(items));
    }

    /**
     * constructor - does not respect, that items may be equal
     * 
     * @param c
     */
    public ListSet(Collection<? extends E> c) {
        super(c);
    }

    /**
     * constructor
     * 
     * @param initialCapacity
     */
    public ListSet(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(E e) {
        removeDuplette(e);
        return super.add(e);
    }

    /**
     * removeDuplette
     * 
     * @param e
     */
    protected void removeDuplette(E e) {
        final int i = Collections.binarySearch(this, e, comp);
        if (i > -1) {
            remove(i);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(int index, E element) {
        removeDuplette(element);
        super.add(index, element);
    }

}
