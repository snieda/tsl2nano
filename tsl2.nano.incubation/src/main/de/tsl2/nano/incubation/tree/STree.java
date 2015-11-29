/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 29.11.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.incubation.tree;

import java.util.Map;

/**
 * Simple {@link Tree} using a simple counter as connection info
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class STree<T> extends Tree<Integer, T> {

    /** serialVersionUID */
    private static final long serialVersionUID = 6791740823737461396L;


    /**
     * constructor
     */
    protected STree() {
    }

    /**
     * constructor
     * @param node
     * @param parent
     * @param m
     */
    public STree(T node, Tree<Integer, T> parent, Map<? extends Integer, ? extends Tree<Integer, T>> m) {
        super(node, parent, m);
    }

    /**
     * constructor
     * @param node
     * @param parent
     */
    public STree(T node, Tree<Integer, T> parent) {
        super(node, parent);
    }

    public void add(T... childs) {
        for (int i = 0; i < childs.length; i++) {
            add(size(), childs[i]);
        }
    }
}
