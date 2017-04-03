/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 24.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.util;

import java.util.HashMap;
import java.util.Map;

import de.tsl2.nano.core.util.StringUtil;

/**
 * generic fuzzy finder. see {@link IFuzzyDescriptor} as descriptor for data and filter.
 * 
 * @author Tom
 * @version $Revision$
 */
public class FuzzyFinder<T> {
    private IFuzzyDescriptor<T> descriptor;

    /**
     * constructor
     */
    public FuzzyFinder(IFuzzyDescriptor<T> descriptor) {
        this.descriptor = descriptor;
    }

    public FuzzyFinder(final Iterable<T> availables) {
        descriptor = new IFuzzyDescriptor<T>() {
            @Override
            public Iterable<T> getAvailables() {
                return availables;
            }

            @Override
            public double distance(T item, String expression) {
                return StringUtil.fuzzyMatch(item, expression);
            }
        };
    }

    public T find(String filter) {
        Map<Double, T> result = fuzzyFind(filter);
        return (result.size() > 0 && result.containsKey(1d) ? result.get(1d) : null);
    }

    @SuppressWarnings("unchecked")
    public <M extends Map<Double, T>> M fuzzyFind(String filter) {
        HashMap<Double, T> map = new HashMap<Double, T>();
        Double match;
        Iterable<T> availables = descriptor.getAvailables();
        for (T element : availables) {
            match = descriptor.distance(element, filter);
            if (match > 0) {
                while (map.containsKey(match))
                    match += 0000000001;
                map.put(match, element);
            }
        }
        return (M) map;
    }
}
