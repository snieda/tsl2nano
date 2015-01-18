/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 17.01.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.util.StringUtil;

/**
 * evaluates the current difference between two objects.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class Diff<T> {
    T first;
    T second;

    transient List<IAttribute> attributes;

    /**
     * constructor
     * 
     * @param first
     * @param second
     */
    public Diff(T first, T second) {
        super();
        this.first = first;
        this.second = second;
    }

    /**
     * @return Returns the first.
     */
    public T getFirst() {
        return first;
    }

    /**
     * @return Returns the second.
     */
    public T getSecond() {
        return second;
    }

    /**
     * @return Returns the attributes.
     */
    private List<IAttribute> getAttributes() {
        if (attributes == null)
            attributes = BeanClass.getBeanClass(first.getClass()).getAttributes();
        return attributes;
    }

    public Collection<Difference> getDifferences() {
        List<IAttribute> attrs = getAttributes();
        ArrayList<Difference> diffs = new ArrayList<Difference>(attrs.size());
        Object v1, v2;
        for (IAttribute a : attrs) {
            v1 = a.getValue(first);
            v2 = a.getValue(second);
            if (!v2.equals(v2))
                diffs.add(new Difference(a.getName(), v1, v2));
        }
        return diffs;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Difference between '" + first + "' and '" + second + "'");
        Collection<Difference> diffs = getDifferences();
        int i = 0;
        for (Difference d : diffs) {
            buf.append("\t" + ++i + d.toString());
        }
        return buf.toString();
    }
}

class Difference {
    String name;
    Object first;
    Object second;

    /**
     * constructor
     * 
     * @param name
     * @param first
     * @param second
     */
    public Difference(String name, Object first, Object second) {
        super();
        this.name = name;
        this.first = first;
        this.second = second;
    }
    @Override
    public String toString() {
        return StringUtil.fixString(name, 12, ' ', true) + first + " --> " + second;
    }
}